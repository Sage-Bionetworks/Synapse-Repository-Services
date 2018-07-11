package org.sagebionetworks.repo.model.dbo.file;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_BUCKET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_DDL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_NUMBER_OF_PARTS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_REQUEST_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_STARTED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_REQUEST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MULTIPART_UPLOAD;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * The master table used to track the state of multi-part uploads.
 *
 */
public class DBOMultipartUpload implements MigratableDatabaseObject<DBOMultipartUpload, DBOMultipartUpload>{
		
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_MULTIPART_UPLOAD_ID, true).withIsBackupId(true),
		new FieldColumn("requestHash", COL_MULTIPART_REQUEST_HASH),
		new FieldColumn("etag", COL_MULTIPART_UPLOAD_ETAG).withIsEtag(true),
		new FieldColumn("requestBlob", COL_MULTIPART_UPLOAD_REQUEST),
		new FieldColumn("startedBy", COL_MULTIPART_STARTED_BY),
		new FieldColumn("startedOn", COL_MULTIPART_STARTED_ON),
		new FieldColumn("updatedOn", COL_MULTIPART_UPDATED_ON),
		new FieldColumn("fileHandleId", COL_MULTIPART_FILE_HANDLE_ID),
		new FieldColumn("state", COL_MULTIPART_STATE),
		new FieldColumn("uploadToken", COL_MULTIPART_UPLOAD_TOKEN),
		new FieldColumn("bucket", COL_MULTIPART_BUCKET),
		new FieldColumn("key", COL_MULTIPART_KEY),
		new FieldColumn("numberOfParts", COL_MULTIPART_NUMBER_OF_PARTS)
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
	String uploadToken;
	String bucket;
	String key;
	Integer numberOfParts;

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
				dbo.setStartedOn(new Date(rs.getTimestamp(COL_MULTIPART_STARTED_ON).getTime()));
				dbo.setUpdatedOn(new Date(rs.getTimestamp(COL_MULTIPART_UPDATED_ON).getTime()));
				dbo.setFileHandleId(rs.getLong(COL_MULTIPART_FILE_HANDLE_ID));
				if(rs.wasNull()){
					dbo.setFileHandleId(null);
				}
				dbo.setState(rs.getString(COL_MULTIPART_STATE));
				dbo.setRequestBlob(rs.getBytes(COL_MULTIPART_UPLOAD_REQUEST));
				dbo.setUploadToken(rs.getString(COL_MULTIPART_UPLOAD_TOKEN));
				dbo.setBucket(rs.getString(COL_MULTIPART_BUCKET));
				dbo.setKey(rs.getString(COL_MULTIPART_KEY));
				dbo.setNumberOfParts(rs.getInt(COL_MULTIPART_NUMBER_OF_PARTS));
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
		return MigrationType.MULTIPART_UPLOAD;
	}

	@Override
	public MigratableTableTranslation<DBOMultipartUpload, DBOMultipartUpload> getTranslator() {
		return new BasicMigratableTableTranslation<DBOMultipartUpload>();
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
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new DBOMultipartUploadPartState());
		return list;
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

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getUploadToken() {
		return uploadToken;
	}

	public void setUploadToken(String uploadToken) {
		this.uploadToken = uploadToken;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Integer getNumberOfParts() {
		return numberOfParts;
	}

	public void setNumberOfParts(Integer numberOfParts) {
		this.numberOfParts = numberOfParts;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result
				+ ((numberOfParts == null) ? 0 : numberOfParts.hashCode());
		result = prime * result + Arrays.hashCode(requestBlob);
		result = prime * result
				+ ((requestHash == null) ? 0 : requestHash.hashCode());
		result = prime * result
				+ ((startedBy == null) ? 0 : startedBy.hashCode());
		result = prime * result
				+ ((startedOn == null) ? 0 : startedOn.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result
				+ ((updatedOn == null) ? 0 : updatedOn.hashCode());
		result = prime * result
				+ ((uploadToken == null) ? 0 : uploadToken.hashCode());
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
		if (bucket == null) {
			if (other.bucket != null)
				return false;
		} else if (!bucket.equals(other.bucket))
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (numberOfParts == null) {
			if (other.numberOfParts != null)
				return false;
		} else if (!numberOfParts.equals(other.numberOfParts))
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
		if (updatedOn == null) {
			if (other.updatedOn != null)
				return false;
		} else if (!updatedOn.equals(other.updatedOn))
			return false;
		if (uploadToken == null) {
			if (other.uploadToken != null)
				return false;
		} else if (!uploadToken.equals(other.uploadToken))
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
				+ ", uploadToken=" + uploadToken + ", bucket=" + bucket
				+ ", key=" + key + ", numberOfParts=" + numberOfParts + "]";
	}


}
