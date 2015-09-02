package org.sagebionetworks.repo.model;

public class FileHandleIdNameContentType {
	private String fileHandleId;
	private String fileName;
	private String contentType;
	
	public FileHandleIdNameContentType() {}

	public FileHandleIdNameContentType(String fileHandleId, String fileName,
			String contentType) {
		super();
		this.fileHandleId = fileHandleId;
		this.fileName = fileName;
		this.contentType = contentType;
	}

	public String getFileHandleId() {
		return fileHandleId;
	}

	public void setFileHandleId(String fileHandleId) {
		this.fileHandleId = fileHandleId;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contentType == null) ? 0 : contentType.hashCode());
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result
				+ ((fileName == null) ? 0 : fileName.hashCode());
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
		FileHandleIdNameContentType other = (FileHandleIdNameContentType) obj;
		if (contentType == null) {
			if (other.contentType != null)
				return false;
		} else if (!contentType.equals(other.contentType))
			return false;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		return true;
	}

}
