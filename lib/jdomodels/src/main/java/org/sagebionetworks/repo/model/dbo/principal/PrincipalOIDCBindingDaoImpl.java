package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_PROVIDER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_SUBJECT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_OIDC_BINDING;

import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PrincipalOIDCBindingDaoImpl implements PrincipalOIDCBindingDao {

	private IdGenerator idGenerator;
	private JdbcTemplate jdbcTemplate;

	public PrincipalOIDCBindingDaoImpl(IdGenerator idGenerator, JdbcTemplate jdbcTemplate) {
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public void bindPrincipalToSubject(Long principalId, OAuthProvider provider, String subject) {
		String sql = "INSERT IGNORE INTO " + TABLE_PRINCIPAL_OIDC_BINDING + "(" 
			+ COL_PRINCIPAL_OIDC_BINDING_ID + ", " 
			+ COL_PRINCIPAL_OIDC_BINDING_CREATED_ON + ","
			+ COL_PRINCIPAL_OIDC_BINDING_PRINCIPAL_ID + ","
			+ COL_PRINCIPAL_OIDC_BINDING_PROVIDER + ","
			+ COL_PRINCIPAL_OIDC_BINDING_SUBJECT 
		+ ") VALUES (?, NOW(), ?, ?, ?)";
		
		Long newId = idGenerator.generateNewId(IdType.PRINCIPAL_OIDC_BINDING_ID);
		
		jdbcTemplate.update(sql, newId, principalId, provider.name(), subject);
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
