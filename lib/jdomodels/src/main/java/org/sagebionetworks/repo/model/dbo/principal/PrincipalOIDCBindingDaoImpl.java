package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_PROVIDER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_SUBJECT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_OIDC_BINDING;

import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PrincipalOIDCBindingDaoImpl implements PrincipalOIDCBindingDao {

	private static TableMapping<DBOPrincipalOIDCBinding> MAPPER = new DBOPrincipalOIDCBinding().getTableMapping();
	private IdGenerator idGenerator;
	private JdbcTemplate jdbcTemplate;
	
	public PrincipalOIDCBindingDaoImpl(IdGenerator idGenerator, JdbcTemplate jdbcTemplate) {
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public void bindPrincipalToSubject(Long principalId, Long aliasId, OAuthProvider provider, String subject) {
		String sql = "INSERT IGNORE INTO " + TABLE_PRINCIPAL_OIDC_BINDING + "(" 
			+ COL_PRINCIPAL_OIDC_BINDING_ID + ", "
			+ COL_PRINCIPAL_OIDC_BINDING_ETAG + ", " 
			+ COL_PRINCIPAL_OIDC_BINDING_CREATED_ON + ","
			+ COL_PRINCIPAL_OIDC_BINDING_PRINCIPAL_ID + ","
			+ COL_PRINCIPAL_OIDC_BINDING_ALIAS_ID + ","
			+ COL_PRINCIPAL_OIDC_BINDING_PROVIDER + ","
			+ COL_PRINCIPAL_OIDC_BINDING_SUBJECT 
		+ ") VALUES (?, UUID(), NOW(), ?, ?, ?, ?)";
		
		Long newId = idGenerator.generateNewId(IdType.PRINCIPAL_OIDC_BINDING_ID);
		
		jdbcTemplate.update(sql, newId, principalId, aliasId, provider.name(), subject);
	}

	@Override
	public Optional<PrincipalOidcBinding> findBindingForSubject(OAuthProvider provider, String subject) {
		String sql = "SELECT * FROM " + TABLE_PRINCIPAL_OIDC_BINDING + " WHERE "
				+ COL_PRINCIPAL_OIDC_BINDING_PROVIDER + "=? AND " + COL_PRINCIPAL_OIDC_BINDING_SUBJECT + "=?";
		try {
			return Optional.of(jdbcTemplate.queryForObject(sql, MAPPER, provider.name(), subject))
				.map( dbo -> new PrincipalOidcBinding()
					.setBindingId(dbo.getId())
					.setProvider(OAuthProvider.valueOf(dbo.getProvider()))
					.setSubject(dbo.getSubject())
					.setUserId(dbo.getPrincipalId())
					.setAliasId(dbo.getAliasId())
					);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}
	
	@Override
	@WriteTransaction
	public void setBindingAlias(Long bindingId, Long aliasId) {
		String sql = "UPDATE " + TABLE_PRINCIPAL_OIDC_BINDING + " SET " 
			+ COL_PRINCIPAL_OIDC_BINDING_ALIAS_ID + "=?,"
			+ COL_PRINCIPAL_OIDC_BINDING_ETAG + "=UUID()"
			+ " WHERE " + COL_PRINCIPAL_OIDC_BINDING_ID + "=?";
		
		jdbcTemplate.update(sql, aliasId, bindingId);
		
	}
	
	@Override
	@WriteTransaction
	public void deleteBinding(Long bindingId) {
		String sql = "DELETE FROM " +TABLE_PRINCIPAL_OIDC_BINDING + " WHERE " + COL_PRINCIPAL_OIDC_BINDING_ID + "=?";
		
		jdbcTemplate.update(sql, bindingId);
		
	}
	
	@Override
	@WriteTransaction
	public void clearBindings(Long principalId) {
		String sql = "DELETE FROM " +TABLE_PRINCIPAL_OIDC_BINDING + " WHERE " + COL_PRINCIPAL_OIDC_BINDING_PRINCIPAL_ID + "=?";
		
		jdbcTemplate.update(sql, principalId);
		
	}
	
	@Override
	public void truncateAll() {
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_PRINCIPAL_OIDC_BINDING);
	}

}
