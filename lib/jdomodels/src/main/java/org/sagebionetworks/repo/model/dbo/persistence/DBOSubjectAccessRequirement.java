package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_BINDING_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_SUBJECT_ACCESS_REQUIREMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SUBJECT_ACCESS_REQUIREMENT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOSubjectAccessRequirement implements MigratableDatabaseObject<DBOSubjectAccessRequirement, DBOSubjectAccessRequirement> {

	private Long subjectId;
	private String subjectType;
	private Long accessRequirementId;
	private String bindingType;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("subjectId", COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID, true),
		new FieldColumn("subjectType", COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, true),
		new FieldColumn("accessRequirementId", COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID).withIsBackupId(true),
		new FieldColumn("bindingType", COL_SUBJECT_ACCESS_REQUIREMENT_BINDING_TYPE, true),
	};

	private static final MigratableTableTranslation<DBOSubjectAccessRequirement, DBOSubjectAccessRequirement> MIGRATION_MAPPER = new BasicMigratableTableTranslation<>();

	@Override
	public TableMapping<DBOSubjectAccessRequirement> getTableMapping() {

		return new TableMapping<DBOSubjectAccessRequirement>(){

			@Override
			public DBOSubjectAccessRequirement mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOSubjectAccessRequirement sar = new DBOSubjectAccessRequirement();
				sar.setSubjectId(rs.getLong(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID));
				sar.setSubjectType(rs.getString(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE));
				sar.setAccessRequirementId(rs.getLong(COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID));
				sar.setBindingType(rs.getString(COL_SUBJECT_ACCESS_REQUIREMENT_BINDING_TYPE));
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

	/**
	 * @return the bindingType
	 */
	public String getBindingType() {
		return bindingType;
	}

	/**
	 * @param bindingType the bindingType to set
	 */
	public void setBindingType(String bindingType) {
		this.bindingType = bindingType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessRequirementId, bindingType, subjectId, subjectType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOSubjectAccessRequirement)) {
			return false;
		}
		DBOSubjectAccessRequirement other = (DBOSubjectAccessRequirement) obj;
		return Objects.equals(accessRequirementId, other.accessRequirementId)
				&& Objects.equals(bindingType, other.bindingType) && Objects.equals(subjectId, other.subjectId)
				&& Objects.equals(subjectType, other.subjectType);
	}

	@Override
	public String toString() {
		return "DBOSubjectAccessRequirement [subjectId=" + subjectId + ", subjectType=" + subjectType
				+ ", accessRequirementId=" + accessRequirementId + ", bindingType=" + bindingType + "]";
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.NODE_ACCESS_REQUIRMENT;
	}

	@Override
	public MigratableTableTranslation<DBOSubjectAccessRequirement, DBOSubjectAccessRequirement> getTranslator() { return MIGRATION_MAPPER; }

	@Override
	public Class<? extends DBOSubjectAccessRequirement> getBackupClass() {
		return DBOSubjectAccessRequirement.class;
	}

	@Override
	public Class<? extends DBOSubjectAccessRequirement> getDatabaseObjectClass() {
		return DBOSubjectAccessRequirement.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
}
