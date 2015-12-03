package org.sagebionetworks.repo.model.dbo.file;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * The master table used to track the state of multi-part uploads.
 *
 */
public class DBOMultipartUpload implements MigratableDatabaseObject<DBOMultipartUpload, DBOMultipartUpload>{
		
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_MULTIPART_UPLOAD_ID, true).withIsBackupId(true),
		new FieldColumn("requestHash", COL_MULTIPART_REQUEST_HASH, true).withIsBackupId(true),
		new FieldColumn("etag", COL_MULTIPART_UPLOAD_ETAG).withIsEtag(true),
		new FieldColumn("requestBlob", COL_MULTIPART_UPLOAD_REQUEST),
		new FieldColumn("startedBy", COL_MULTIPART_STARTED_BY),
		new FieldColumn("startedOn", COL_MULTIPART_STARTED_ON),
		new FieldColumn("updatedOn", COL_MULTIPART_UPDATED_ON),
		new FieldColumn("fileHandleId", COL_MULTIPART_FILE_HANDLE_ID),
		new FieldColumn("state", COL_MULTIPART_STATE),
		new FieldColumn("storageLocationId", COL_MULTIPART_STORAGE_LOCATION_ID),
		new FieldColumn("storageLocationToken", COL_MULTIPART_STORAGE_LOCATION_TOKEN),
		
	};
	
	Long id;
	String requestHash;
	String etag;
	byte[] requestBlob;
	Long startedBy;
	Date startedOn;
	Date updatedOn;
	Long fileHandleId;
	String state;
	Long storageLocationId;
	String storageLocationToken;

	@Override
	public TableMapping<DBOMultipartUpload> getTableMapping() {
		return new TableMapping<DBOMultipartUpload>(){

			@Override
			public DBOMultipartUpload mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBOMultipartUpload dbo = new DBOMultipartUpload();
				dbo.setId(rs.getLong(COL_MULTIPART_UPLOAD_ID));
				dbo.setRequestHash(rs.getString(COL_MULTIPART_REQUEST_HASH));
				dbo.setEtag(rs.getString(COL_MULTIPART_UPLOAD_ETAG));
				dbo.setStartedBy(rs.getLong(COL_MULTIPART_STARTED_BY));
				dbo.setStartedOn(rs.getDate(COL_MULTIPART_STARTED_ON));
				dbo.setUpdatedOn(rs.getDate(COL_MULTIPART_UPDATED_ON));
				dbo.setFileHandleId(rs.getLong(COL_MULTIPART_FILE_HANDLE_ID));
				dbo.setState(rs.getString(COL_MULTIPART_STATE));
				dbo.setStorageLocationId(rs.getLong(COL_MULTIPART_STORAGE_LOCATION_ID));
				dbo.setRequestBlob(rs.getBytes(COL_MULTIPART_UPLOAD_REQUEST));
				dbo.setStorageLocationToken(rs.getString(COL_MULTIPART_STORAGE_LOCATION_TOKEN));
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

	public String getRequestHash() {
		return requestHash;
	}

	public void setRequestHash(String requestHash) {
		this.requestHash = requestHash;
	}

	public Date getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Date updatedOn) {
		this.updatedOn = updatedOn;
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

	public byte[] getRequestBlob() {
		return requestBlob;
	}

	public void setRequestBlob(byte[] requestBlob) {
		this.requestBlob = requestBlob;
	}

	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}

	public Long getStorageLocationId() {
		return storageLocationId;
	}

	public void setStorageLocationId(Long storageLocationId) {
		this.storageLocationId = storageLocationId;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getStorageLocationToken() {
		return storageLocationToken;
	}

	public void setStorageLocationToken(String storageLocationToken) {
		this.storageLocationToken = storageLocationToken;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + Arrays.hashCode(requestBlob);
		result = prime * result
				+ ((requestHash == null) ? 0 : requestHash.hashCode());
		result = prime * result
				+ ((startedBy == null) ? 0 : startedBy.hashCode());
		result = prime * result
				+ ((startedOn == null) ? 0 : startedOn.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime
				* result
				+ ((storageLocationId == null) ? 0 : storageLocationId
						.hashCode());
		result = prime
				* result
				+ ((storageLocationToken == null) ? 0 : storageLocationToken
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (!Arrays.equals(requestBlob, other.requestBlob))
			return false;
		if (requestHash == null) {
			if (other.requestHash != null)
				return false;
		} else if (!requestHash.equals(other.requestHash))
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
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		if (storageLocationId == null) {
			if (other.storageLocationId != null)
				return false;
		} else if (!storageLocationId.equals(other.storageLocationId))
			return false;
		if (storageLocationToken == null) {
			if (other.storageLocationToken != null)
				return false;
		} else if (!storageLocationToken.equals(other.storageLocationToken))
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
		return "DBOMultipartUpload [id=" + id + ", requestHash=" + requestHash
				+ ", etag=" + etag + ", requestBlob="
				+ Arrays.toString(requestBlob) + ", startedBy=" + startedBy
				+ ", startedOn=" + startedOn + ", updatedOn=" + updatedOn
				+ ", fileHandleId=" + fileHandleId + ", state=" + state
				+ ", storageLocationId=" + storageLocationId
				+ ", storageLocationToken=" + storageLocationToken + "]";
	}

}
