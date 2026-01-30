package org.sagebionetworks.repo.model.dbo.schema;

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

public class DBORecordSetValidationStats implements MigratableDatabaseObject<DBORecordSetValidationStats, DBORecordSetValidationStats> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", SqlConstants.COL_RECORDSET_VALIDATION_STATS_ID, true).withIsBackupId(true),
		new FieldColumn("etag", SqlConstants.COL_RECORDSET_VALIDATION_STATS_ETAG).withIsEtag(true),
		new FieldColumn("recordSetId", SqlConstants.COL_RECORDSET_VALIDATION_STATS_RECORDSET_ID),
		new FieldColumn("recordSetVersion", SqlConstants.COL_RECORDSET_VALIDATION_STATS_RECORDSET_VERSION),
		new FieldColumn("validationStatsJson", SqlConstants.COL_RECORDSET_VALIDATION_STATS_JSON)
	};
	
	private Long id;
	private String etag;
	private Long recordSetId;
	private Long recordSetVersion;
	private String validationStatsJson;
	
	public Long getId() {
		return id;
	}

	public DBORecordSetValidationStats setId(Long id) {
		this.id = id;
		return this;
	}
	
	public String getEtag() {
		return etag;
	}
	
	public DBORecordSetValidationStats setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public Long getRecordSetId() {
		return recordSetId;
	}

	public DBORecordSetValidationStats setRecordSetId(Long recordSetId) {
		this.recordSetId = recordSetId;
		return this;
	}

	public Long getRecordSetVersion() {
		return recordSetVersion;
	}

	public DBORecordSetValidationStats setRecordSetVersion(Long recordSetVersion) {
		this.recordSetVersion = recordSetVersion;
		return this;
	}

	public String getValidationStatsJson() {
		return validationStatsJson;
	}

	public DBORecordSetValidationStats setValidationStatsJson(String validationStatsJson) {
		this.validationStatsJson = validationStatsJson;
		return this;
	}

	@Override
	public TableMapping<DBORecordSetValidationStats> getTableMapping() {
		return new TableMapping<DBORecordSetValidationStats>() {
			
			@Override
			public DBORecordSetValidationStats mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new DBORecordSetValidationStats()
					.setId(rs.getLong(SqlConstants.COL_RECORDSET_VALIDATION_STATS_ID))
					.setEtag(rs.getString(SqlConstants.COL_RECORDSET_VALIDATION_STATS_ETAG))
					.setRecordSetId(rs.getLong(SqlConstants.COL_RECORDSET_VALIDATION_STATS_RECORDSET_ID))
					.setRecordSetVersion(rs.getLong(SqlConstants.COL_RECORDSET_VALIDATION_STATS_RECORDSET_VERSION))
					.setValidationStatsJson(rs.getString(SqlConstants.COL_RECORDSET_VALIDATION_STATS_JSON));
			}
			
			@Override
			public String getTableName() {
				return SqlConstants.TABLE_RECORDSET_VALIDATION_STATS;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_FILE_RECORDSET_VALIDATION_STATS;
			}
			
			@Override
			public Class<? extends DBORecordSetValidationStats> getDBOClass() {
				return DBORecordSetValidationStats.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.RECORDSET_VALIDATION_STATS;
	}

	@Override
	public MigratableTableTranslation<DBORecordSetValidationStats, DBORecordSetValidationStats> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBORecordSetValidationStats> getBackupClass() {
		return DBORecordSetValidationStats.class;
	}

	@Override
	public Class<? extends DBORecordSetValidationStats> getDatabaseObjectClass() {
		return DBORecordSetValidationStats.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(etag, id, recordSetId, recordSetVersion, validationStatsJson);
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
		DBORecordSetValidationStats other = (DBORecordSetValidationStats) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(id, other.id)
			&& Objects.equals(recordSetId, other.recordSetId) && Objects.equals(recordSetVersion, other.recordSetVersion)
			&& Objects.equals(validationStatsJson, other.validationStatsJson);
	}

	@Override
	public String toString() {
		return "DBORecordSetValidationStats [id=" + id + ", etag=" + etag + ", recordSetId=" + recordSetId + ", recordSetVersion=" + recordSetVersion + ", validationStatsJson="
			+ validationStatsJson + "]";
	}
	
}
