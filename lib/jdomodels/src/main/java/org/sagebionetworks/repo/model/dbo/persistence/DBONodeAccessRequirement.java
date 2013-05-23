package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ACCESS_REQUIREMENT_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_NODE_ACCESS_REQUIREMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE_ACCESS_REQUIREMENT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBONodeAccessRequirement implements MigratableDatabaseObject<DBONodeAccessRequirement, DBONodeAccessRequirement> {

	private Long nodeId;
	private Long accessRequirementId;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("nodeId", COL_NODE_ACCESS_REQUIREMENT_NODE_ID, true),
		new FieldColumn("accessRequirementId", COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID).withIsBackupId(true)
	};

	@Override
	public TableMapping<DBONodeAccessRequirement> getTableMapping() {

		return new TableMapping<DBONodeAccessRequirement>(){

			@Override
			public DBONodeAccessRequirement mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBONodeAccessRequirement nar = new DBONodeAccessRequirement();
				nar.setNodeId(rs.getLong(COL_NODE_ACCESS_REQUIREMENT_NODE_ID));
				nar.setAccessRequirementId(rs.getLong(COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID));
				return nar;
			}

			@Override
			public String getTableName() {
				return TABLE_NODE_ACCESS_REQUIREMENT;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_NODE_ACCESS_REQUIREMENT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBONodeAccessRequirement> getDBOClass() {
				return DBONodeAccessRequirement.class;
			}};
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public Long getAccessRequirementId() {
		return accessRequirementId;
	}

	public void setAccessRequirementId(Long accessRequirementId) {
		this.accessRequirementId = accessRequirementId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((accessRequirementId == null) ? 0 : accessRequirementId
						.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
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
		DBONodeAccessRequirement other = (DBONodeAccessRequirement) obj;
		if (accessRequirementId == null) {
			if (other.accessRequirementId != null)
				return false;
		} else if (!accessRequirementId.equals(other.accessRequirementId))
			return false;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBONodeAccessRequirement [nodeId=" + nodeId
				+ ", accessRequirementId=" + accessRequirementId + "]";
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.NODE_ACCESS_REQUIRMENT;
	}

	@Override
	public MigratableTableTranslation<DBONodeAccessRequirement, DBONodeAccessRequirement> getTranslator() {
		return new MigratableTableTranslation<DBONodeAccessRequirement, DBONodeAccessRequirement>(){

			@Override
			public DBONodeAccessRequirement createDatabaseObjectFromBackup(
					DBONodeAccessRequirement backup) {
				return backup;
			}

			@Override
			public DBONodeAccessRequirement createBackupFromDatabaseObject(
					DBONodeAccessRequirement dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBONodeAccessRequirement> getBackupClass() {
		return DBONodeAccessRequirement.class;
	}

	@Override
	public Class<? extends DBONodeAccessRequirement> getDatabaseObjectClass() {
		return DBONodeAccessRequirement.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}


}
