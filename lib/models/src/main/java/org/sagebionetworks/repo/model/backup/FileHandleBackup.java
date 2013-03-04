package org.sagebionetworks.repo.model.backup;


/**
 * Backup of a FileHandle object.
 * 
 * @author John
 *
 */
public class FileHandleBackup {
	
	private Long id;
	private String etag;
	private Long previewId;
	private Long createdBy;
	private Long createdOn;
	private String metadataType;
	private String contentType;
	private Long contentSize;
	private String contentMD5;
	private String bucketName;
	private String key;
	private String name;
	
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
	public Long getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}
	public String getMetadataType() {
		return metadataType;
	}
	public void setMetadataType(String metadataType) {
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
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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
		FileHandleBackup other = (FileHandleBackup) obj;
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
		if (metadataType == null) {
			if (other.metadataType != null)
				return false;
		} else if (!metadataType.equals(other.metadataType))
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
		return "FileHandleBackup [id=" + id + ", etag=" + etag + ", previewId="
				+ previewId + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + ", metadataType=" + metadataType
				+ ", contentType=" + contentType + ", contentSize="
				+ contentSize + ", contentMD5=" + contentMD5 + ", bucketName="
				+ bucketName + ", key=" + key + ", name=" + name + "]";
	}
	
}
