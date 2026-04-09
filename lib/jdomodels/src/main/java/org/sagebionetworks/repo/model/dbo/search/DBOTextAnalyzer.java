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

public class DBOTextAnalyzer implements MigratableDatabaseObject<DBOTextAnalyzer, DBOTextAnalyzer> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", "ID", true).withIsBackupId(true),
			new FieldColumn("etag", "ETAG").withIsEtag(true),
			new FieldColumn("name", "NAME"),
			new FieldColumn("description", "DESCRIPTION"),
			new FieldColumn("organizationName", "ORGANIZATION_NAME"),
			new FieldColumn("settings", "SETTINGS"),
			new FieldColumn("createdBy", "CREATED_BY"),
			new FieldColumn("createdOn", "CREATED_ON"),
			new FieldColumn("modifiedBy", "MODIFIED_BY"),
			new FieldColumn("modifiedOn", "MODIFIED_ON"),
	};

	private Long id;
	private String etag;
	private String name;
	private String description;
	private String organizationName;
	private String settings;
	private Long createdBy;
	private Timestamp createdOn;
	private Long modifiedBy;
	private Timestamp modifiedOn;

	private static final TableMapping<DBOTextAnalyzer> TABLE_MAPPING = new TableMapping<>() {
		@Override
		public DBOTextAnalyzer mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOTextAnalyzer dbo = new DBOTextAnalyzer();
			dbo.setId(rs.getLong("ID"));
			dbo.setEtag(rs.getString("ETAG"));
			dbo.setName(rs.getString("NAME"));
			dbo.setDescription(rs.getString("DESCRIPTION"));
			dbo.setOrganizationName(rs.getString("ORGANIZATION_NAME"));
			dbo.setSettings(rs.getString("SETTINGS"));
			dbo.setCreatedBy(rs.getLong("CREATED_BY"));
			dbo.setCreatedOn(rs.getTimestamp("CREATED_ON"));
			dbo.setModifiedBy(rs.getLong("MODIFIED_BY"));
			dbo.setModifiedOn(rs.getTimestamp("MODIFIED_ON"));
			return dbo;
		}

		@Override
		public String getTableName() {
			return "TEXT_ANALYZER";
		}

		@Override
		public String getDDLFileName() {
			return "schema/TextAnalyzer-ddl.sql";
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOTextAnalyzer> getDBOClass() {
			return DBOTextAnalyzer.class;
		}
	};

	@Override
	public TableMapping<DBOTextAnalyzer> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.TEXT_ANALYZER;
	}

	private static final BasicMigratableTableTranslation<DBOTextAnalyzer> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();

	@Override
	public MigratableTableTranslation<DBOTextAnalyzer, DBOTextAnalyzer> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOTextAnalyzer> getBackupClass() {
		return DBOTextAnalyzer.class;
	}

	@Override
	public Class<? extends DBOTextAnalyzer> getDatabaseObjectClass() {
		return DBOTextAnalyzer.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public DBOTextAnalyzer setId(Long id) {
		this.id = id;
		return this;
	}

	public String getEtag() {
		return etag;
	}

	public DBOTextAnalyzer setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public String getName() {
		return name;
	}

	public DBOTextAnalyzer setName(String name) {
		this.name = name;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public DBOTextAnalyzer setDescription(String description) {
		this.description = description;
		return this;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public DBOTextAnalyzer setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
		return this;
	}

	public String getSettings() {
		return settings;
	}

	public DBOTextAnalyzer setSettings(String settings) {
		this.settings = settings;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public DBOTextAnalyzer setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public DBOTextAnalyzer setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public DBOTextAnalyzer setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public Timestamp getModifiedOn() {
		return modifiedOn;
	}

	public DBOTextAnalyzer setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, etag, name, description, organizationName, settings, createdBy, createdOn, modifiedBy, modifiedOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOTextAnalyzer)) {
			return false;
		}
		DBOTextAnalyzer other = (DBOTextAnalyzer) obj;
		return Objects.equals(id, other.id)
				&& Objects.equals(etag, other.etag)
				&& Objects.equals(name, other.name)
				&& Objects.equals(description, other.description)
				&& Objects.equals(organizationName, other.organizationName)
				&& Objects.equals(settings, other.settings)
				&& Objects.equals(createdBy, other.createdBy)
				&& Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(modifiedBy, other.modifiedBy)
				&& Objects.equals(modifiedOn, other.modifiedOn);
	}

	@Override
	public String toString() {
		return "DBOTextAnalyzer [id=" + id + ", etag=" + etag + ", name=" + name + ", description=" + description
				+ ", organizationName=" + organizationName + ", settings=" + settings
				+ ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + "]";
	}
}
