package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_QUOTA_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_QUOTA_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_QUOTA_QUOTA_IN_MB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_STORAGE_QUOTA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STORAGE_QUOTA;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOStorageQuota implements MigratableDatabaseObject<DBOStorageQuota, DBOStorageQuota> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("ownerId", COL_STORAGE_QUOTA_OWNER_ID, true).withIsBackupId(true),
		new FieldColumn("eTag", COL_STORAGE_QUOTA_ETAG).withIsEtag(true),
		new FieldColumn("quotaInMb", COL_STORAGE_QUOTA_QUOTA_IN_MB),
	};

	@Override
	public TableMapping<DBOStorageQuota> getTableMapping() {
		return new TableMapping<DBOStorageQuota>() {

			@Override
			public DBOStorageQuota mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOStorageQuota dbo = new DBOStorageQuota();
				dbo.setOwnerId(rs.getLong(COL_STORAGE_QUOTA_OWNER_ID));
				dbo.seteTag(rs.getString(COL_STORAGE_QUOTA_ETAG));
				dbo.setQuotaInMb(rs.getInt(COL_STORAGE_QUOTA_QUOTA_IN_MB));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_STORAGE_QUOTA;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_STORAGE_QUOTA;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOStorageQuota> getDBOClass() {
				return DBOStorageQuota.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.STORAGE_QUOTA;
	}

	@Override
	public MigratableTableTranslation<DBOStorageQuota, DBOStorageQuota> getTranslator() {
		return new MigratableTableTranslation<DBOStorageQuota, DBOStorageQuota>(){

			@Override
			public DBOStorageQuota createDatabaseObjectFromBackup(
					DBOStorageQuota backup) {
				return backup;
			}

			@Override
			public DBOStorageQuota createBackupFromDatabaseObject(
					DBOStorageQuota dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBOStorageQuota> getBackupClass() {
		return DBOStorageQuota.class;
	}

	@Override
	public Class<? extends DBOStorageQuota> getDatabaseObjectClass() {
		return DBOStorageQuota.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public String geteTag() {
		return eTag;
	}

	public void seteTag(String eTag) {
		this.eTag = eTag;
	}

	public Integer getQuotaInMb() {
		return quotaInMb;
	}

	public void setQuotaInMb(Integer quotaInMb) {
		this.quotaInMb = quotaInMb;
	}

	private Long ownerId;
	private String eTag;
	private Integer quotaInMb;
}
