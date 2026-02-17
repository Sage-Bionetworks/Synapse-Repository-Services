package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_IDP_PROVIDER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_IDP_REALM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_PRINCIPAL_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_PRINCIPAL_PRINCIPAL_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_PRINCIPAL_REALM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.REALM_PRINCIPAL_TYPE_ADMINISTRATORS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.REALM_PRINCIPAL_TYPE_ANONYMOUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.REALM_PRINCIPAL_TYPE_AUTHENTICATED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.REALM_PRINCIPAL_TYPE_PUBLIC;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REALM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REALM_IDP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REALM_PRINCIPAL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.RealmDao;
import org.sagebionetworks.repo.model.auth.IdentityProvider;
import org.sagebionetworks.repo.model.auth.OAuthIdentityProvider;
import org.sagebionetworks.repo.model.auth.Realm;
import org.sagebionetworks.repo.model.auth.RealmIdList;
import org.sagebionetworks.repo.model.auth.RealmPrincipal;
import org.sagebionetworks.repo.model.auth.SynapseIdentityProvider;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.DBORealm;
import org.sagebionetworks.repo.model.dbo.persistence.DBORealmIdentityProvider;
import org.sagebionetworks.repo.model.dbo.persistence.DBORealmPrincipal;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class RealmDaoImpl implements RealmDao {
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	private static final String SYNAPSE_IDENTITY_PROVIDER = "SYNAPSE";
	
	private static final String DEFAULT_REALM_NAME = "SYNAPSE";
	
	private static final String DELETE_IDPS_SQL = "DELETE FROM "+
			TABLE_REALM_IDP+" WHERE "+COL_REALM_IDP_REALM_ID+" = ?";
		
	private static final String DELETE_PRINCIPALS_SQL = "DELETE FROM "+
			TABLE_REALM_PRINCIPAL+" WHERE "+COL_REALM_PRINCIPAL_REALM_ID+" = ?";
	
	private static final String REALM_IDP_SQL = "SELECT * FROM "+TABLE_REALM_IDP+" WHERE "+COL_REALM_IDP_REALM_ID+" = ?";
	
	private static final String REALM_FOR_IDP_SQL = "SELECT "+COL_REALM_IDP_REALM_ID+" FROM "+TABLE_REALM_IDP+
			" WHERE "+COL_REALM_IDP_PROVIDER+" = ?";
	
	private static final String REALM_PRINCIPAL_SQL = "SELECT * FROM "+TABLE_REALM_PRINCIPAL+" WHERE "+COL_REALM_PRINCIPAL_REALM_ID+" = ?";
	
	private static final String SELECT_ALL_IDS_SQL = "SELECT "+COL_REALM_ID+" FROM "+TABLE_REALM;
	
	private static final String DELETE_REALM_SQL = "DELETE FROM "+TABLE_REALM+" WHERE "+COL_REALM_ID+" = ?";
	
	private static final String SELECT_REALM_FOR_ANONYNOUS_PRINCIPAL_SQL = 
			"SELECT "+COL_REALM_PRINCIPAL_REALM_ID+" FROM "+TABLE_REALM_PRINCIPAL+
			" WHERE "+
			COL_REALM_PRINCIPAL_PRINCIPAL_ID+" = "+"? AND "+
			COL_REALM_PRINCIPAL_PRINCIPAL_TYPE+" = '"+REALM_PRINCIPAL_TYPE_ANONYMOUS+"'";

	static Long stringToLong(String s) {
		Long result = null;
		if (s!=null) {
			result = Long.parseLong(s);
		}
		return result;
	}
	static DBORealm copyRealmToDBORealm(Realm realm) {
		DBORealm dbo = new DBORealm();
		dbo.setId(stringToLong(realm.getId()));
		dbo.setName(realm.getName());
		dbo.setCreationDate(realm.getCreatedOn());
		return dbo;
	}
	
	static Realm copyDBORealmToRealm(DBORealm dbo) {
		Realm result = new Realm();
		result.setId(dbo.getId().toString());
		result.setName(dbo.getName());
		result.setCreatedOn(dbo.getCreationDate());
		return result;
	}
	
	static String identityProviderName(IdentityProvider idp) {
		if (idp instanceof SynapseIdentityProvider) {
			return SYNAPSE_IDENTITY_PROVIDER;
		} else if (idp instanceof OAuthIdentityProvider) {
			return ((OAuthIdentityProvider)idp).getProvider().name();
		} else {
			throw new IllegalArgumentException("Unexpected type "+idp.getClass().getName());
		}
	}
	
	static List<DBORealmIdentityProvider> copyRealmToRealmIdps(Realm realm) {
		List<DBORealmIdentityProvider> result = new ArrayList<DBORealmIdentityProvider>();
		if (realm.getIdentityProvider()!=null) {
			for (IdentityProvider idp : realm.getIdentityProvider()) {
				DBORealmIdentityProvider realmIdp = new DBORealmIdentityProvider();
				realmIdp.setRealmId(stringToLong(realm.getId()));
				realmIdp.setIdentityProvider(identityProviderName(idp));
				result.add(realmIdp);
			}
		}
		return result;
	}
	
	static void copyRealmIdpsToRealm(List<DBORealmIdentityProvider> dboList, Realm realm) {
		List<IdentityProvider> dtoList = new ArrayList<IdentityProvider>();
		for (DBORealmIdentityProvider dbo : dboList) {
			if (SYNAPSE_IDENTITY_PROVIDER.equals(dbo.getIdentityProvider())) {
				dtoList.add(new SynapseIdentityProvider());
			} else {
				OAuthIdentityProvider dto = new OAuthIdentityProvider();
				dto.setProvider(OAuthProvider.valueOf(dbo.getIdentityProvider()));
				dtoList.add(dto);
			}
		}
		realm.setIdentityProvider(dtoList);
	}
	
	static void copyRealmPrincipalsToRealm(List<DBORealmPrincipal> principals, RealmPrincipal realmPrincipal) {
		for (DBORealmPrincipal principal : principals) {
			switch (principal.getPrincipalType()) {
				case REALM_PRINCIPAL_TYPE_ANONYMOUS:
					realmPrincipal.setAnonymousUser(principal.getPrincipalId().toString());
					break;
				case REALM_PRINCIPAL_TYPE_PUBLIC:
					realmPrincipal.setPublicGroup(principal.getPrincipalId().toString());
					break;
				case REALM_PRINCIPAL_TYPE_AUTHENTICATED:
					realmPrincipal.setAuthenticatedUsers(principal.getPrincipalId().toString());
					break;
				case REALM_PRINCIPAL_TYPE_ADMINISTRATORS:
					realmPrincipal.setAdministrativeGroup(principal.getPrincipalId().toString());
					break;
				default:
					throw new IllegalStateException("Unexpected type" +principal.getPrincipalType());
			}
		}
	}
	
	List<DBORealmPrincipal> copyRealmPrincipalsToDBOList(RealmPrincipal realmPrincipal) {
		List<DBORealmPrincipal> result = new ArrayList<DBORealmPrincipal>();
		DBORealmPrincipal dbo = new DBORealmPrincipal();
		dbo.setRealmId(stringToLong(realmPrincipal.getRealmId()));
		dbo.setId(idGenerator.generateNewId(IdType.REALM_PRINCIPAL));
		dbo.setPrincipalId(stringToLong(realmPrincipal.getAnonymousUser()));
		dbo.setPrincipalType(REALM_PRINCIPAL_TYPE_ANONYMOUS);
		result.add(dbo);
		dbo = new DBORealmPrincipal();
		dbo.setRealmId(stringToLong(realmPrincipal.getRealmId()));
		dbo.setId(idGenerator.generateNewId(IdType.REALM_PRINCIPAL));
		dbo.setPrincipalId(stringToLong(realmPrincipal.getPublicGroup()));
		dbo.setPrincipalType(REALM_PRINCIPAL_TYPE_PUBLIC);
		result.add(dbo);
		dbo = new DBORealmPrincipal();
		dbo.setRealmId(stringToLong(realmPrincipal.getRealmId()));
		dbo.setId(idGenerator.generateNewId(IdType.REALM_PRINCIPAL));
		dbo.setPrincipalId(stringToLong(realmPrincipal.getAuthenticatedUsers()));
		dbo.setPrincipalType(REALM_PRINCIPAL_TYPE_AUTHENTICATED);
		result.add(dbo);
		dbo = new DBORealmPrincipal();
		dbo.setRealmId(stringToLong(realmPrincipal.getRealmId()));
		dbo.setId(idGenerator.generateNewId(IdType.REALM_PRINCIPAL));
		dbo.setPrincipalId(stringToLong(realmPrincipal.getAdministrativeGroup()));
		dbo.setPrincipalType(REALM_PRINCIPAL_TYPE_ADMINISTRATORS);
		result.add(dbo);
		return result;
	}

	private DBORealm createPrivate(Realm dto) {
		dto.setCreatedOn(new Date());
		DBORealm dbo = copyRealmToDBORealm(dto);
		dbo = basicDao.createNew(dbo);
		
		// remove existing IDPs for this realm
		jdbcTemplate.update(DELETE_IDPS_SQL, dto.getId());
		// create the IDPs
		List<DBORealmIdentityProvider> idps = copyRealmToRealmIdps(dto);
		basicDao.createBatch(idps);
		return dbo;
	}

	@WriteTransaction
	@Override
	public Realm createRealm(Realm dto) {
		dto.setId(idGenerator.generateNewId(IdType.REALM).toString());
		createPrivate(dto);
		return dto;
	}
	
	@WriteTransaction
	@Override
	public RealmPrincipal createRealmPrincipals(RealmPrincipal dto) {
		// remove the principals for the realm, prior to recreating them
		jdbcTemplate.update(DELETE_PRINCIPALS_SQL, dto.getRealmId());
		// create realm principals
		List<DBORealmPrincipal> principals = copyRealmPrincipalsToDBOList(dto);
		if (!principals.isEmpty()) {
			basicDao.createBatch(principals);
		}
		return dto;
	}

	@Override
	public Realm getRealm(String id) {
		SqlParameterSource param = new SinglePrimaryKeySqlParameterSource(id);
		Optional<DBORealm> dboOptional = basicDao.getObjectByPrimaryKeyIfExists(DBORealm.class, param);
		if (dboOptional.isEmpty()) {
			throw new NotFoundException(id);
		}
		DBORealm dbo = dboOptional.get();
		Realm result = copyDBORealmToRealm(dbo);
		List<DBORealmIdentityProvider> idps = jdbcTemplate.query(REALM_IDP_SQL, new RowMapper<DBORealmIdentityProvider>(){
			@Override
			public DBORealmIdentityProvider mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBORealmIdentityProvider dboRealmIdp = new DBORealmIdentityProvider();
				dboRealmIdp.setRealmId(rs.getLong(COL_REALM_IDP_REALM_ID));
				dboRealmIdp.setIdentityProvider(rs.getString(COL_REALM_IDP_PROVIDER));
				return dboRealmIdp;
			}}, id);
		copyRealmIdpsToRealm(idps, result);
		return result;
	}

	@Override
	public RealmPrincipal getRealmPrincipals(String id) {
		RealmPrincipal result = new RealmPrincipal();
		List<DBORealmPrincipal> principals = jdbcTemplate.query(REALM_PRINCIPAL_SQL, (new DBORealmPrincipal()).getTableMapping(), id);
		copyRealmPrincipalsToRealm(principals, result);
		result.setRealmId(id);
		return result;
	}
	
	@Override
	public Optional<String> getRealmIdForIdentityProvider(IdentityProvider identityProvider) {
		try {
			return Optional.of(jdbcTemplate.queryForObject(REALM_FOR_IDP_SQL, String.class, identityProviderName(identityProvider)));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public RealmIdList listRealmIds() {
		RealmIdList result = new RealmIdList();
		result.setRealms(
			namedJdbcTemplate.queryForList(SELECT_ALL_IDS_SQL, new MapSqlParameterSource(), String.class)
		);
		return result;
	}

	@WriteTransaction
	@Override
	public void deleteRealmPrincipals(String id) {
		jdbcTemplate.update(DELETE_PRINCIPALS_SQL, id);
	}

	@WriteTransaction
	@Override
	public void deleteRealm(String id) {
		// remove the IDPs for this realm
		jdbcTemplate.update(DELETE_IDPS_SQL, id);
		// remove the realm
		jdbcTemplate.update(DELETE_REALM_SQL, id);
	}

	@WriteTransaction
	@Override
	public void bootstrapDefaultRealm() {
		SqlParameterSource param = new SinglePrimaryKeySqlParameterSource(AuthorizationConstants.DEFAULT_REALM_ID);
		Optional<DBORealm> dboOptional = basicDao.getObjectByPrimaryKeyIfExists(DBORealm.class, param);
		if (dboOptional.isPresent()) {
			return;
		}
		Realm defaultRealm = new Realm();
		defaultRealm.setId(AuthorizationConstants.DEFAULT_REALM_ID);
		defaultRealm.setName(DEFAULT_REALM_NAME);
		List<IdentityProvider> idps = new ArrayList<IdentityProvider>();
		SynapseIdentityProvider synapseIdp = new SynapseIdentityProvider();
		idps.add(synapseIdp);
		OAuthIdentityProvider googleIdp = new OAuthIdentityProvider();
		googleIdp.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		idps.add(googleIdp);
		OAuthIdentityProvider orcidIdp = new OAuthIdentityProvider();
		orcidIdp.setProvider(OAuthProvider.ORCID);
		idps.add(orcidIdp);
		defaultRealm.setIdentityProvider(idps);
		createPrivate(defaultRealm);
	}

	@WriteTransaction
	@Override
	public void addPrincipalsToDefaultRealm() {
		RealmPrincipal realmPrincipal = new RealmPrincipal();
		realmPrincipal.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
		// Note, that the Synapse Administrator group is doing 'double duty' here by also being the
		// administrative group for the default realm.
		realmPrincipal.setAdministrativeGroup(BOOTSTRAP_PRINCIPAL.ADMINISTRATORS_GROUP.getPrincipalId().toString());
		realmPrincipal.setAnonymousUser(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString());
		realmPrincipal.setAuthenticatedUsers(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().toString());
		realmPrincipal.setPublicGroup(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId().toString());
		createRealmPrincipals(realmPrincipal);
	}
	
	@Override
	public Optional<String> getRealmForAnonymousPrincipal(String principalId) {
		try {
			return Optional.of(jdbcTemplate.queryForObject(SELECT_REALM_FOR_ANONYNOUS_PRINCIPAL_SQL, String.class, principalId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}
}
