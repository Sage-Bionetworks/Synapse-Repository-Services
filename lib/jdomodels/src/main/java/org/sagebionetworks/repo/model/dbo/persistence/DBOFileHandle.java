package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_MD5;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_METADATA_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_PREVIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * The DBO object for file metadata.
 * 
 * @author John
 *
 */
public class DBOFileHandle implements DatabaseObject<DBOFileHandle> {
	
	/**
	 * The type of metadata represented.
	 * @author John
	 *
	 */
	public static enum MetadataType {
		S3,
		EXTERNAL,
		PREVIEW,
	}
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_FILES_ID, true),
		new FieldColumn("etag", COL_FILES_ETAG),
		new FieldColumn("previewId", COL_FILES_PREVIEW_ID),
		new FieldColumn("createdBy", COL_FILES_CREATED_BY),
		new FieldColumn("createdOn", COL_FILES_CREATED_ON),
		new FieldColumn("metadataType", COL_FILES_METADATA_TYPE),
		new FieldColumn("contentType", COL_FILES_CONTENT_TYPE),
		new FieldColumn("contentSize", COL_FILES_CONTENT_SIZE),
		new FieldColumn("contentMD5", COL_FILES_CONTENT_MD5),
		new FieldColumn("bucketName", COL_FILES_BUCKET_NAME),
		new FieldColumn("key", COL_FILES_KEY),
		new FieldColumn("name", COL_FILES_NAME),
	};
	
	private Long id;
	private String etag;
	private Long previewId;
	private Long createdBy;
	private Timestamp createdOn;
	private MetadataType metadataType;
	private String contentType;
	private Long contentSize;
	private String contentMD5;
	private String bucketName;
	private String key;
	private String name;

	@Override
	public TableMapping<DBOFileHandle> getTableMapping() {
		return new TableMapping<DBOFileHandle>() {
			
			@Override
			public DBOFileHandle mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOFileHandle results = new DBOFileHandle();
				results.setId(rs.getLong(COL_FILES_ID));
				results.setEtag(rs.getString(COL_FILES_ETAG));
				// This can be null
				results.setPreviewId(rs.getLong(COL_FILES_PREVIEW_ID));
				if(rs.wasNull()){
					results.setPreviewId(null);
				}
				results.setCreatedBy(rs.getLong(COL_FILES_CREATED_BY));
				results.setCreatedOn(rs.getTimestamp(COL_FILES_CREATED_ON));
				results.setMetadataType(MetadataType.valueOf(rs.getString(COL_FILES_METADATA_TYPE)));
				results.setContentType(rs.getString(COL_FILES_CONTENT_TYPE));
				results.setContentSize(rs.getLong(COL_FILES_CONTENT_SIZE));
				if(rs.wasNull()){
					results.setContentSize(null);
				}
				results.setContentMD5(rs.getString(COL_FILES_CONTENT_MD5));
				results.setBucketName(rs.getString(COL_FILES_BUCKET_NAME));
				results.setKey(rs.getString(COL_FILES_KEY));
				results.setName(rs.getString(COL_FILES_NAME));
				return results;
			}
			
			@Override
			public String getTableName() {
				return TABLE_FILES;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_FILES;
			}
			
			@Override
			public Class<? extends DBOFileHandle> getDBOClass() {
				return DBOFileHandle.class;
			}
		};
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPreviewId() {
		return previewId;
	}

	public void setPreviewId(Long previewId) {
		this.previewId = previewId;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public MetadataType getMetadataTypeEnum() {
		return metadataType;
	}
	
	public String getMetadataType() {
		return metadataType.name();
	}

	public void setMetadataType(MetadataType metadataType) {
		this.metadataType = metadataType;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Long getContentSize() {
		return contentSize;
	}

	public void setContentSize(Long contentSize) {
		this.contentSize = contentSize;
	}

	public String getContentMD5() {
		return contentMD5;
	}

	public void setContentMD5(String contentMD5) {
		this.contentMD5 = contentMD5;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((bucketName == null) ? 0 : bucketName.hashCode());
		result = prime * result
				+ ((contentMD5 == null) ? 0 : contentMD5.hashCode());
		result = prime * result
				+ ((contentSize == null) ? 0 : contentSize.hashCode());
		result = prime * result
				+ ((contentType == null) ? 0 : contentType.hashCode());
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result
				+ ((metadataType == null) ? 0 : metadataType.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((previewId == null) ? 0 : previewId.hashCode());
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
		DBOFileHandle other = (DBOFileHandle) obj;
		if (bucketName == null) {
			if (other.bucketName != null)
				return false;
		} else if (!bucketName.equals(other.bucketName))
			return false;
		if (contentMD5 == null) {
			if (other.contentMD5 != null)
				return false;
		} else if (!contentMD5.equals(other.contentMD5))
			return false;
		if (contentSize == null) {
			if (other.contentSize != null)
				return false;
		} else if (!contentSize.equals(other.contentSize))
			return false;
		if (contentType == null) {
			if (other.contentType != null)
				return false;
		} else if (!contentType.equals(other.contentType))
			return false;
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
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
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
		if (metadataType != other.metadataType)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (previewId == null) {
			if (other.previewId != null)
				return false;
		} else if (!previewId.equals(other.previewId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOFileMetadata [id=" + id + ", etag=" + etag + ", previewId="
				+ previewId + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + ", metadataType=" + metadataType
				+ ", contentType=" + contentType + ", contentSize="
				+ contentSize + ", contentMD5=" + contentMD5 + ", bucketName="
				+ bucketName + ", key=" + key + ", name=" + name + "]";
	}

}
