package org.sagebionetworks.repo.model.dbo.feature;

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
 * Used to conditionally enabled/disable features for testing
 * 
 * @author Marco Marasca
 */
public class DBOFeatureStatus implements MigratableDatabaseObject<DBOFeatureStatus, DBOFeatureStatus> {

	private static final FieldColumn FIELDS[] = new FieldColumn[] {
			new FieldColumn("id", SqlConstants.COL_FEATURE_STATUS_ID, true).withIsBackupId(true),
			new FieldColumn("etag", SqlConstants.COL_FEATURE_STATUS_ETAG).withIsEtag(true),
			new FieldColumn("featureType", SqlConstants.COL_FEATURE_STATUS_TYPE),
			new FieldColumn("enabled", SqlConstants.COL_FEATURE_STATUS_ENABLED) };

	private static final TableMapping<DBOFeatureStatus> TABLE_MAPPING = new TableMapping<DBOFeatureStatus>() {

		@Override
		public DBOFeatureStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOFeatureStatus dbo = new DBOFeatureStatus();

			dbo.setId(rs.getLong(SqlConstants.COL_FEATURE_STATUS_ID));
			dbo.setEtag(rs.getString(SqlConstants.COL_FEATURE_STATUS_ETAG));
			dbo.setFeatureType(rs.getString(SqlConstants.COL_FEATURE_STATUS_TYPE));
			dbo.setEnabled(rs.getBoolean(SqlConstants.COL_FEATURE_STATUS_ENABLED));

			return dbo;
		}

		@Override
		public String getTableName() {
			return SqlConstants.TABLE_FEATURE_STATUS;
		}

		@Override
		public String getDDLFileName() {
			return SqlConstants.DDL_FEATURE_STATUS;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOFeatureStatus> getDBOClass() {
			return DBOFeatureStatus.class;
		}

	};

	private static final MigratableTableTranslation<DBOFeatureStatus, DBOFeatureStatus> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();

	private Long id;
	private String etag;
	private String featureType;
	private Boolean enabled;

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

	public String getFeatureType() {
		return featureType;
	}

	public void setFeatureType(String featureType) {
		this.featureType = featureType;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public TableMapping<DBOFeatureStatus> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.FEATURE_STATUS;
	}

	@Override
	public MigratableTableTranslation<DBOFeatureStatus, DBOFeatureStatus> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOFeatureStatus> getBackupClass() {
		return DBOFeatureStatus.class;
	}

	@Override
	public Class<? extends DBOFeatureStatus> getDatabaseObjectClass() {
		return DBOFeatureStatus.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(enabled, etag, featureType, id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DBOFeatureStatus other = (DBOFeatureStatus) obj;
		return Objects.equals(enabled, other.enabled) && Objects.equals(etag, other.etag)
				&& Objects.equals(featureType, other.featureType) && Objects.equals(id, other.id);
	}

	@Override
	public String toString() {
		return "DBOFeatureStatus [id=" + id + ", etag=" + etag + ", featureType=" + featureType + ", enabled=" + enabled
				+ "]";
	}

}
