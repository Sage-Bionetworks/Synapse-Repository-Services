package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

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
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * An immutable version of a form template, owned by {@link DBOFormTemplate}.
 * The full template body is serialized to JSON; DEPRECATED is a column of its
 * own because searches filter on it.
 */
public class DBOFormTemplateRevision implements MigratableDatabaseObject<DBOFormTemplateRevision, DBOFormTemplateRevision> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("ownerId", SqlConstants.COL_FORM_TEMPLATE_REVISION_OWNER_ID, true).withIsBackupId(true),
			new FieldColumn("number", SqlConstants.COL_FORM_TEMPLATE_REVISION_NUMBER, true),
			new FieldColumn("modifiedBy", SqlConstants.COL_FORM_TEMPLATE_REVISION_MODIFIED_BY),
			new FieldColumn("modifiedOn", SqlConstants.COL_FORM_TEMPLATE_REVISION_MODIFIED_ON),
			new FieldColumn("deprecated", SqlConstants.COL_FORM_TEMPLATE_REVISION_DEPRECATED),
			new FieldColumn("templateJson", SqlConstants.COL_FORM_TEMPLATE_REVISION_TEMPLATE_JSON)
	};

	private static final TableMapping<DBOFormTemplateRevision> TABLE_MAPPING = new TableMapping<DBOFormTemplateRevision>() {

		@Override
		public DBOFormTemplateRevision mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOFormTemplateRevision dbo = new DBOFormTemplateRevision();
			dbo.setOwnerId(rs.getLong(SqlConstants.COL_FORM_TEMPLATE_REVISION_OWNER_ID));
			dbo.setNumber(rs.getLong(SqlConstants.COL_FORM_TEMPLATE_REVISION_NUMBER));
			dbo.setModifiedBy(rs.getLong(SqlConstants.COL_FORM_TEMPLATE_REVISION_MODIFIED_BY));
			dbo.setModifiedOn(rs.getLong(SqlConstants.COL_FORM_TEMPLATE_REVISION_MODIFIED_ON));
			dbo.setDeprecated(rs.getBoolean(SqlConstants.COL_FORM_TEMPLATE_REVISION_DEPRECATED));
			dbo.setTemplateJson(rs.getString(SqlConstants.COL_FORM_TEMPLATE_REVISION_TEMPLATE_JSON));
			return dbo;
		}

		@Override
		public String getTableName() {
			return SqlConstants.TABLE_FORM_TEMPLATE_REVISION;
		}

		@Override
		public String getDDLFileName() {
			return SqlConstants.DDL_FORM_TEMPLATE_REVISION;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOFormTemplateRevision> getDBOClass() {
			return DBOFormTemplateRevision.class;
		}
	};

	private static final MigratableTableTranslation<DBOFormTemplateRevision, DBOFormTemplateRevision> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();

	private Long ownerId;
	private Long number;
	private Long modifiedBy;
	private Long modifiedOn;
	private Boolean deprecated;
	private String templateJson;

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public Long getNumber() {
		return number;
	}

	public void setNumber(Long number) {
		this.number = number;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Long getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public Boolean getDeprecated() {
		return deprecated;
	}

	public void setDeprecated(Boolean deprecated) {
		this.deprecated = deprecated;
	}

	public String getTemplateJson() {
		return templateJson;
	}

	public void setTemplateJson(String templateJson) {
		this.templateJson = templateJson;
	}

	@Override
	public TableMapping<DBOFormTemplateRevision> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.FORM_TEMPLATE_REVISION;
	}

	@Override
	public MigratableTableTranslation<DBOFormTemplateRevision, DBOFormTemplateRevision> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOFormTemplateRevision> getBackupClass() {
		return DBOFormTemplateRevision.class;
	}

	@Override
	public Class<? extends DBOFormTemplateRevision> getDatabaseObjectClass() {
		return DBOFormTemplateRevision.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ownerId, number, modifiedBy, modifiedOn, deprecated, templateJson);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		DBOFormTemplateRevision other = (DBOFormTemplateRevision) obj;
		return Objects.equals(ownerId, other.ownerId)
				&& Objects.equals(number, other.number)
				&& Objects.equals(modifiedBy, other.modifiedBy)
				&& Objects.equals(modifiedOn, other.modifiedOn)
				&& Objects.equals(deprecated, other.deprecated)
				&& Objects.equals(templateJson, other.templateJson);
	}

	@Override
	public String toString() {
		return "DBOFormTemplateRevision [ownerId=" + ownerId + ", number=" + number + ", modifiedBy=" + modifiedBy
				+ ", modifiedOn=" + modifiedOn + ", deprecated=" + deprecated + ", templateJson=" + templateJson + "]";
	}
}
