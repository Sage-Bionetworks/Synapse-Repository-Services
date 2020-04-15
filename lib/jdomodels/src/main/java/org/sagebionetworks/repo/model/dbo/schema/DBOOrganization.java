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
import java.util.Objects;

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

	private Long id;
	private String name;
	private Long createdBy;
	private Timestamp createdOn;

	public static final TableMapping<DBOOrganization> TABLE_MAPPING = new TableMapping<DBOOrganization>() {

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

	@Override
	public TableMapping<DBOOrganization> getTableMapping() {
		return TABLE_MAPPING;
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
	
	public static final MigratableTableTranslation<DBOOrganization, DBOOrganization> TRANSLATOR = new BasicMigratableTableTranslation<DBOOrganization>();

	@Override
	public MigratableTableTranslation<DBOOrganization, DBOOrganization> getTranslator() {
		return TRANSLATOR;
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
		return Objects.hash(createdBy, createdOn, id, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOOrganization)) {
			return false;
		}
		DBOOrganization other = (DBOOrganization) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(id, other.id) && Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return "DBOOrganization [id=" + id + ", name=" + name + ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ "]";
	}

}
