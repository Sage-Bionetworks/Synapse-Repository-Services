package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_PROVIDER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_SUBJECT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_OIDC_BINDING;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PrincipalOIDCBindingDaoImpl implements PrincipalOIDCBindingDao {

	private IdGenerator idGenerator;
	private DBOBasicDao basicDao;
	private JdbcTemplate jdbcTemplate;

	public PrincipalOIDCBindingDaoImpl(IdGenerator idGenerator, DBOBasicDao basicDao, JdbcTemplate jdbcTemplate) {
		this.idGenerator = idGenerator;
		this.basicDao = basicDao;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public void bindPrincipalToSubject(Long principalId, OAuthProvider provider, String subject) {
		DBOPrincipalOIDCBinding binding = new DBOPrincipalOIDCBinding();

		binding.setId(idGenerator.generateNewId(IdType.PRINCIPAL_OIDC_BINDING_ID));
		binding.setCreatedOn(Timestamp.from(Instant.now()));
		binding.setPrincipalId(principalId);
		binding.setProvider(provider.name());
		binding.setSubject(subject);

		basicDao.createNew(binding);
	}

	@Override
	public Optional<Long> findBindingForSubject(OAuthProvider provider, String subject) {
		String sql = "SELECT " + COL_PRINCIPAL_OIDC_BINDING_PRINCIPAL_ID + " FROM " + TABLE_PRINCIPAL_OIDC_BINDING + " WHERE "
				+ COL_PRINCIPAL_OIDC_BINDING_PROVIDER + "=? AND " + COL_PRINCIPAL_OIDC_BINDING_SUBJECT + "=?";
		try {
			return Optional.of(jdbcTemplate.queryForObject(sql, Long.class, provider.name(), subject));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}
	
	@Override
	public void truncateAll() {
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_PRINCIPAL_OIDC_BINDING);
	}

}
