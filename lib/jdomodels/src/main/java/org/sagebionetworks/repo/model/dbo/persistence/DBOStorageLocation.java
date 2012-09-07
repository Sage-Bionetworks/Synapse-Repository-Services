package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_CONTENT_MD5;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_CONTENT_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_CONTENT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_IS_ATTACHMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_LOCATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_STORAGE_PROVIDER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_STORAGE_LOCATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STORAGE_LOCATION;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sagebionetworks.repo.model.dbo.AutoIncrementDatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * External storage locations.
 *
 * @author ewu
 */
public class DBOStorageLocation implements AutoIncrementDatabaseObject<DBOStorageLocation> {

	private Long id;         // Primary key off a auto-sequence
	private Long nodeId;     // The node that owns this location
	private Long userId;     // The user that owns this location
	private String location; // The location, usually the path or the URL to the storage
	private Boolean isAttachment;   // Is this an attachment
	private String storageProvider; // Who provides the storage. Example: AWS S3, local storage.
	private String contentType;     // Plain text, PDF, PNG, etc.
	private Long contentSize;       // The size in bytes
	private String contentMd5;      // MD5 on the content

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_STORAGE_LOCATION_ID, true),
			new FieldColumn("nodeId", COL_STORAGE_LOCATION_NODE_ID),
			new FieldColumn("userId", COL_STORAGE_LOCATION_USER_ID),
			new FieldColumn("location", COL_STORAGE_LOCATION_LOCATION),
			new FieldColumn("isAttachment", COL_STORAGE_LOCATION_IS_ATTACHMENT),
			new FieldColumn("storageProvider", COL_STORAGE_LOCATION_STORAGE_PROVIDER),
			new FieldColumn("contentType", COL_STORAGE_LOCATION_CONTENT_TYPE),
			new FieldColumn("contentSize", COL_STORAGE_LOCATION_CONTENT_SIZE),
			new FieldColumn("contentMd5", COL_STORAGE_LOCATION_CONTENT_MD5)
		};

	@Override
	public TableMapping<DBOStorageLocation> getTableMapping() {

		return new TableMapping<DBOStorageLocation>(){

			@Override
			public DBOStorageLocation mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBOStorageLocation locData = new DBOStorageLocation();
				locData.setId(rs.getLong(COL_STORAGE_LOCATION_ID));
				locData.setNodeId(rs.getLong(COL_STORAGE_LOCATION_NODE_ID));
				locData.setUserId(rs.getLong(COL_STORAGE_LOCATION_USER_ID));
				locData.setLocation(rs.getString(COL_STORAGE_LOCATION_LOCATION));
				locData.setIsAttachment(rs.getBoolean(COL_STORAGE_LOCATION_IS_ATTACHMENT));
				locData.setStorageProvider(rs.getString(COL_STORAGE_LOCATION_STORAGE_PROVIDER));
				locData.setContentType(rs.getString(COL_STORAGE_LOCATION_CONTENT_TYPE));
				locData.setContentSize(rs.getLong(COL_STORAGE_LOCATION_CONTENT_SIZE));
				locData.setContentMd5(rs.getString(COL_STORAGE_LOCATION_CONTENT_MD5));
				return locData;
			}

			@Override
			public String getTableName() {
				return TABLE_STORAGE_LOCATION;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_STORAGE_LOCATION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOStorageLocation> getDBOClass() {
				return DBOStorageLocation.class;
			}};
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		if (id == null) {
			throw new NullPointerException();
		}
		this.id = id;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		if (nodeId == null) {
			throw new NullPointerException();
		}
		this.nodeId = nodeId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		if (userId == null) {
			throw new NullPointerException();
		}
		this.userId = userId;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		if (location == null || location.length() == 0) {
			throw new IllegalArgumentException();
		}
		this.location = location;
	}

	public Boolean getIsAttachment() {
		return isAttachment;
	}

	public void setIsAttachment(Boolean isAttachment) {
		if (isAttachment == null) {
			throw new NullPointerException();
		}
		this.isAttachment = isAttachment;
	}

	public String getStorageProvider() {
		return storageProvider;
	}

	public void setStorageProvider(String storageProvider) {
		if (storageProvider == null || storageProvider.length() == 0) {
			throw new IllegalArgumentException();
		}
		this.storageProvider = storageProvider;
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

	public String getContentMd5() {
		return contentMd5;
	}

	public void setContentMd5(String contentMd5) {
		this.contentMd5 = contentMd5;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj.getClass() != this.getClass()) {
			return false;
		}
		DBOStorageLocation that = (DBOStorageLocation)obj;
		return new EqualsBuilder().
				append(this.id, that.id).
				append(this.nodeId, that.nodeId).
				append(this.userId, that.userId).
				append(this.isAttachment, that.isAttachment).
				append(this.storageProvider, that.storageProvider).
				append(this.location, that.location).
				append(this.contentType, that.contentType).
				append(this.contentSize, that.contentSize).
				append(this.contentMd5, that.contentMd5).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).
				append(id).
				append(nodeId).
				append(userId).
				append(isAttachment).
				append(storageProvider).
				append(location).
				append(contentType).
				append(contentSize).
				append(contentMd5).
				hashCode();
	}
}
