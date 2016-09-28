package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccess;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccessType;
import org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.AclModificationMessage;
import org.sagebionetworks.repo.model.message.AclModificationType;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class DBOAccessControlListDaoImpl implements AccessControlListDAO {
	static private Log log = LogFactory.getLog(DBOAccessControlListDaoImpl.class);	
	private static final String IDS_PARAM_NAME = "ids_param";

	private static final String SELECT_ACCESS_TYPES_FOR_RESOURCE = "SELECT "+COL_RESOURCE_ACCESS_TYPE_ELEMENT+" FROM "+TABLE_RESOURCE_ACCESS_TYPE+" WHERE "+COL_RESOURCE_ACCESS_TYPE_ID+" = ?";

	private static final String DELETE_RESOURCE_ACCESS_SQL = "DELETE FROM "+TABLE_RESOURCE_ACCESS+" WHERE "+COL_RESOURCE_ACCESS_OWNER+" = ?";

	private static final String SELECT_ALL_RESOURCE_ACCESS = "SELECT * FROM "+TABLE_RESOURCE_ACCESS+" WHERE "+COL_RESOURCE_ACCESS_OWNER+" = ?";

	private static final String SELECT_FOR_UPDATE = "SELECT * FROM "+TABLE_ACCESS_CONTROL_LIST+
			" WHERE "+COL_ACL_OWNER_ID+" = :" + COL_ACL_OWNER_ID+" AND "+COL_ACL_OWNER_TYPE+" = :" + COL_ACL_OWNER_TYPE+" FOR UPDATE";

	private static final String SQL_SELECT_ALL_ACL_WITH_ACL_ID = "SELECT * FROM "+TABLE_ACCESS_CONTROL_LIST+" WHERE "+COL_ACL_ID+" = ?";

	private static final String SQL_SELECT_OWNER_TYPE_FOR_RESOURCE = "SELECT "+COL_ACL_OWNER_TYPE+" FROM "+TABLE_ACCESS_CONTROL_LIST+" WHERE "+COL_ACL_ID+" = ?";

	private static final String SQL_SELECT_ACL_ID_FOR_RESOURCE = "SELECT "+COL_ACL_ID+" FROM "+TABLE_ACCESS_CONTROL_LIST+
			" WHERE "+COL_ACL_OWNER_ID+" = ? AND "+COL_ACL_OWNER_TYPE+" = ?";
	
	private static final String SQL_SELECT_ACL_IDS_FOR_RESOURCE = "SELECT "+COL_ACL_ID +" FROM "+TABLE_ACCESS_CONTROL_LIST+
			" WHERE "+COL_ACL_OWNER_ID+" IN (:"+IDS_PARAM_NAME+") AND "+COL_ACL_OWNER_TYPE+" = :" + COL_ACL_OWNER_TYPE + " ORDER BY " + COL_ACL_OWNER_ID;
	
	private static final String SQL_DELETE_ACLS_BY_IDS = "DELETE FROM " + TABLE_ACCESS_CONTROL_LIST + 
														" WHERE " + COL_ACL_OWNER_ID + " IN (:"+IDS_PARAM_NAME+")"+
														" AND " +  COL_ACL_OWNER_TYPE + " = :" + DBOAccessControlList.OWNER_TYPE_FIELD_NAME;

	private static final String SQL_GET_ALL_USER_GROUP = "SELECT DISTINCT "+COL_RESOURCE_ACCESS_GROUP_ID
			+" FROM "+TABLE_ACCESS_CONTROL_LIST+", "+TABLE_RESOURCE_ACCESS+", "+TABLE_RESOURCE_ACCESS_TYPE
			+" WHERE "+TABLE_ACCESS_CONTROL_LIST+"."+COL_ACL_ID+" = "+TABLE_RESOURCE_ACCESS+"."+COL_RESOURCE_ACCESS_OWNER
			+" AND "+TABLE_RESOURCE_ACCESS+"."+COL_RESOURCE_ACCESS_ID+" = "+TABLE_RESOURCE_ACCESS_TYPE+"."+COL_RESOURCE_ACCESS_TYPE_ID
			+" AND "+ TABLE_ACCESS_CONTROL_LIST+"."+COL_ACL_OWNER_ID +" = ?"
			+" AND "+ TABLE_ACCESS_CONTROL_LIST+"."+COL_ACL_OWNER_TYPE +" = ?"
			+" AND "+ TABLE_RESOURCE_ACCESS_TYPE+"."+COL_RESOURCE_ACCESS_TYPE_ELEMENT +" = ?";

	/**
	 * Keep a copy of the row mapper.
	 */
	private static RowMapper<DBOAccessControlList> aclRowMapper = (new DBOAccessControlList()).getTableMapping();

	private static final RowMapper<DBOResourceAccess> accessMapper = new DBOResourceAccess().getTableMapping();
	
	private static final RowMapper<Long> rowMapperACLId= new RowMapper<Long>(){
		@Override
		public
		Long mapRow(ResultSet rs, int rowNum) throws SQLException{
			return rs.getLong(COL_ACL_ID);
		}
	};
	
	private static final Function<ResourceAccess, Long> RESOURCE_ACCESS_TO_PRINCIPAL_TRANSFORMER = new Function<ResourceAccess, Long>() {
		@Override
		public Long apply(ResourceAccess input) {
			return input.getPrincipalId();
		}
	};

	@Autowired
	private DBOBasicDao dboBasicDao;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	TransactionalMessenger transactionalMessenger;
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@WriteTransaction
	@Override
	public String create(AccessControlList acl, ObjectType ownerType) throws DatastoreException, NotFoundException {

		if (acl == null) {
			throw new IllegalArgumentException("ACL cannot be null.");
		}

		acl.setEtag(UUID.randomUUID().toString());

		AccessControlListUtils.validateACL(acl);

		DBOAccessControlList dbo = AccessControlListUtils.createDBO(acl, idGenerator.generateNewId(TYPE.ACL_ID), ownerType);
		dboBasicDao.createNew(dbo);
		populateResourceAccess(dbo.getId(), acl.getResourceAccess());

		transactionalMessenger.sendMessageAfterCommit(dbo.getId().toString(), ObjectType.ACCESS_CONTROL_LIST, 
				acl.getEtag(), ChangeType.CREATE);
		return acl.getId(); // This preserves the "syn" prefix
	}

	/**
	 * Populate the resource access table after the ACL table is ready.
	 * @param acl
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	private void populateResourceAccess(long dboId, Set<ResourceAccess> resourceAccess)
			throws DatastoreException, NotFoundException {
		// Now create each Resource Access
		for(ResourceAccess ra: resourceAccess) {
			DBOResourceAccess dboRa = new DBOResourceAccess();
			// assign an id
			dboRa.setId(idGenerator.generateNewId(TYPE.ACL_RES_ACC_ID));
			dboRa.setOwner(dboId);
			if (ra.getPrincipalId()==null) {
				throw new IllegalArgumentException("ResourceAccess cannot have null principalID");
			} else {
				dboRa.setUserGroupId(ra.getPrincipalId());
			}
			dboRa = dboBasicDao.createNew(dboRa);
			// Now add all of the access
			Set<ACCESS_TYPE> access = ra.getAccessType();
			List<DBOResourceAccessType> batch = AccessControlListUtils.createResourceAccessTypeBatch(dboRa.getId(), dboId, access);
			// Add the batch
			dboBasicDao.createBatch(batch);
		}
	}

	@Override
	public AccessControlList get(final String ownerId, final ObjectType ownerType)
			throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOAccessControlList.OWNER_ID_FIELD_NAME, KeyFactory.stringToKey(ownerId));
		param.addValue(DBOAccessControlList.OWNER_TYPE_FIELD_NAME, ownerType.name());
		DBOAccessControlList dboAcl = dboBasicDao.getObjectByPrimaryKey(DBOAccessControlList.class, param);
		AccessControlList acl = doGet(dboAcl);
		return acl;
	}

	@Override
	public AccessControlList get(Long id) throws DatastoreException, NotFoundException {
		DBOAccessControlList dboAcl = getDboAcl(id);
		AccessControlList acl = doGet(dboAcl);
		return acl;
	}

	@Override
	public Long getAclId(String id, ObjectType objectType)
			throws DatastoreException, NotFoundException {
		try {
			return jdbcTemplate.queryForObject(SQL_SELECT_ACL_ID_FOR_RESOURCE, Long.class, KeyFactory.stringToKey(id), objectType.name());
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Acl " + id + " not found");
		}
	}
	
	@Override
	public List<Long> getAclIds(List<Long> nodeIds, ObjectType objectType){
		ValidateArgument.required(nodeIds, "nodeIds");
		ValidateArgument.required(objectType, "objectType");
		
		if(nodeIds.isEmpty()){ //no need to query database
			return new ArrayList<Long>();
		}
		try {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue(IDS_PARAM_NAME, nodeIds); 
			params.addValue(COL_ACL_OWNER_TYPE, objectType.name());
			return namedParameterJdbcTemplate.query(SQL_SELECT_ACL_IDS_FOR_RESOURCE, params, rowMapperACLId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("None of the Acl ids could not be found");
		}
	}

	@Override
	public ObjectType getOwnerType(Long id) throws DatastoreException, NotFoundException {
		try {
			return ObjectType.valueOf(jdbcTemplate.queryForObject(SQL_SELECT_OWNER_TYPE_FOR_RESOURCE, String.class, id));
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("owner type " + id + " not found");
		}
	}
	
	private DBOAccessControlList getDboAcl(Long id) throws DatastoreException, NotFoundException {
		List<DBOAccessControlList> dboList = null;
		dboList = jdbcTemplate.query(SQL_SELECT_ALL_ACL_WITH_ACL_ID, aclRowMapper, id);
		if (dboList.size() != 1) {
			throw new NotFoundException("Acl " + id + " not found");
		}
		return dboList.get(0);
	}

	/*
	 * allows us to get the ACL with different parameters
	 */
	private AccessControlList doGet(DBOAccessControlList dboAcl) throws NotFoundException {
		ObjectType ownerType = ObjectType.valueOf(dboAcl.getOwnerType());
		AccessControlList acl = AccessControlListUtils.createAcl(dboAcl, ownerType);
		// Now fetch the rest of the data for this ACL
		List<DBOResourceAccess> raList = jdbcTemplate.query(SELECT_ALL_RESOURCE_ACCESS, accessMapper, dboAcl.getId());
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		acl.setResourceAccess(raSet);
		for(DBOResourceAccess raDbo: raList){
			List<String> typeList = jdbcTemplate.query(SELECT_ACCESS_TYPES_FOR_RESOURCE, new RowMapper<String>(){
				@Override
				public String mapRow(ResultSet rs, int rowNum)throws SQLException {
					return rs.getString(COL_RESOURCE_ACCESS_TYPE_ELEMENT);
				}}, raDbo.getId());
			// build up this type
			ResourceAccess ra = new ResourceAccess();
			ra.setPrincipalId(raDbo.getUserGroupId());
			ra.setAccessType(new HashSet<ACCESS_TYPE>());
			for(String typeString: typeList){
				ra.getAccessType().add(ACCESS_TYPE.valueOf(typeString));
			}
			raSet.add(ra);
		}
		return acl;
	}

	@WriteTransaction
	@Override
	public void update(AccessControlList acl, ObjectType ownerType) throws DatastoreException, NotFoundException {

		AccessControlListUtils.validateACL(acl);

		// Check e-tags before update
		final Long ownerKey = KeyFactory.stringToKey(acl.getId());
		DBOAccessControlList origDbo = selectForUpdate(ownerKey, ownerType);
		String etag = origDbo.getEtag();
		if (!acl.getEtag().equals(etag)) {
			throw new ConflictingUpdateException("E-tags do not match.");
		}
		acl.setEtag(UUID.randomUUID().toString());

		AccessControlList oldAcl = null;
		if (ownerType == ObjectType.ENTITY) {
			oldAcl = get(acl.getId(), ObjectType.ENTITY);
		}

		DBOAccessControlList dbo = AccessControlListUtils.createDBO(acl, origDbo.getId(), ownerType);
		dboBasicDao.update(dbo);
		// Now delete the resource access
		jdbcTemplate.update(DELETE_RESOURCE_ACCESS_SQL, dbo.getId());
		// Now recreate it from the passed data.
		populateResourceAccess(dbo.getId(), acl.getResourceAccess());

		transactionalMessenger.sendMessageAfterCommit(dbo.getId().toString(), ObjectType.ACCESS_CONTROL_LIST, 
				acl.getEtag(), ChangeType.UPDATE);

		if (ownerType == ObjectType.ENTITY) {
			// Now we compare the old and the new acl to see what might have changed, so we can send notifications out.
			// We only care about principals being added or removed, not what exactly has happened.
			Set<Long> oldPrincipals = Transform.toSet(oldAcl.getResourceAccess(), RESOURCE_ACCESS_TO_PRINCIPAL_TRANSFORMER);
			Set<Long> newPrincipals = Transform.toSet(acl.getResourceAccess(), RESOURCE_ACCESS_TO_PRINCIPAL_TRANSFORMER);

			SetView<Long> addedPrincipals = Sets.difference(newPrincipals, oldPrincipals);
			SetView<Long> deletedPrincipals = Sets.difference(oldPrincipals, newPrincipals);

			for (Long principal : deletedPrincipals) {
				AclModificationMessage message = new AclModificationMessage();
				message.setObjectId(acl.getId());
				message.setObjectType(ownerType);
				message.setPrincipalId(principal);
				message.setAclModificationType(AclModificationType.PRINCIPAL_REMOVED);
				transactionalMessenger.sendModificationMessageAfterCommit(message);
			}

			for (Long principal : addedPrincipals) {
				AclModificationMessage message = new AclModificationMessage();
				message.setObjectId(acl.getId());
				message.setObjectType(ownerType);
				message.setPrincipalId(principal);
				message.setAclModificationType(AclModificationType.PRINCIPAL_ADDED);
				transactionalMessenger.sendModificationMessageAfterCommit(message);
			}
		}
	}

	@WriteTransaction
	@Override
	public void delete(String ownerId, ObjectType ownerType) throws DatastoreException {
		final Long ownerKey = KeyFactory.stringToKey(ownerId);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(DBOAccessControlList.OWNER_ID_FIELD_NAME, ownerKey);
		params.addValue(DBOAccessControlList.OWNER_TYPE_FIELD_NAME, ownerType.name());

		try {
			Long dboId = getAclId(ownerId, ownerType);
			dboBasicDao.deleteObjectByPrimaryKey(DBOAccessControlList.class, params);
			transactionalMessenger.sendMessageAfterCommit(dboId.toString(), ObjectType.ACCESS_CONTROL_LIST, 
					UUID.randomUUID().toString(), ChangeType.DELETE);
		} catch (NotFoundException e) {
			// if there is no valid AclId for this ownerId and ownerType, do nothing
			log.info("Atempted to delete an ACL that does not exist. OwnerId: " + ownerId + ", ownerType: " + ownerType);
		}
	}
	
	@WriteTransactionReadCommitted
	@Override
	public int delete(List<Long> ownerIds, ObjectType ownerType) throws DatastoreException {
		ValidateArgument.required(ownerIds, "ownerIds");
		ValidateArgument.required(ownerType, "ownerType");
		
		if(ownerIds.isEmpty()){
			//no need to update database if not deleting anything
			return 0;
		}
		
		List<Long> aclIds = getAclIds(ownerIds, ownerType);
		for(Long aclId : aclIds){
			transactionalMessenger.sendMessageAfterCommit(aclId.toString(), ObjectType.ACCESS_CONTROL_LIST, UUID.randomUUID().toString(), ChangeType.DELETE);
		}
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(IDS_PARAM_NAME, ownerIds);
		params.addValue(DBOAccessControlList.OWNER_TYPE_FIELD_NAME, ownerType.name());
		return namedParameterJdbcTemplate.update(SQL_DELETE_ACLS_BY_IDS, params);
		
	}

	@Override
	public boolean canAccess(Set<Long> groups, String resourceId, ObjectType resourceType,
			ACCESS_TYPE accessType) throws DatastoreException {
		Long idLong = KeyFactory.stringToKey(resourceId);
		HashSet<Long> benefactors = Sets.newHashSet(idLong);
		Set<Long> results = getAccessibleBenefactors(groups, benefactors, resourceType, accessType);
		return results.contains(idLong);
	}
	
	// To avoid potential race conditions, we do "SELECT ... FOR UPDATE" on etags.
	private DBOAccessControlList selectForUpdate(final Long ownerId, ObjectType ownerType) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACL_OWNER_ID, ownerId);
		param.addValue(COL_ACL_OWNER_TYPE, ownerType.name());
		return namedParameterJdbcTemplate.queryForObject(SELECT_FOR_UPDATE, param, aclRowMapper);
	}

	@Override
	public Set<Long> getAccessibleBenefactors(Set<Long> groups, Set<Long> benefactors,
			ObjectType resourceType, ACCESS_TYPE accessType) {
		ValidateArgument.required(groups, "groups");
		ValidateArgument.required(benefactors, "benefactors");
		ValidateArgument.required(resourceType, "resourceType");
		ValidateArgument.required(accessType, "accessType");
		if(groups.isEmpty() || benefactors.isEmpty()){
			// there will be no matches for empty inputs.
			return new HashSet<Long>(0);
		}
		Map<String, Object> namedParameters = new HashMap<String, Object>(4);
		namedParameters.put(AuthorizationSqlUtil.RESOURCE_ID_BIND_VAR, benefactors);
		namedParameters.put(AuthorizationSqlUtil.PRINCIPAL_IDS_BIND_VAR, groups);
		namedParameters.put(AuthorizationSqlUtil.RESOURCE_TYPE_BIND_VAR, resourceType.name());
		namedParameters.put(AuthorizationSqlUtil.ACCESS_TYPE_BIND_VAR, accessType.name());
		// query
		List<Long> result = namedParameterJdbcTemplate.query(AuthorizationSqlUtil.SELECT_RESOURCE_INTERSECTION, namedParameters, new RowMapper<Long>() {

			@Override
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getLong(COL_ACL_OWNER_ID);
			}
		});
		return new HashSet<Long>(result);
	}

	@Override
	public Set<String> getPrincipalIds(String objectId, ObjectType objectType, ACCESS_TYPE accessType){
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(accessType, "accessType");
		Set<String> results = new HashSet<String>();
		results.addAll(jdbcTemplate.queryForList(SQL_GET_ALL_USER_GROUP, String.class, KeyFactory.stringToKey(objectId), objectType.name(), accessType.name()));
		return results;
	}
}
