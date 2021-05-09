package org.sagebionetworks.repo.manager.file.multipart;

import java.util.Objects;

/**
 * DTO to create a file handle when a multipart request is completed.
 * 
 * @author Marco Marasca
 *
 */
public class FileHandleCreateRequest {
	
	private String fileName;
	private String contentType;
	private String contentMD5;
	private Long storageLocationId;
	private Boolean generatePreview;
	
	public FileHandleCreateRequest(String fileName, String contentType, String contentMD5, Long storageLocationId, Boolean generatePreview) {
		this.fileName = fileName;
		this.contentType = contentType;
		this.contentMD5 = contentMD5;
		this.storageLocationId = storageLocationId;
		this.generatePreview = generatePreview;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public String getContentMD5() {
		return contentMD5;
	}
	
	public Long getStorageLocationId() {
		return storageLocationId;
	}

	public Boolean getGeneratePreview() {
		return generatePreview;
	}

	@Override
	public int hashCode() {
		return Objects.hash(contentMD5, contentType, fileName, generatePreview, storageLocationId);
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
		FileHandleCreateRequest other = (FileHandleCreateRequest) obj;
		return Objects.equals(contentMD5, other.contentMD5) && Objects.equals(contentType, other.contentType)
				&& Objects.equals(fileName, other.fileName) && Objects.equals(generatePreview, other.generatePreview)
				&& Objects.equals(storageLocationId, other.storageLocationId);
	}

	@Override
	public String toString() {
		return "FileHandleCreateRequest [fileName=" + fileName + ", contentType=" + contentType + ", contentMD5="
				+ contentMD5 + ", storageLocationId=" + storageLocationId + ", generatePreview=" + generatePreview
				+ "]";
	}
	
}
