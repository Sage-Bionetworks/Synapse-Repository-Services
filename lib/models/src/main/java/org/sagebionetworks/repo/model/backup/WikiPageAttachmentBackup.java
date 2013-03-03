package org.sagebionetworks.repo.model.backup;

/**
 * Backup of a wikipage attachment
 * @author John
 *
 */
public class WikiPageAttachmentBackup {
	
	private Long fileHandleId;
	private String fileName;
	public WikiPageAttachmentBackup(){
	}
	public WikiPageAttachmentBackup(Long fileHandleId, String fileName) {
		super();
		this.fileHandleId = fileHandleId;
		this.fileName = fileName;
	}
	
	public Long getFileHandleId() {
		return fileHandleId;
	}
	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		WikiPageAttachmentBackup other = (WikiPageAttachmentBackup) obj;
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
		return "WikiPageAttachmentBackup [fileHandleId=" + fileHandleId
				+ ", fileName=" + fileName + "]";
	}


}
