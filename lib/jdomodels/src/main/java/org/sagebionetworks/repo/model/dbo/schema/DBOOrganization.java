package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_ORGANIZATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ORGANIZATION;

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

public class DBOOrganization implements MigratableDatabaseObject<DBOOrganization, DBOOrganization> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_ORGANIZATION_ID, true).withIsBackupId(true),
			new FieldColumn("name", COL_ORGANIZATION_NAME),
			new FieldColumn("createdBy", COL_ORGANIZATION_CREATED_BY),
			new FieldColumn("createdOn", COL_ORGANIZATION_CREATED_ON), };

	Long id;
	String name;
	Long createdBy;
	Timestamp createdOn;

	@Override
	public TableMapping<DBOOrganization> getTableMapping() {
		return new TableMapping<DBOOrganization>() {

			@Override
			public DBOOrganization mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOOrganization dbo = new DBOOrganization();
				dbo.setId(rs.getLong(COL_ORGANIZATION_ID));
				dbo.setName(rs.getString(COL_ORGANIZATION_NAME));
				dbo.setCreatedBy(rs.getLong(COL_ORGANIZATION_CREATED_BY));
				dbo.setCreatedOn(rs.getTimestamp(COL_ORGANIZATION_CREATED_ON));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_ORGANIZATION;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_ORGANIZATION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOOrganization> getDBOClass() {
				return DBOOrganization.class;
			}
		};
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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
	public MigrationType getMigratableTableType() {
		return MigrationType.ORGANIZATION;
	}

	@Override
	public MigratableTableTranslation<DBOOrganization, DBOOrganization> getTranslator() {
		return new BasicMigratableTableTranslation<DBOOrganization>();
	}

	@Override
	public Class<? extends DBOOrganization> getBackupClass() {
		return DBOOrganization.class;
	}

	@Override
	public Class<? extends DBOOrganization> getDatabaseObjectClass() {
		return DBOOrganization.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		DBOOrganization other = (DBOOrganization) obj;
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
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
		return "DBOOrganization [id=" + id + ", name=" + name + ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ "]";
	}

}
