package org.sagebionetworks.repo.model.dbo.search;

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

public class DBOColumnAnalyzerOverride implements MigratableDatabaseObject<DBOColumnAnalyzerOverride, DBOColumnAnalyzerOverride> {

	private Long id;
	private String etag;
	private String organizationName;
	private String name;
	private String description;
	private String overrides;
	private Long createdBy;
	private Timestamp createdOn;
	private Long modifiedBy;
	private Timestamp modifiedOn;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", "ID", true).withIsBackupId(true),
		new FieldColumn("etag", "ETAG").withIsEtag(true),
		new FieldColumn("organizationName", "ORGANIZATION_NAME"),
		new FieldColumn("name", "NAME"),
		new FieldColumn("description", "DESCRIPTION"),
		new FieldColumn("overrides", "OVERRIDES"),
		new FieldColumn("createdBy", "CREATED_BY"),
		new FieldColumn("createdOn", "CREATED_ON"),
		new FieldColumn("modifiedBy", "MODIFIED_BY"),
		new FieldColumn("modifiedOn", "MODIFIED_ON")
	};

	private static final TableMapping<DBOColumnAnalyzerOverride> TABLE_MAPPING = new TableMapping<>() {
		@Override
		public DBOColumnAnalyzerOverride mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOColumnAnalyzerOverride dbo = new DBOColumnAnalyzerOverride();
			dbo.setId(rs.getLong("ID"));
			dbo.setEtag(rs.getString("ETAG"));
			dbo.setOrganizationName(rs.getString("ORGANIZATION_NAME"));
			dbo.setName(rs.getString("NAME"));
			dbo.setDescription(rs.getString("DESCRIPTION"));
			dbo.setOverrides(rs.getString("OVERRIDES"));
			dbo.setCreatedBy(rs.getLong("CREATED_BY"));
			dbo.setCreatedOn(rs.getTimestamp("CREATED_ON"));
			dbo.setModifiedBy(rs.getLong("MODIFIED_BY"));
			dbo.setModifiedOn(rs.getTimestamp("MODIFIED_ON"));
			return dbo;
		}

		@Override
		public String getTableName() {
			return "COLUMN_ANALYZER_OVERRIDE";
		}

		@Override
		public String getDDLFileName() {
			return "schema/ColumnAnalyzerOverride-ddl.sql";
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOColumnAnalyzerOverride> getDBOClass() {
			return DBOColumnAnalyzerOverride.class;
		}
	};

	private static final BasicMigratableTableTranslation<DBOColumnAnalyzerOverride> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();

	@Override
	public TableMapping<DBOColumnAnalyzerOverride> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.COLUMN_ANALYZER_OVERRIDE;
	}

	@Override
	public MigratableTableTranslation<DBOColumnAnalyzerOverride, DBOColumnAnalyzerOverride> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOColumnAnalyzerOverride> getBackupClass() {
		return DBOColumnAnalyzerOverride.class;
	}

	@Override
	public Class<? extends DBOColumnAnalyzerOverride> getDatabaseObjectClass() {
		return DBOColumnAnalyzerOverride.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public DBOColumnAnalyzerOverride setId(Long id) {
		this.id = id;
		return this;
	}

	public String getEtag() {
		return etag;
	}

	public DBOColumnAnalyzerOverride setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public DBOColumnAnalyzerOverride setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
		return this;
	}

	public String getName() {
		return name;
	}

	public DBOColumnAnalyzerOverride setName(String name) {
		this.name = name;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public DBOColumnAnalyzerOverride setDescription(String description) {
		this.description = description;
		return this;
	}

	public String getOverrides() {
		return overrides;
	}

	public DBOColumnAnalyzerOverride setOverrides(String overrides) {
		this.overrides = overrides;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public DBOColumnAnalyzerOverride setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public DBOColumnAnalyzerOverride setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public DBOColumnAnalyzerOverride setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public Timestamp getModifiedOn() {
		return modifiedOn;
	}

	public DBOColumnAnalyzerOverride setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, createdOn, description, etag, id, modifiedBy, modifiedOn, name, organizationName, overrides);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOColumnAnalyzerOverride)) {
			return false;
		}
		DBOColumnAnalyzerOverride other = (DBOColumnAnalyzerOverride) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(description, other.description) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Objects.equals(modifiedBy, other.modifiedBy)
				&& Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(name, other.name)
				&& Objects.equals(organizationName, other.organizationName) && Objects.equals(overrides, other.overrides);
	}

	@Override
	public String toString() {
		return "DBOColumnAnalyzerOverride [id=" + id + ", etag=" + etag + ", organizationName=" + organizationName + ", name=" + name
				+ ", description=" + description + ", overrides=" + overrides + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + "]";
	}

}
