package org.sagebionetworks.repo.model.dao;

/**
 * Information about the creator of a FileHandle.
 *
 */
public class FileHandleCreator {
	
	String fileHanelId;
	String creatorUserId;
	
	public FileHandleCreator(String fileHanelId, String creatorUserId) {
		super();
		this.fileHanelId = fileHanelId;
		this.creatorUserId = creatorUserId;
	}
	public String getFileHanelId() {
		return fileHanelId;
	}
	public void setFileHanelId(String fileHanelId) {
		this.fileHanelId = fileHanelId;
	}
	public String getCreatorUserId() {
		return creatorUserId;
	}
	public void setCreatorUserId(String creatorUserId) {
		this.creatorUserId = creatorUserId;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((creatorUserId == null) ? 0 : creatorUserId.hashCode());
		result = prime * result
				+ ((fileHanelId == null) ? 0 : fileHanelId.hashCode());
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
		FileHandleCreator other = (FileHandleCreator) obj;
		if (creatorUserId == null) {
			if (other.creatorUserId != null)
				return false;
		} else if (!creatorUserId.equals(other.creatorUserId))
			return false;
		if (fileHanelId == null) {
			if (other.fileHanelId != null)
				return false;
		} else if (!fileHanelId.equals(other.fileHanelId))
			return false;
		return true;
	}

}
