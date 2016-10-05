package org.sagebionetworks.repo.manager.file;

public class FileHandleOverwriteData {

	private String newFileName;
	private String newContentType;

	public String getNewFileName() {
		return newFileName;
	}
	public void setNewFileName(String newFileName) {
		this.newFileName = newFileName;
	}
	public String getNewContentType() {
		return newContentType;
	}
	public void setNewContentType(String newContentType) {
		this.newContentType = newContentType;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((newContentType == null) ? 0 : newContentType.hashCode());
		result = prime * result + ((newFileName == null) ? 0 : newFileName.hashCode());
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
		FileHandleOverwriteData other = (FileHandleOverwriteData) obj;
		if (newContentType == null) {
			if (other.newContentType != null)
				return false;
		} else if (!newContentType.equals(other.newContentType))
			return false;
		if (newFileName == null) {
			if (other.newFileName != null)
				return false;
		} else if (!newFileName.equals(other.newFileName))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "FileHandleOverwriteData [newFileName=" + newFileName + ", newContentType=" + newContentType + "]";
	}
}
