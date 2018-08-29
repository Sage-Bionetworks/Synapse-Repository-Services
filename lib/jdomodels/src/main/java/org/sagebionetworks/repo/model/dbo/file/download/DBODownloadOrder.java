package org.sagebionetworks.repo.model.dbo.file.download;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_ORDER_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_ORDER_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_ORDER_FILES_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_ORDER_FILE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_ORDER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_ORDER_TOTAL_NUM_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_ORDER_TOTAL_SIZE_MB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DOWNLOAD_ORDER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_ORDER;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBODownloadOrder implements MigratableDatabaseObject<DBODownloadOrder, DBODownloadOrder> {

	private Long ordeId;
	private Long createdBy;
	private Long createdOn;
	private String zipFileName;
	private Long totalSizeMB;
	private Long totalNumberOfFiles;
	private byte[] files;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("ordeId", COL_DOWNLOAD_ORDER_ID, true).withIsBackupId(true),
			new FieldColumn("createdBy", COL_DOWNLOAD_ORDER_CREATED_BY),
			new FieldColumn("createdOn", COL_DOWNLOAD_ORDER_CREATED_ON),
			new FieldColumn("zipFileName", COL_DOWNLOAD_ORDER_FILE_NAME),
			new FieldColumn("totalSizeMB", COL_DOWNLOAD_ORDER_TOTAL_SIZE_MB),
			new FieldColumn("totalNumberOfFiles", COL_DOWNLOAD_ORDER_TOTAL_NUM_FILES),
			new FieldColumn("files", COL_DOWNLOAD_ORDER_FILES_BLOB),
	};

	@Override
	public TableMapping<DBODownloadOrder> getTableMapping() {
		return new TableMapping<DBODownloadOrder>() {

			@Override
			public DBODownloadOrder mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBODownloadOrder order = new DBODownloadOrder();
				order.setOrdeId(rs.getLong(COL_DOWNLOAD_ORDER_ID));
				order.setCreatedBy(rs.getLong(COL_DOWNLOAD_ORDER_CREATED_BY));
				order.setCreatedOn(rs.getLong(COL_DOWNLOAD_ORDER_CREATED_ON));
				order.setZipFileName(rs.getString(COL_DOWNLOAD_ORDER_FILE_NAME));
				order.setTotalSizeMB(rs.getLong(COL_DOWNLOAD_ORDER_TOTAL_SIZE_MB));
				order.setTotalNumberOfFiles(rs.getLong(COL_DOWNLOAD_ORDER_TOTAL_NUM_FILES));
				java.sql.Blob blob = rs.getBlob(COL_DOWNLOAD_ORDER_FILES_BLOB);
				if(blob != null){
					order.setFiles(blob.getBytes(1, (int) blob.length()));
				}
				return order;
			}

			@Override
			public String getTableName() {
				return TABLE_DOWNLOAD_ORDER;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DOWNLOAD_ORDER;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBODownloadOrder> getDBOClass() {
				return DBODownloadOrder.class;
			}
		};
	}

	public Long getOrdeId() {
		return ordeId;
	}

	public void setOrdeId(Long ordeId) {
		this.ordeId = ordeId;
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

	public String getZipFileName() {
		return zipFileName;
	}

	public void setZipFileName(String zipFileName) {
		this.zipFileName = zipFileName;
	}

	public Long getTotalSizeMB() {
		return totalSizeMB;
	}

	public void setTotalSizeMB(Long totalSizeMB) {
		this.totalSizeMB = totalSizeMB;
	}

	public Long getTotalNumberOfFiles() {
		return totalNumberOfFiles;
	}

	public void setTotalNumberOfFiles(Long totalNumberOfFiles) {
		this.totalNumberOfFiles = totalNumberOfFiles;
	}

	public byte[] getFiles() {
		return files;
	}

	public void setFiles(byte[] files) {
		this.files = files;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DOWNLOAD_ORDER;
	}

	@Override
	public MigratableTableTranslation<DBODownloadOrder, DBODownloadOrder> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBODownloadOrder> getBackupClass() {
		return DBODownloadOrder.class;
	}

	@Override
	public Class<? extends DBODownloadOrder> getDatabaseObjectClass() {
		return DBODownloadOrder.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + Arrays.hashCode(files);
		result = prime * result + ((ordeId == null) ? 0 : ordeId.hashCode());
		result = prime * result + ((totalNumberOfFiles == null) ? 0 : totalNumberOfFiles.hashCode());
		result = prime * result + ((totalSizeMB == null) ? 0 : totalSizeMB.hashCode());
		result = prime * result + ((zipFileName == null) ? 0 : zipFileName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBODownloadOrder other = (DBODownloadOrder) obj;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (!Arrays.equals(files, other.files))
			return false;
		if (ordeId == null) {
			if (other.ordeId != null)
				return false;
		} else if (!ordeId.equals(other.ordeId))
			return false;
		if (totalNumberOfFiles == null) {
			if (other.totalNumberOfFiles != null)
				return false;
		} else if (!totalNumberOfFiles.equals(other.totalNumberOfFiles))
			return false;
		if (totalSizeMB == null) {
			if (other.totalSizeMB != null)
				return false;
		} else if (!totalSizeMB.equals(other.totalSizeMB))
			return false;
		if (zipFileName == null) {
			if (other.zipFileName != null)
				return false;
		} else if (!zipFileName.equals(other.zipFileName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBODownloadOrder [ordeId=" + ordeId + ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", zipFileName=" + zipFileName + ", totalSizeMB=" + totalSizeMB + ", totalNumberOfFiles="
				+ totalNumberOfFiles + ", files=" + Arrays.toString(files) + "]";
	}

}
