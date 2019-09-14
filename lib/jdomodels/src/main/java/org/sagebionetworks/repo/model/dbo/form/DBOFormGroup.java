package org.sagebionetworks.repo.model.dbo.form;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_GROUP_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_GROUP_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_GROUP_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_FORM_GROUP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FORM_GROUP;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOFormGroup implements MigratableDatabaseObject<DBOFormGroup, DBOFormGroup> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("groupId", COL_FORM_GROUP_ID, true).withIsBackupId(true),
			new FieldColumn("name", COL_FORM_GROUP_NAME),
			new FieldColumn("createdBy", COL_FORM_GROUP_CREATED_BY),
			new FieldColumn("createdOn", COL_FORM_GROUP_CREATED_ON), };

	Long groupId;
	String name;
	Long createdBy;
	Timestamp createdOn;



	@Override
	public TableMapping<DBOFormGroup> getTableMapping() {
		return new TableMapping<DBOFormGroup>() {

			@Override
			public DBOFormGroup mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOFormGroup dbo = new DBOFormGroup();
				dbo.setGroupId(rs.getLong(COL_FORM_GROUP_ID));
				dbo.setName(rs.getString(COL_FORM_GROUP_NAME));
				dbo.setCreatedOn(rs.getTimestamp(COL_FORM_GROUP_CREATED_ON));
				dbo.setCreatedBy(rs.getLong(COL_FORM_GROUP_CREATED_BY));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_FORM_GROUP;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_FORM_GROUP;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOFormGroup> getDBOClass() {
				return DBOFormGroup.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.FORM_GROUP;
	}

	@Override
	public MigratableTableTranslation<DBOFormGroup, DBOFormGroup> getTranslator() {
		return new BasicMigratableTableTranslation<DBOFormGroup>();
	}

	@Override
	public Class<? extends DBOFormGroup> getBackupClass() {
		return DBOFormGroup.class;
	}

	@Override
	public Class<? extends DBOFormGroup> getDatabaseObjectClass() {
		return DBOFormGroup.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}
	
	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		DBOFormGroup other = (DBOFormGroup) obj;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (groupId == null) {
			if (other.groupId != null)
				return false;
		} else if (!groupId.equals(other.groupId))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOFormGroup [groupId=" + groupId + ", name=" + name + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + "]";
	}

}
