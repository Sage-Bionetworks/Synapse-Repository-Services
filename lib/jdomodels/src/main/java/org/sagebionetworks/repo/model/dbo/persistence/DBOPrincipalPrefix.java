package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_PREFIX_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_PREFIX_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_PRINCIPAL_PREFIX;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_PREFIX;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * This table is populated by a worker and queried by the services to lookup
 * principals using a prefix. This table is not migrated.
 * 
 * @author John
 *
 */
public class DBOPrincipalPrefix implements DatabaseObject<DBOPrincipalPrefix> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("token", COL_PRINCIPAL_PREFIX_TOKEN).withIsPrimaryKey(true),
			new FieldColumn("principalId", COL_PRINCIPAL_PREFIX_PRINCIPAL_ID).withIsPrimaryKey(true) };

	private String token;
	private Long principalId;

	@Override
	public TableMapping<DBOPrincipalPrefix> getTableMapping() {
		return new TableMapping<DBOPrincipalPrefix>() {

			@Override
			public DBOPrincipalPrefix mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOPrincipalPrefix dbo = new DBOPrincipalPrefix();
				dbo.setPrincipalId(rs.getLong(COL_PRINCIPAL_PREFIX_PRINCIPAL_ID));
				dbo.setToken(rs.getString(COL_PRINCIPAL_PREFIX_TOKEN));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_PRINCIPAL_PREFIX;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_PRINCIPAL_PREFIX;
			}

			@Override
			public Class<? extends DBOPrincipalPrefix> getDBOClass() {
				return DBOPrincipalPrefix.class;
			}
		};
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
		return Objects.hash(principalId, token);
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
		return Objects.equals(principalId, other.principalId) && Objects.equals(token, other.token);
	}

	@Override
	public String toString() {
		return "DBOPrincipalPrefix [token=" + token + ", principalId=" + principalId + "]";
	}

}
