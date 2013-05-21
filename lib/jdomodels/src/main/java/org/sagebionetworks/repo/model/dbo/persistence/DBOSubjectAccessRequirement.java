package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_SUBJECT_ACCESS_REQUIREMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SUBJECT_ACCESS_REQUIREMENT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOSubjectAccessRequirement implements MigratableDatabaseObject<DBOSubjectAccessRequirement, DBONodeAccessRequirement> {

	private Long subjectId;
	private String subjectType;
	private Long accessRequirementId;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("subjectId", COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID, true),
		new FieldColumn("subjectType", COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, true),
		new FieldColumn("accessRequirementId", COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID).withIsBackupId(true)
	};

	@Override
	public TableMapping<DBOSubjectAccessRequirement> getTableMapping() {

		return new TableMapping<DBOSubjectAccessRequirement>(){

			@Override
			public DBOSubjectAccessRequirement mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOSubjectAccessRequirement sar = new DBOSubjectAccessRequirement();
				sar.setSubjectId(rs.getLong(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID));
				sar.setSubjectType(rs.getString(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE));
				sar.setAccessRequirementId(rs.getLong(COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID));
				return sar;
			}

			@Override
			public String getTableName() {
				return TABLE_SUBJECT_ACCESS_REQUIREMENT;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_SUBJECT_ACCESS_REQUIREMENT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOSubjectAccessRequirement> getDBOClass() {
				return DBOSubjectAccessRequirement.class;
			}};
	}

	public Long getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(Long subjectId) {
		this.subjectId = subjectId;
	}

	public String getSubjectType() {
		return subjectType;
	}

	public void setSubjectType(String subjectType) {
		this.subjectType = subjectType;
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
		result = prime * result
				+ ((subjectId == null) ? 0 : subjectId.hashCode());
		result = prime * result
				+ ((subjectType == null) ? 0 : subjectType.hashCode());
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
		DBOSubjectAccessRequirement other = (DBOSubjectAccessRequirement) obj;
		if (accessRequirementId == null) {
			if (other.accessRequirementId != null)
				return false;
		} else if (!accessRequirementId.equals(other.accessRequirementId))
			return false;
		if (subjectId == null) {
			if (other.subjectId != null)
				return false;
		} else if (!subjectId.equals(other.subjectId))
			return false;
		if (subjectType != other.subjectType)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOSubjectAccessRequirement [subjectId=" + subjectId
				+ ", subjectType=" + subjectType + ", accessRequirementId="
				+ accessRequirementId + "]";
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.NODE_ACCESS_REQUIREMENT;
	}

	@Override
	public MigratableTableTranslation<DBOSubjectAccessRequirement, DBONodeAccessRequirement> getTranslator() {

		return new MigratableTableTranslation<DBOSubjectAccessRequirement, DBONodeAccessRequirement>(){

			@Override
			public DBOSubjectAccessRequirement createDatabaseObjectFromBackup(
					DBONodeAccessRequirement backup) {
				DBOSubjectAccessRequirement dbo = new DBOSubjectAccessRequirement();
				dbo.setAccessRequirementId(backup.getAccessRequirementId());
				dbo.setSubjectId(backup.getNodeId());
				dbo.setSubjectType(RestrictableObjectType.ENTITY.toString());
				return dbo;
			}

			@Override
			public DBONodeAccessRequirement createBackupFromDatabaseObject(
					DBOSubjectAccessRequirement dbo) {
				DBONodeAccessRequirement backup = new DBONodeAccessRequirement();
				RestrictableObjectType subjectType = RestrictableObjectType.valueOf(dbo.getSubjectType());
				if (subjectType!=RestrictableObjectType.ENTITY) {
					throw new IllegalStateException("Cannot create a DBONodeAccessRequirement from a "+
							"DBOSubjectAccessRequirement where subjectType is other than "+"" +
									"ENTITY.  (In this case it's "+subjectType+".)");
				}
				backup.setAccessRequirementId(dbo.getAccessRequirementId());
				backup.setNodeId(dbo.getSubjectId());
				return backup;
			}};
	}

	@Override
	public Class<? extends DBONodeAccessRequirement> getBackupClass() {
		return DBONodeAccessRequirement.class;
	}

	@Override
	public Class<? extends DBOSubjectAccessRequirement> getDatabaseObjectClass() {
		return DBOSubjectAccessRequirement.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}


}
