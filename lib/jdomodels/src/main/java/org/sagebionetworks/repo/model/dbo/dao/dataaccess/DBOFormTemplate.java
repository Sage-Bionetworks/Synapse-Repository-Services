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
 * The mutable owner row of a form template. The body of each version lives in
 * {@link DBOFormTemplateRevision}; this row tracks only the identity, the name
 * and which revision is current.
 */
public class DBOFormTemplate implements MigratableDatabaseObject<DBOFormTemplate, DBOFormTemplate> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", SqlConstants.COL_FORM_TEMPLATE_ID, true).withIsBackupId(true),
			new FieldColumn("etag", SqlConstants.COL_FORM_TEMPLATE_ETAG).withIsEtag(true),
			new FieldColumn("name", SqlConstants.COL_FORM_TEMPLATE_NAME),
			new FieldColumn("currentRevisionNumber", SqlConstants.COL_FORM_TEMPLATE_CURRENT_REV_NUM),
			new FieldColumn("createdBy", SqlConstants.COL_FORM_TEMPLATE_CREATED_BY),
			new FieldColumn("createdOn", SqlConstants.COL_FORM_TEMPLATE_CREATED_ON)
	};

	private static final TableMapping<DBOFormTemplate> TABLE_MAPPING = new TableMapping<DBOFormTemplate>() {

		@Override
		public DBOFormTemplate mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOFormTemplate dbo = new DBOFormTemplate();
			dbo.setId(rs.getLong(SqlConstants.COL_FORM_TEMPLATE_ID));
			dbo.setEtag(rs.getString(SqlConstants.COL_FORM_TEMPLATE_ETAG));
			dbo.setName(rs.getString(SqlConstants.COL_FORM_TEMPLATE_NAME));
			dbo.setCurrentRevisionNumber(rs.getLong(SqlConstants.COL_FORM_TEMPLATE_CURRENT_REV_NUM));
			dbo.setCreatedBy(rs.getLong(SqlConstants.COL_FORM_TEMPLATE_CREATED_BY));
			dbo.setCreatedOn(rs.getLong(SqlConstants.COL_FORM_TEMPLATE_CREATED_ON));
			return dbo;
		}

		@Override
		public String getTableName() {
			return SqlConstants.TABLE_FORM_TEMPLATE;
		}

		@Override
		public String getDDLFileName() {
			return SqlConstants.DDL_FORM_TEMPLATE;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOFormTemplate> getDBOClass() {
			return DBOFormTemplate.class;
		}
	};

	private static final MigratableTableTranslation<DBOFormTemplate, DBOFormTemplate> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();

	private Long id;
	private String etag;
	private String name;
	private Long currentRevisionNumber;
	private Long createdBy;
	private Long createdOn;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getCurrentRevisionNumber() {
		return currentRevisionNumber;
	}

	public void setCurrentRevisionNumber(Long currentRevisionNumber) {
		this.currentRevisionNumber = currentRevisionNumber;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public TableMapping<DBOFormTemplate> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.FORM_TEMPLATE;
	}

	@Override
	public MigratableTableTranslation<DBOFormTemplate, DBOFormTemplate> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOFormTemplate> getBackupClass() {
		return DBOFormTemplate.class;
	}

	@Override
	public Class<? extends DBOFormTemplate> getDatabaseObjectClass() {
		return DBOFormTemplate.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return List.of(new DBOFormTemplateRevision());
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, etag, name, currentRevisionNumber, createdBy, createdOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		DBOFormTemplate other = (DBOFormTemplate) obj;
		return Objects.equals(id, other.id)
				&& Objects.equals(etag, other.etag)
				&& Objects.equals(name, other.name)
				&& Objects.equals(currentRevisionNumber, other.currentRevisionNumber)
				&& Objects.equals(createdBy, other.createdBy)
				&& Objects.equals(createdOn, other.createdOn);
	}

	@Override
	public String toString() {
		return "DBOFormTemplate [id=" + id + ", etag=" + etag + ", name=" + name + ", currentRevisionNumber="
				+ currentRevisionNumber + ", createdBy=" + createdBy + ", createdOn=" + createdOn + "]";
	}
}
