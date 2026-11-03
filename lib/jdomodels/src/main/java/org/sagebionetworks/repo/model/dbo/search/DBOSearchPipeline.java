package org.sagebionetworks.repo.model.dbo.search;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_ORGANIZATION_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_SETTINGS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_SEARCH_PIPELINE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SEARCH_PIPELINE;

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

public class DBOSearchPipeline implements MigratableDatabaseObject<DBOSearchPipeline, DBOSearchPipeline> {

	private Long id;
	private String etag;
	private String organizationName;
	private String name;
	private String description;
	private String settings;
	private Long createdBy;
	private Timestamp createdOn;
	private Long modifiedBy;
	private Timestamp modifiedOn;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_SEARCH_PIPELINE_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_SEARCH_PIPELINE_ETAG).withIsEtag(true),
		new FieldColumn("organizationName", COL_SEARCH_PIPELINE_ORGANIZATION_NAME),
		new FieldColumn("name", COL_SEARCH_PIPELINE_NAME),
		new FieldColumn("description", COL_SEARCH_PIPELINE_DESCRIPTION),
		new FieldColumn("settings", COL_SEARCH_PIPELINE_SETTINGS),
		new FieldColumn("createdBy", COL_SEARCH_PIPELINE_CREATED_BY),
		new FieldColumn("createdOn", COL_SEARCH_PIPELINE_CREATED_ON),
		new FieldColumn("modifiedBy", COL_SEARCH_PIPELINE_MODIFIED_BY),
		new FieldColumn("modifiedOn", COL_SEARCH_PIPELINE_MODIFIED_ON)
	};

	private static final TableMapping<DBOSearchPipeline> TABLE_MAPPING = new TableMapping<>() {
		@Override
		public DBOSearchPipeline mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOSearchPipeline dbo = new DBOSearchPipeline();
			dbo.setId(rs.getLong(COL_SEARCH_PIPELINE_ID));
			dbo.setEtag(rs.getString(COL_SEARCH_PIPELINE_ETAG));
			dbo.setOrganizationName(rs.getString(COL_SEARCH_PIPELINE_ORGANIZATION_NAME));
			dbo.setName(rs.getString(COL_SEARCH_PIPELINE_NAME));
			dbo.setDescription(rs.getString(COL_SEARCH_PIPELINE_DESCRIPTION));
			dbo.setSettings(rs.getString(COL_SEARCH_PIPELINE_SETTINGS));
			dbo.setCreatedBy(rs.getLong(COL_SEARCH_PIPELINE_CREATED_BY));
			dbo.setCreatedOn(rs.getTimestamp(COL_SEARCH_PIPELINE_CREATED_ON));
			dbo.setModifiedBy(rs.getLong(COL_SEARCH_PIPELINE_MODIFIED_BY));
			dbo.setModifiedOn(rs.getTimestamp(COL_SEARCH_PIPELINE_MODIFIED_ON));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_SEARCH_PIPELINE;
		}

		@Override
		public String getDDLFileName() {
			return DDL_SEARCH_PIPELINE;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOSearchPipeline> getDBOClass() {
			return DBOSearchPipeline.class;
		}
	};

	private static final MigratableTableTranslation<DBOSearchPipeline, DBOSearchPipeline> MIGRATION_TRANSLATOR =
			new BasicMigratableTableTranslation<>();

	@Override
	public TableMapping<DBOSearchPipeline> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.SEARCH_PIPELINE;
	}

	@Override
	public MigratableTableTranslation<DBOSearchPipeline, DBOSearchPipeline> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOSearchPipeline> getBackupClass() {
		return DBOSearchPipeline.class;
	}

	@Override
	public Class<? extends DBOSearchPipeline> getDatabaseObjectClass() {
		return DBOSearchPipeline.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public DBOSearchPipeline setId(Long id) {
		this.id = id;
		return this;
	}

	public String getEtag() {
		return etag;
	}

	public DBOSearchPipeline setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public DBOSearchPipeline setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
		return this;
	}

	public String getName() {
		return name;
	}

	public DBOSearchPipeline setName(String name) {
		this.name = name;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public DBOSearchPipeline setDescription(String description) {
		this.description = description;
		return this;
	}

	public String getSettings() {
		return settings;
	}

	public DBOSearchPipeline setSettings(String settings) {
		this.settings = settings;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public DBOSearchPipeline setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public DBOSearchPipeline setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public DBOSearchPipeline setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public Timestamp getModifiedOn() {
		return modifiedOn;
	}

	public DBOSearchPipeline setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, createdOn, description, etag, id, modifiedBy, modifiedOn, name, organizationName, settings);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOSearchPipeline)) {
			return false;
		}
		DBOSearchPipeline other = (DBOSearchPipeline) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(description, other.description) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Objects.equals(modifiedBy, other.modifiedBy)
				&& Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(name, other.name)
				&& Objects.equals(organizationName, other.organizationName) && Objects.equals(settings, other.settings);
	}

	@Override
	public String toString() {
		return "DBOSearchPipeline [id=" + id + ", etag=" + etag + ", organizationName=" + organizationName + ", name=" + name
				+ ", description=" + description + ", settings=" + settings + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + "]";
	}

}
