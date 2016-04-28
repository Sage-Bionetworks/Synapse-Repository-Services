package org.sagebionetworks.repo.model.dbo.wikiV2;

public class WikiAttachment {

	private String fileName;
	private String fileHandleId;

	public WikiAttachment(String id, String name) {
		this.fileHandleId = id;
		this.fileName = name;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileHandleId() {
		return fileHandleId;
	}
	public void setFileHandleId(String fileHandleId) {
		this.fileHandleId = fileHandleId;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
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
		WikiAttachment other = (WikiAttachment) obj;
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
	@Override
	public String toString() {
		return "WikiAttachment [fileName=" + fileName + ", fileHandleId=" + fileHandleId + "]";
	}
}
