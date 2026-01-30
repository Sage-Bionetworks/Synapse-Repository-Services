package org.sagebionetworks.repo.model.dbo.persistence;


import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_PRINCIPAL_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_PRINCIPAL_PRINCIPAL_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_PRINCIPAL_REALM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_REALM_PRINCIPAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REALM_PRINCIPAL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Mapping between groups and nodes. Used to relate Teams to Challenges
 */
public class DBORealmPrincipal implements MigratableDatabaseObject<DBORealmPrincipal, DBORealmPrincipal> {

	private Long id;
	private Long realmId;
	private Long principalId;
	private String principalType;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_REALM_PRINCIPAL_ID, true).withIsBackupId(true),
			new FieldColumn("realmId", COL_REALM_PRINCIPAL_REALM_ID),
			new FieldColumn("principalId", COL_REALM_PRINCIPAL_PRINCIPAL_ID),
			new FieldColumn("principalType", COL_REALM_PRINCIPAL_PRINCIPAL_TYPE)
	};

	@Override
	public TableMapping<DBORealmPrincipal> getTableMapping() {
		return new TableMapping<DBORealmPrincipal>() {
			
			@Override
			public DBORealmPrincipal mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBORealmPrincipal dbo = new DBORealmPrincipal();
				dbo.setId(rs.getLong(COL_REALM_PRINCIPAL_ID));
				dbo.setRealmId(rs.getLong(COL_REALM_PRINCIPAL_REALM_ID));
				dbo.setPrincipalId(rs.getLong(COL_REALM_PRINCIPAL_PRINCIPAL_ID));
				dbo.setPrincipalType(rs.getString(COL_REALM_PRINCIPAL_PRINCIPAL_TYPE));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_REALM_PRINCIPAL;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_FILE_REALM_PRINCIPAL;
			}
			
			@Override
			public Class<? extends DBORealmPrincipal> getDBOClass() {
				return DBORealmPrincipal.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.REALM_PRINCIPAL;
	}

	@Override
	public MigratableTableTranslation<DBORealmPrincipal, DBORealmPrincipal> getTranslator() {
		// We do not currently have a backup for this object.
		return new BasicMigratableTableTranslation<DBORealmPrincipal>();
	}

	@Override
	public Class<? extends DBORealmPrincipal> getBackupClass() {
		return DBORealmPrincipal.class;
	}

	@Override
	public Class<? extends DBORealmPrincipal> getDatabaseObjectClass() {
		return DBORealmPrincipal.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getRealmId() {
		return realmId;
	}

	public void setRealmId(Long realmId) {
		this.realmId = realmId;
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}

	public String getPrincipalType() {
		return principalType;
	}

	public void setPrincipalType(String principalType) {
		this.principalType = principalType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((principalId == null) ? 0 : principalId.hashCode());
		result = prime * result + ((principalType == null) ? 0 : principalType.hashCode());
		result = prime * result + ((realmId == null) ? 0 : realmId.hashCode());
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
		DBORealmPrincipal other = (DBORealmPrincipal) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		if (principalType == null) {
			if (other.principalType != null)
				return false;
		} else if (!principalType.equals(other.principalType))
			return false;
		if (realmId == null) {
			if (other.realmId != null)
				return false;
		} else if (!realmId.equals(other.realmId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBORealmPrincipal [id=" + id + ", realmId=" + realmId + ", principalId=" + principalId
				+ ", principalType=" + principalType + "]";
	}



}
