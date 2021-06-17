package org.sagebionetworks.repo.manager.file;

import java.io.File;
import java.util.Objects;

import com.amazonaws.event.ProgressListener;

public class LocalFileUploadRequest {
	
	Long storageLocationId;
	String userId;
	File fileToUpload;
	String contentType;
	String fileName;
	ProgressListener listener;
	
	public Long getStorageLocationId() {
		return storageLocationId;
	}
	public String getUserId() {
		return userId;
	}
	public File getFileToUpload() {
		return fileToUpload;
	}
	public String getContentType() {
		return contentType;
	}
	public String getFileName() {
		return fileName;
	}
	public ProgressListener getListener() {
		return listener;
	}
	public LocalFileUploadRequest withStorageLocationId(Long storageLocationId) {
		this.storageLocationId = storageLocationId;
		return this;
	}
	public LocalFileUploadRequest withUserId(String userId) {
		this.userId = userId;
		return this;
	}
	public LocalFileUploadRequest withFileToUpload(File fileToUpload) {
		this.fileToUpload = fileToUpload;
		return this;
	}
	public LocalFileUploadRequest withContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}
	public LocalFileUploadRequest withFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}
	public LocalFileUploadRequest withListener(ProgressListener listener) {
		this.listener = listener;
		return this;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(contentType, fileName, fileToUpload, listener, storageLocationId, userId);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof LocalFileUploadRequest)) {
			return false;
		}
		LocalFileUploadRequest other = (LocalFileUploadRequest) obj;
		return Objects.equals(contentType, other.contentType) && Objects.equals(fileName, other.fileName)
				&& Objects.equals(fileToUpload, other.fileToUpload) && Objects.equals(listener, other.listener)
				&& Objects.equals(storageLocationId, other.storageLocationId) && Objects.equals(userId, other.userId);
	}
	@Override
	public String toString() {
		return "LocalFileUploadRequest [storageLocationId=" + storageLocationId + ", userId=" + userId
				+ ", fileToUpload=" + fileToUpload + ", contentType=" + contentType + ", fileName=" + fileName
				+ ", listener=" + listener + "]";
	}
	

}
