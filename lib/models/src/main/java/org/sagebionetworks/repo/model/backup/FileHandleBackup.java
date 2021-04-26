package org.sagebionetworks.repo.model.backup;

import java.sql.Timestamp;
import java.util.Objects;

import org.sagebionetworks.repo.model.dao.FileHandleStatus;

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
	private Timestamp updatedOn;
	private String metadataType;
	private String contentType;
	private Long contentSize;
	private String contentMD5;
	private String bucketName;
	private String key;
	private String name;
	private Long storageLocationId;
	private String endpoint;
	private Boolean isPreview;
	private FileHandleStatus status;

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
	
	public Timestamp getUpdatedOn() {
		return updatedOn;
	}
	
	public void setUpdatedOn(Timestamp updatedOn) {
		this.updatedOn = updatedOn;
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

	public Long getStorageLocationId() {
		return storageLocationId;
	}

	public void setStorageLocationId(Long storageLocationId) {
		this.storageLocationId = storageLocationId;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public Boolean getIsPreview() {
		return isPreview;
	}

	public void setIsPreview(Boolean isPreview) {
		this.isPreview = isPreview;
	}

	public FileHandleStatus getStatus() {
		return status;
	}

	public void setStatus(FileHandleStatus status) {
		this.status = status;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bucketName, contentMD5, contentSize, contentType, createdBy, createdOn, endpoint, etag, id, isPreview, key,
				metadataType, name, previewId, status, storageLocationId, updatedOn);
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
		FileHandleBackup other = (FileHandleBackup) obj;
		return Objects.equals(bucketName, other.bucketName) && Objects.equals(contentMD5, other.contentMD5)
				&& Objects.equals(contentSize, other.contentSize) && Objects.equals(contentType, other.contentType)
				&& Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(endpoint, other.endpoint) && Objects.equals(etag, other.etag) && Objects.equals(id, other.id)
				&& Objects.equals(isPreview, other.isPreview) && Objects.equals(key, other.key)
				&& Objects.equals(metadataType, other.metadataType) && Objects.equals(name, other.name)
				&& Objects.equals(previewId, other.previewId) && status == other.status
				&& Objects.equals(storageLocationId, other.storageLocationId) && Objects.equals(updatedOn, other.updatedOn);
	}

	@Override
	public String toString() {
		return "FileHandleBackup [id=" + id + ", etag=" + etag + ", previewId=" + previewId + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + ", updatedOn=" + updatedOn + ", metadataType=" + metadataType + ", contentType=" + contentType
				+ ", contentSize=" + contentSize + ", contentMD5=" + contentMD5 + ", bucketName=" + bucketName + ", key=" + key + ", name="
				+ name + ", storageLocationId=" + storageLocationId + ", endpoint=" + endpoint + ", isPreview=" + isPreview + ", status="
				+ status + "]";
	}

}
