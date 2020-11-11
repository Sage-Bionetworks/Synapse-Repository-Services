package org.sagebionetworks.repo.model.dbo.file.download;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_2_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_2_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_2_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DOWNLOAD_2_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_2;

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

import com.google.common.collect.Lists;

public class DBODownloadList2 implements MigratableDatabaseObject<DBODownloadList2, DBODownloadList2> {

	private Long principalId;
	private Long updatedOn;
	private String etag;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("principalId", COL_DOWNLOAD_LIST_2_PRINCIPAL_ID, true).withIsBackupId(true),
			new FieldColumn("etag", COL_DOWNLOAD_LIST_2_ETAG).withIsEtag(true),
			new FieldColumn("updatedOn", COL_DOWNLOAD_LIST_2_UPDATED_ON), };

	public static final TableMapping<DBODownloadList2> MAPPING = new TableMapping<DBODownloadList2>() {

		@Override
		public DBODownloadList2 mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBODownloadList2 dbo = new DBODownloadList2();
			dbo.setEtag(rs.getString(COL_DOWNLOAD_LIST_2_ETAG));
			dbo.setPrincipalId(rs.getLong(COL_DOWNLOAD_LIST_2_PRINCIPAL_ID));
			dbo.setUpdatedOn(rs.getLong(COL_DOWNLOAD_LIST_2_UPDATED_ON));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_DOWNLOAD_LIST_2;
		}

		@Override
		public String getDDLFileName() {
			return DDL_DOWNLOAD_2_LIST;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBODownloadList2> getDBOClass() {
			return DBODownloadList2.class;
		}
	};
	
	@Override
	public TableMapping<DBODownloadList2> getTableMapping() {
		return MAPPING;
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}

	public Long getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Long updatedOn) {
		this.updatedOn = updatedOn;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DOWNLOAD_LIST_2;
	}

	@Override
	public MigratableTableTranslation<DBODownloadList2, DBODownloadList2> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBODownloadList2> getBackupClass() {
		return DBODownloadList2.class;
	}

	@Override
	public Class<? extends DBODownloadList2> getDatabaseObjectClass() {
		return DBODownloadList2.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return Lists.newArrayList(new DBODownloadListItem2());
	}

	@Override
	public int hashCode() {
		return Objects.hash(etag, principalId, updatedOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBODownloadList2)) {
			return false;
		}
		DBODownloadList2 other = (DBODownloadList2) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(principalId, other.principalId)
				&& Objects.equals(updatedOn, other.updatedOn);
	}

	@Override
	public String toString() {
		return "DBODownloadList2 [principalId=" + principalId + ", updatedOn=" + updatedOn + ", etag=" + etag + "]";
	}

}
