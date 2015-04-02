package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_PREFIX_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_PREFIX_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_PREFIX;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * This table is populated by a worker and queried by the services to lookup principals using a prefix.
 * This table is not migrated.
 * 
 * @author John
 *
 */
@Table(name = TABLE_PRINCIPAL_PREFIX, constraints = { "INDEX PREFIX_TOKEN_INDEX (" + COL_PRINCIPAL_PREFIX_TOKEN +")"})
public class DBOPrincipalPrefix implements DatabaseObject<DBOPrincipalPrefix>{
	
	private static TableMapping<DBOPrincipalPrefix> mapping = AutoTableMapping.create(DBOPrincipalPrefix.class);

	@Field(name = COL_PRINCIPAL_PREFIX_TOKEN, nullable = false, varchar = 256, primary=true)
	private String token;
	
	@Field(name = COL_PRINCIPAL_PREFIX_PRINCIPAL_ID, nullable = false, primary=true)
	@ForeignKey(name = "PREFIX_USR_ID_FK", table = TABLE_USER_GROUP, field = COL_USER_GROUP_ID, cascadeDelete = true)
	private Long principalId;
	
	@Override
	public TableMapping<DBOPrincipalPrefix> getTableMapping() {
		return mapping;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((principalId == null) ? 0 : principalId.hashCode());
		result = prime * result + ((token == null) ? 0 : token.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOPrincipalPrefix other = (DBOPrincipalPrefix) obj;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOPrincipalPrefix [token=" + token + ", principalId="
				+ principalId + "]";
	}

}
