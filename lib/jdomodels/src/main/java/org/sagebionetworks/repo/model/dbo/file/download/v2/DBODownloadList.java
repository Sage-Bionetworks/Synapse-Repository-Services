package org.sagebionetworks.repo.model.dbo.file.download.v2;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DOWNLOAD_V2_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_V2;

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

import com.google.common.collect.Lists;

public class DBODownloadList implements MigratableDatabaseObject<DBODownloadList, DBODownloadList> {

	private Long principalId;
	private Timestamp updatedOn;
	private String etag;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("principalId", COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID, true).withIsBackupId(true),
			new FieldColumn("etag", COL_DOWNLOAD_LIST_V2_ETAG).withIsEtag(true),
			new FieldColumn("updatedOn", COL_DOWNLOAD_LIST_V2_UPDATED_ON)};

	public static final TableMapping<DBODownloadList> MAPPING = new TableMapping<DBODownloadList>() {

		@Override
		public DBODownloadList mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBODownloadList dbo = new DBODownloadList();
			dbo.setEtag(rs.getString(COL_DOWNLOAD_LIST_V2_ETAG));
			dbo.setPrincipalId(rs.getLong(COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID));
			dbo.setUpdatedOn(rs.getTimestamp(COL_DOWNLOAD_LIST_V2_UPDATED_ON));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_DOWNLOAD_LIST_V2;
		}

		@Override
		public String getDDLFileName() {
			return DDL_DOWNLOAD_V2_LIST;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBODownloadList> getDBOClass() {
			return DBODownloadList.class;
		}
	};
	
	@Override
	public TableMapping<DBODownloadList> getTableMapping() {
		return MAPPING;
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}

	public Timestamp getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Timestamp updatedOn) {
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
	public MigratableTableTranslation<DBODownloadList, DBODownloadList> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBODownloadList> getBackupClass() {
		return DBODownloadList.class;
	}

	@Override
	public Class<? extends DBODownloadList> getDatabaseObjectClass() {
		return DBODownloadList.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return Lists.newArrayList(new DBODownloadListItem());
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
		if (!(obj instanceof DBODownloadList)) {
			return false;
		}
		DBODownloadList other = (DBODownloadList) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(principalId, other.principalId)
				&& Objects.equals(updatedOn, other.updatedOn);
	}

	@Override
	public String toString() {
		return "DBODownloadList2 [principalId=" + principalId + ", updatedOn=" + updatedOn + ", etag=" + etag + "]";
	}

}
