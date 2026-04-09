package org.sagebionetworks.repo.model.dbo.search;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SYNSET_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SYNSET_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SYNSET_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SYNSET_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SYNSET_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SYNSET_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SYNSET_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SYNSET_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SYNSET_ORGANIZATION_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SYNSET_RULES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_SYNONYM_SET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SYNONYM_SET;

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

public class DBOSynonymSet implements MigratableDatabaseObject<DBOSynonymSet, DBOSynonymSet> {

	private Long id;
	private String etag;
	private String organizationName;
	private String name;
	private String description;
	private String rules;
	private Long createdBy;
	private Timestamp createdOn;
	private Long modifiedBy;
	private Timestamp modifiedOn;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_SYNSET_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_SYNSET_ETAG).withIsEtag(true),
		new FieldColumn("organizationName", COL_SYNSET_ORGANIZATION_NAME),
		new FieldColumn("name", COL_SYNSET_NAME),
		new FieldColumn("description", COL_SYNSET_DESCRIPTION),
		new FieldColumn("rules", COL_SYNSET_RULES),
		new FieldColumn("createdBy", COL_SYNSET_CREATED_BY),
		new FieldColumn("createdOn", COL_SYNSET_CREATED_ON),
		new FieldColumn("modifiedBy", COL_SYNSET_MODIFIED_BY),
		new FieldColumn("modifiedOn", COL_SYNSET_MODIFIED_ON)
	};

	private static final TableMapping<DBOSynonymSet> TABLE_MAPPING = new TableMapping<>() {
		@Override
		public DBOSynonymSet mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOSynonymSet dbo = new DBOSynonymSet();
			dbo.setId(rs.getLong(COL_SYNSET_ID));
			dbo.setEtag(rs.getString(COL_SYNSET_ETAG));
			dbo.setOrganizationName(rs.getString(COL_SYNSET_ORGANIZATION_NAME));
			dbo.setName(rs.getString(COL_SYNSET_NAME));
			dbo.setDescription(rs.getString(COL_SYNSET_DESCRIPTION));
			dbo.setRules(rs.getString(COL_SYNSET_RULES));
			dbo.setCreatedBy(rs.getLong(COL_SYNSET_CREATED_BY));
			dbo.setCreatedOn(rs.getTimestamp(COL_SYNSET_CREATED_ON));
			dbo.setModifiedBy(rs.getLong(COL_SYNSET_MODIFIED_BY));
			dbo.setModifiedOn(rs.getTimestamp(COL_SYNSET_MODIFIED_ON));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_SYNONYM_SET;
		}

		@Override
		public String getDDLFileName() {
			return DDL_SYNONYM_SET;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOSynonymSet> getDBOClass() {
			return DBOSynonymSet.class;
		}
	};

	private static final BasicMigratableTableTranslation<DBOSynonymSet> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();

	@Override
	public TableMapping<DBOSynonymSet> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.SYNONYM_SET;
	}

	@Override
	public MigratableTableTranslation<DBOSynonymSet, DBOSynonymSet> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOSynonymSet> getBackupClass() {
		return DBOSynonymSet.class;
	}

	@Override
	public Class<? extends DBOSynonymSet> getDatabaseObjectClass() {
		return DBOSynonymSet.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public DBOSynonymSet setId(Long id) {
		this.id = id;
		return this;
	}

	public String getEtag() {
		return etag;
	}

	public DBOSynonymSet setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public DBOSynonymSet setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
		return this;
	}

	public String getName() {
		return name;
	}

	public DBOSynonymSet setName(String name) {
		this.name = name;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public DBOSynonymSet setDescription(String description) {
		this.description = description;
		return this;
	}

	public String getRules() {
		return rules;
	}

	public DBOSynonymSet setRules(String rules) {
		this.rules = rules;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public DBOSynonymSet setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public DBOSynonymSet setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public DBOSynonymSet setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public Timestamp getModifiedOn() {
		return modifiedOn;
	}

	public DBOSynonymSet setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, createdOn, description, etag, id, modifiedBy, modifiedOn, name, organizationName, rules);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOSynonymSet)) {
			return false;
		}
		DBOSynonymSet other = (DBOSynonymSet) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(description, other.description) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Objects.equals(modifiedBy, other.modifiedBy)
				&& Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(name, other.name)
				&& Objects.equals(organizationName, other.organizationName) && Objects.equals(rules, other.rules);
	}

	@Override
	public String toString() {
		return "DBOSynonymSet [id=" + id + ", etag=" + etag + ", organizationName=" + organizationName + ", name=" + name
				+ ", description=" + description + ", rules=" + rules + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + "]";
	}

}
