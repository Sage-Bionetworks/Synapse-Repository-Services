package org.sagebionetworks.repo.model.dbo.search;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_COL_ANALYZER_OVERRIDES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_COL_SEMANTIC_ENRICHMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_DEFAULT_ANALYZER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_ORGANIZATION_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_SEARCH_CONFIGURATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SEARCH_CONFIGURATION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.util.TemporaryCode;

public class DBOSearchConfiguration implements MigratableDatabaseObject<DBOSearchConfiguration, DBOSearchConfiguration> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_SEARCH_CONFIG_ID, true).withIsBackupId(true),
			new FieldColumn("etag", COL_SEARCH_CONFIG_ETAG).withIsEtag(true),
			new FieldColumn("organizationName", COL_SEARCH_CONFIG_ORGANIZATION_NAME),
			new FieldColumn("name", COL_SEARCH_CONFIG_NAME),
			new FieldColumn("description", COL_SEARCH_CONFIG_DESCRIPTION),
			new FieldColumn("defaultAnalyzer", COL_SEARCH_CONFIG_DEFAULT_ANALYZER),
			new FieldColumn("columnAnalyzerOverridesJson", COL_SEARCH_CONFIG_COL_ANALYZER_OVERRIDES),
			new FieldColumn("columnSemanticEnrichmentJson", COL_SEARCH_CONFIG_COL_SEMANTIC_ENRICHMENT),
			new FieldColumn("createdBy", COL_SEARCH_CONFIG_CREATED_BY),
			new FieldColumn("createdOn", COL_SEARCH_CONFIG_CREATED_ON),
			new FieldColumn("modifiedBy", COL_SEARCH_CONFIG_MODIFIED_BY),
			new FieldColumn("modifiedOn", COL_SEARCH_CONFIG_MODIFIED_ON),
	};

	private Long id;
	private String etag;
	private String organizationName;
	private String name;
	private String description;
	private String defaultAnalyzer;
	private String columnAnalyzerOverridesJson;
	private String columnSemanticEnrichmentJson;
	private Long createdBy;
	private Timestamp createdOn;
	private Long modifiedBy;
	private Timestamp modifiedOn;

	// Legacy backups still serialize the <synonymSetsJson> XML element (the dropped JSON
	// column). Caught here so deserialization does not fail; the translator below
	// discards it. No FieldColumn entry — never read from or written to the database.
	@TemporaryCode(author = "BryanFauble", comment = "PLFM-9676: Can be removed after one migration cycle.")
	private String synonymSetsJson;

	private static final TableMapping<DBOSearchConfiguration> TABLE_MAPPING = new TableMapping<>() {
		@Override
		public DBOSearchConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOSearchConfiguration dbo = new DBOSearchConfiguration();
			dbo.setId(rs.getLong(COL_SEARCH_CONFIG_ID));
			dbo.setEtag(rs.getString(COL_SEARCH_CONFIG_ETAG));
			dbo.setOrganizationName(rs.getString(COL_SEARCH_CONFIG_ORGANIZATION_NAME));
			dbo.setName(rs.getString(COL_SEARCH_CONFIG_NAME));
			dbo.setDescription(rs.getString(COL_SEARCH_CONFIG_DESCRIPTION));
			dbo.setDefaultAnalyzer(rs.getString(COL_SEARCH_CONFIG_DEFAULT_ANALYZER));
			dbo.setColumnAnalyzerOverridesJson(rs.getString(COL_SEARCH_CONFIG_COL_ANALYZER_OVERRIDES));
			dbo.setColumnSemanticEnrichmentJson(rs.getString(COL_SEARCH_CONFIG_COL_SEMANTIC_ENRICHMENT));
			dbo.setCreatedBy(rs.getLong(COL_SEARCH_CONFIG_CREATED_BY));
			dbo.setCreatedOn(rs.getTimestamp(COL_SEARCH_CONFIG_CREATED_ON));
			dbo.setModifiedBy(rs.getLong(COL_SEARCH_CONFIG_MODIFIED_BY));
			dbo.setModifiedOn(rs.getTimestamp(COL_SEARCH_CONFIG_MODIFIED_ON));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_SEARCH_CONFIGURATION;
		}

		@Override
		public String getDDLFileName() {
			return DDL_SEARCH_CONFIGURATION;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOSearchConfiguration> getDBOClass() {
			return DBOSearchConfiguration.class;
		}
	};

	@Override
	public TableMapping<DBOSearchConfiguration> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.SEARCH_CONFIGURATION;
	}

	// Production backups still serialize the legacy <synonymSetsJson> element from the
	// previous interior shape; the translator nulls it on restore. Both
	// columnAnalyzerOverrides and defaultAnalyzer changed shape in this PR — overrides moved
	// from List<String> qnames to a heterogeneous List<Object> of $ref-or-inline, and
	// defaultAnalyzer moved from a bare qname string to a $ref-or-inline object. The
	// safest restore path is to discard the legacy values; curators recreate via REST.
	@TemporaryCode(author = "BryanFauble", comment = "PLFM-9676: Replace with BasicMigratableTableTranslation after the next stack flushes legacy backup shapes.")
	private static final MigratableTableTranslation<DBOSearchConfiguration, DBOSearchConfiguration> MIGRATION_TRANSLATOR =
			new MigratableTableTranslation<DBOSearchConfiguration, DBOSearchConfiguration>() {
		@Override
		public DBOSearchConfiguration createDatabaseObjectFromBackup(DBOSearchConfiguration backup) {
			backup.setColumnAnalyzerOverridesJson(null);
			backup.setSynonymSetsJson(null);
			// Legacy defaultAnalyzer column held a bare qname; the new JSON column requires a
			// JSON value. Either upgrade the bare qname to a $ref shape or drop it. Drop here:
			// the schema reshape is a clean break, and curators are expected to re-save.
			backup.setDefaultAnalyzer(null);
			return backup;
		}

		@Override
		public DBOSearchConfiguration createBackupFromDatabaseObject(DBOSearchConfiguration dbo) {
			return dbo;
		}
	};

	@Override
	public MigratableTableTranslation<DBOSearchConfiguration, DBOSearchConfiguration> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOSearchConfiguration> getBackupClass() {
		return DBOSearchConfiguration.class;
	}

	@Override
	public Class<? extends DBOSearchConfiguration> getDatabaseObjectClass() {
		return DBOSearchConfiguration.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return Collections.emptyList();
	}

	public Long getId() {
		return id;
	}

	public DBOSearchConfiguration setId(Long id) {
		this.id = id;
		return this;
	}

	public String getEtag() {
		return etag;
	}

	public DBOSearchConfiguration setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public DBOSearchConfiguration setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
		return this;
	}

	public String getName() {
		return name;
	}

	public DBOSearchConfiguration setName(String name) {
		this.name = name;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public DBOSearchConfiguration setDescription(String description) {
		this.description = description;
		return this;
	}

	public String getDefaultAnalyzer() {
		return defaultAnalyzer;
	}

	public DBOSearchConfiguration setDefaultAnalyzer(String defaultAnalyzer) {
		this.defaultAnalyzer = defaultAnalyzer;
		return this;
	}

	@TemporaryCode(author = "BryanFauble", comment = "PLFM-9676: Can be removed after one migration cycle.")
	public String getSynonymSetsJson() {
		return synonymSetsJson;
	}

	@TemporaryCode(author = "BryanFauble", comment = "PLFM-9676: Can be removed after one migration cycle.")
	public DBOSearchConfiguration setSynonymSetsJson(String synonymSetsJson) {
		this.synonymSetsJson = synonymSetsJson;
		return this;
	}

	public String getColumnAnalyzerOverridesJson() {
		return columnAnalyzerOverridesJson;
	}

	public DBOSearchConfiguration setColumnAnalyzerOverridesJson(String columnAnalyzerOverridesJson) {
		this.columnAnalyzerOverridesJson = columnAnalyzerOverridesJson;
		return this;
	}

	public String getColumnSemanticEnrichmentJson() {
		return columnSemanticEnrichmentJson;
	}

	public DBOSearchConfiguration setColumnSemanticEnrichmentJson(String columnSemanticEnrichmentJson) {
		this.columnSemanticEnrichmentJson = columnSemanticEnrichmentJson;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public DBOSearchConfiguration setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public DBOSearchConfiguration setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public DBOSearchConfiguration setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public Timestamp getModifiedOn() {
		return modifiedOn;
	}

	public DBOSearchConfiguration setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, etag, organizationName, name, description,
				defaultAnalyzer, columnAnalyzerOverridesJson, columnSemanticEnrichmentJson,
				createdBy, createdOn, modifiedBy, modifiedOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOSearchConfiguration)) {
			return false;
		}
		DBOSearchConfiguration other = (DBOSearchConfiguration) obj;
		return Objects.equals(id, other.id)
				&& Objects.equals(etag, other.etag)
				&& Objects.equals(organizationName, other.organizationName)
				&& Objects.equals(name, other.name)
				&& Objects.equals(description, other.description)
				&& Objects.equals(defaultAnalyzer, other.defaultAnalyzer)
				&& Objects.equals(columnAnalyzerOverridesJson, other.columnAnalyzerOverridesJson)
				&& Objects.equals(columnSemanticEnrichmentJson, other.columnSemanticEnrichmentJson)
				&& Objects.equals(createdBy, other.createdBy)
				&& Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(modifiedBy, other.modifiedBy)
				&& Objects.equals(modifiedOn, other.modifiedOn);
	}

	@Override
	public String toString() {
		return "DBOSearchConfiguration [id=" + id + ", etag=" + etag + ", organizationName=" + organizationName
				+ ", name=" + name + ", description=" + description
				+ ", defaultAnalyzer=" + defaultAnalyzer
				+ ", columnAnalyzerOverridesJson=" + columnAnalyzerOverridesJson
				+ ", columnSemanticEnrichmentJson=" + columnSemanticEnrichmentJson
				+ ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + "]";
	}
}
