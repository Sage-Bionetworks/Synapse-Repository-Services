package org.sagebionetworks.repo.model.dbo.file;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOMultipartUpload implements MigratableDatabaseObject<DBOMultipartUpload, DBOMultipartUpload>{
	
	enum State {
		UPLOADING,
		COMPLETE		
	}
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_MULTIPART_UPLOAD_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_MULTIPART_UPLOAD_ETAG).withIsEtag(true),
		new FieldColumn("awsUploadId", COL_MULTIPART_PROVIDER_UPLOAD_ID),
		new FieldColumn("fileSize", COL_MULTIPART_FILE_SIZE),
		new FieldColumn("partSize", COL_MULTIPART_PART_SIZE),
		new FieldColumn("fileMD5Hex", COL_MULTIPART_FILE_MD5_HEX),
		new FieldColumn("fileName", COL_MULTIPART_FILE_NAME),
		new FieldColumn("contentType", COL_MULTIPART_CONTENT_TYPE),
		new FieldColumn("contentEncoding", COL_MULTIPART_CONTENT_ENCODING),
		new FieldColumn("startedBy", COL_MULTIPART_STARTED_BY),
		new FieldColumn("startedOn", COL_MULTIPART_STARTED_ON),
		new FieldColumn("updatedOn", COL_MULTIPART_UPDATED_ON),
		new FieldColumn("fileHandleId", COL_MULTIPART_FILE_HANDLE_ID),
		new FieldColumn("state", COL_MULTIPART_STATE),
		new FieldColumn("storageLocationId", COL_MULTIPART_STORAGE_LOCATION_ID),
	};
	
	Long id;
	String etag;
	String awsUploadId;
	Long fileSize;
	Long partSize;
	String fileMD5Hex;
	String fileName;
	String contentType;
	String contentEncoding;
	Long startedBy;
	Date startedOn;
	Date updatedOn;
	Long fileHandleId;
	State state;
	Long storageLocationId;

	@Override
	public TableMapping<DBOMultipartUpload> getTableMapping() {
		return new TableMapping<DBOMultipartUpload>(){

			@Override
			public DBOMultipartUpload mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBOMultipartUpload dbo = new DBOMultipartUpload();
				dbo.setId(rs.getLong(COL_MULTIPART_UPLOAD_ID));
				dbo.setEtag(rs.getString(COL_MULTIPART_UPLOAD_ETAG));
				dbo.setAwsUploadId(rs.getString(COL_MULTIPART_PROVIDER_UPLOAD_ID));
				dbo.setFileSize(rs.getLong(COL_MULTIPART_FILE_SIZE));
				dbo.setPartSize(rs.getLong(COL_MULTIPART_PART_SIZE));
				dbo.setFileMD5Hex(rs.getString(COL_MULTIPART_FILE_MD5_HEX));
				dbo.setFileName(rs.getString(COL_MULTIPART_FILE_NAME));
				dbo.setContentType(rs.getString(COL_MULTIPART_CONTENT_TYPE));
				dbo.setContentEncoding(rs.getString(COL_MULTIPART_CONTENT_ENCODING));
				dbo.setStartedBy(rs.getLong(COL_MULTIPART_STARTED_BY));
				dbo.setStartedOn(rs.getDate(COL_MULTIPART_STARTED_ON));
				dbo.setFileHandleId(rs.getLong(COL_MULTIPART_FILE_HANDLE_ID));
				dbo.setState(State.valueOf(rs.getString(COL_MULTIPART_STATE)));
				dbo.setStorageLocationId(rs.getLong(COL_MULTIPART_STORAGE_LOCATION_ID));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_MULTIPART_UPLOAD;
			}

			@Override
			public String getDDLFileName() {
				return COL_MULTIPART_DDL;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOMultipartUpload> getDBOClass() {
				return DBOMultipartUpload.class;
			}};
	}

	@Override
	public MigrationType getMigratableTableType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MigratableTableTranslation<DBOMultipartUpload, DBOMultipartUpload> getTranslator() {
		return new MigratableTableTranslation<DBOMultipartUpload, DBOMultipartUpload>(){

			@Override
			public DBOMultipartUpload createDatabaseObjectFromBackup(
					DBOMultipartUpload backup) {
				return backup;
			}

			@Override
			public DBOMultipartUpload createBackupFromDatabaseObject(
					DBOMultipartUpload dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBOMultipartUpload> getBackupClass() {
		return DBOMultipartUpload.class;
	}

	@Override
	public Class<? extends DBOMultipartUpload> getDatabaseObjectClass() {
		return DBOMultipartUpload.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

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

	public String getAwsUploadId() {
		return awsUploadId;
	}

	public void setAwsUploadId(String awsUploadId) {
		this.awsUploadId = awsUploadId;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public Long getPartSize() {
		return partSize;
	}

	public void setPartSize(Long partSize) {
		this.partSize = partSize;
	}

	public String getFileMD5Hex() {
		return fileMD5Hex;
	}

	public void setFileMD5Hex(String fileMD5Hex) {
		this.fileMD5Hex = fileMD5Hex;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getContentEncoding() {
		return contentEncoding;
	}

	public void setContentEncoding(String contentEncoding) {
		this.contentEncoding = contentEncoding;
	}

	public Long getStartedBy() {
		return startedBy;
	}

	public void setStartedBy(Long startedBy) {
		this.startedBy = startedBy;
	}

	public Date getStartedOn() {
		return startedOn;
	}

	public void setStartedOn(Date startedOn) {
		this.startedOn = startedOn;
	}

	public Long getFileHandleId() {
		return fileHandleId;
	}

	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public Long getStorageLocationId() {
		return storageLocationId;
	}

	public void setStorageLocationId(Long storageLocationId) {
		this.storageLocationId = storageLocationId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((awsUploadId == null) ? 0 : awsUploadId.hashCode());
		result = prime * result
				+ ((contentEncoding == null) ? 0 : contentEncoding.hashCode());
		result = prime * result
				+ ((contentType == null) ? 0 : contentType.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result
				+ ((fileMD5Hex == null) ? 0 : fileMD5Hex.hashCode());
		result = prime * result
				+ ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result
				+ ((fileSize == null) ? 0 : fileSize.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((partSize == null) ? 0 : partSize.hashCode());
		result = prime * result
				+ ((startedBy == null) ? 0 : startedBy.hashCode());
		result = prime * result
				+ ((startedOn == null) ? 0 : startedOn.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime
				* result
				+ ((storageLocationId == null) ? 0 : storageLocationId
						.hashCode());
		result = prime * result
				+ ((updatedOn == null) ? 0 : updatedOn.hashCode());
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
		DBOMultipartUpload other = (DBOMultipartUpload) obj;
		if (awsUploadId == null) {
			if (other.awsUploadId != null)
				return false;
		} else if (!awsUploadId.equals(other.awsUploadId))
			return false;
		if (contentEncoding == null) {
			if (other.contentEncoding != null)
				return false;
		} else if (!contentEncoding.equals(other.contentEncoding))
			return false;
		if (contentType == null) {
			if (other.contentType != null)
				return false;
		} else if (!contentType.equals(other.contentType))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (fileMD5Hex == null) {
			if (other.fileMD5Hex != null)
				return false;
		} else if (!fileMD5Hex.equals(other.fileMD5Hex))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (fileSize == null) {
			if (other.fileSize != null)
				return false;
		} else if (!fileSize.equals(other.fileSize))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (partSize == null) {
			if (other.partSize != null)
				return false;
		} else if (!partSize.equals(other.partSize))
			return false;
		if (startedBy == null) {
			if (other.startedBy != null)
				return false;
		} else if (!startedBy.equals(other.startedBy))
			return false;
		if (startedOn == null) {
			if (other.startedOn != null)
				return false;
		} else if (!startedOn.equals(other.startedOn))
			return false;
		if (state != other.state)
			return false;
		if (storageLocationId == null) {
			if (other.storageLocationId != null)
				return false;
		} else if (!storageLocationId.equals(other.storageLocationId))
			return false;
		if (updatedOn == null) {
			if (other.updatedOn != null)
				return false;
		} else if (!updatedOn.equals(other.updatedOn))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOMultipartUpload [id=" + id + ", etag=" + etag
				+ ", awsUploadId=" + awsUploadId + ", fileSize=" + fileSize
				+ ", partSize=" + partSize + ", fileMD5Hex=" + fileMD5Hex
				+ ", fileName=" + fileName + ", contentType=" + contentType
				+ ", contentEncoding=" + contentEncoding + ", startedBy="
				+ startedBy + ", startedOn=" + startedOn + ", updatedOn="
				+ updatedOn + ", fileHandleId=" + fileHandleId + ", state="
				+ state + ", storageLocationId=" + storageLocationId + "]";
	}

}
