package org.sagebionetworks.repo.model.dbo.file;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_BUCKET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_DDL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_NUMBER_OF_PARTS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_PART_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_REQUEST_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_REQUEST_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_SOURCE_FILE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_SOURCE_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_STARTED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_REQUEST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MULTIPART_UPLOAD;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

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

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
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
		new FieldColumn("uploadType", COL_MULTIPART_UPLOAD_TYPE),
		new FieldColumn("bucket", COL_MULTIPART_BUCKET),
		new FieldColumn("key", COL_MULTIPART_KEY),
		new FieldColumn("numberOfParts", COL_MULTIPART_NUMBER_OF_PARTS),
		new FieldColumn("requestType", COL_MULTIPART_REQUEST_TYPE),
		new FieldColumn("partSize", COL_MULTIPART_PART_SIZE),
		new FieldColumn("sourceFileHandleId", COL_MULTIPART_SOURCE_FILE_HANDLE_ID),
		new FieldColumn("sourceFileEtag", COL_MULTIPART_SOURCE_FILE_ETAG)
	};
	
	private static final TableMapping<DBOMultipartUpload> TABLE_MAPPING = new TableMapping<DBOMultipartUpload>() {

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
			dbo.setUploadType(rs.getString(COL_MULTIPART_UPLOAD_TYPE));
			dbo.setBucket(rs.getString(COL_MULTIPART_BUCKET));
			dbo.setKey(rs.getString(COL_MULTIPART_KEY));
			dbo.setNumberOfParts(rs.getInt(COL_MULTIPART_NUMBER_OF_PARTS));
			dbo.setRequestType(rs.getString(COL_MULTIPART_REQUEST_TYPE));
			dbo.setPartSize(rs.getLong(COL_MULTIPART_PART_SIZE));
			if (rs.wasNull()) {
				dbo.setPartSize(null);
			}
			dbo.setSourceFileHandleId(rs.getLong(COL_MULTIPART_SOURCE_FILE_HANDLE_ID));
			if (rs.wasNull()) {
				dbo.setSourceFileHandleId(null);
			}
			dbo.setSourceFileEtag(rs.getString(COL_MULTIPART_SOURCE_FILE_ETAG));
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
		};
	};
	
	private static final MigratableTableTranslation<DBOMultipartUpload, DBOMultipartUpload> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();
	
	private Long id;
	private String requestHash;
	private String etag;
	private byte[] requestBlob;
	private Long startedBy;
	private Date startedOn;
	private Date updatedOn;
	private Long fileHandleId;
	private String state;
	private String uploadToken;
	private String bucket;
	private String key;
	private Integer numberOfParts;
	private String uploadType;
	private String requestType;
	private Long partSize;
	private Long sourceFileHandleId;
	private String sourceFileEtag;

	@Override
	public TableMapping<DBOMultipartUpload> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MULTIPART_UPLOAD;
	}

	@Override
	public MigratableTableTranslation<DBOMultipartUpload, DBOMultipartUpload> getTranslator() {
		return MIGRATION_TRANSLATOR;
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
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<>();
		list.add(new DBOMultipartUploadPartState());
		list.add(new DBOMultipartUploadComposerPartState());
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

	public String getUploadType() {
		return uploadType;
	}

	public void setUploadType(String uploadType) {
		this.uploadType = uploadType;
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
	
	public String getRequestType() {
		return requestType;
	}
	
	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	public Long getSourceFileHandleId() {
		return sourceFileHandleId;
	}
	
	public void setSourceFileHandleId(Long sourceFileHandleId) {
		this.sourceFileHandleId = sourceFileHandleId;
	}
	
	public String getSourceFileEtag() {
		return sourceFileEtag;
	}
	
	public void setSourceFileEtag(String sourceFileEtag) {
		this.sourceFileEtag = sourceFileEtag;
	}
	
	public Long getPartSize() {
		return partSize;
	}
	
	public void setPartSize(Long partSize) {
		this.partSize = partSize;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(requestBlob);
		result = prime * result + Objects.hash(bucket, etag, fileHandleId, id, key, numberOfParts, partSize,
				requestHash, requestType, sourceFileEtag, sourceFileHandleId, startedBy, startedOn, state, updatedOn,
				uploadToken, uploadType);
		return result;
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
		DBOMultipartUpload other = (DBOMultipartUpload) obj;
		return Objects.equals(bucket, other.bucket) && Objects.equals(etag, other.etag)
				&& Objects.equals(fileHandleId, other.fileHandleId) && Objects.equals(id, other.id)
				&& Objects.equals(key, other.key) && Objects.equals(numberOfParts, other.numberOfParts)
				&& Objects.equals(partSize, other.partSize) && Arrays.equals(requestBlob, other.requestBlob)
				&& Objects.equals(requestHash, other.requestHash) && Objects.equals(requestType, other.requestType)
				&& Objects.equals(sourceFileEtag, other.sourceFileEtag)
				&& Objects.equals(sourceFileHandleId, other.sourceFileHandleId)
				&& Objects.equals(startedBy, other.startedBy) && Objects.equals(startedOn, other.startedOn)
				&& Objects.equals(state, other.state) && Objects.equals(updatedOn, other.updatedOn)
				&& Objects.equals(uploadToken, other.uploadToken) && Objects.equals(uploadType, other.uploadType);
	}

	@Override
	public String toString() {
		return "DBOMultipartUpload [id=" + id + ", requestHash=" + requestHash + ", etag=" + etag + ", requestBlob="
				+ Arrays.toString(requestBlob) + ", startedBy=" + startedBy + ", startedOn=" + startedOn
				+ ", updatedOn=" + updatedOn + ", fileHandleId=" + fileHandleId + ", state=" + state + ", uploadToken="
				+ uploadToken + ", bucket=" + bucket + ", key=" + key + ", numberOfParts=" + numberOfParts
				+ ", uploadType=" + uploadType + ", requestType=" + requestType + ", partSize=" + partSize
				+ ", sourceFileHandleId=" + sourceFileHandleId + ", sourceFileEtag=" + sourceFileEtag + "]";
	}

}
