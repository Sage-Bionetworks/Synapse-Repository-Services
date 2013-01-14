package org.sagebionetworks.repo.manager.file;

/**
 * Data about a file that was uploaded.
 * @author John
 *
 */
public class FileData {
	
	private String fileName;
	private String fileToken;
	private String contentType;
	private String md5;
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileToken() {
		return fileToken;
	}
	public void setFileToken(String fileToken) {
		this.fileToken = fileToken;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public String getMd5() {
		return md5;
	}
	public void setMd5(String md5) {
		this.md5 = md5;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contentType == null) ? 0 : contentType.hashCode());
		result = prime * result
				+ ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result
				+ ((fileToken == null) ? 0 : fileToken.hashCode());
		result = prime * result + ((md5 == null) ? 0 : md5.hashCode());
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
		FileData other = (FileData) obj;
		if (contentType == null) {
			if (other.contentType != null)
				return false;
		} else if (!contentType.equals(other.contentType))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (fileToken == null) {
			if (other.fileToken != null)
				return false;
		} else if (!fileToken.equals(other.fileToken))
			return false;
		if (md5 == null) {
			if (other.md5 != null)
				return false;
		} else if (!md5.equals(other.md5))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "FileData [fileName=" + fileName + ", fileToken=" + fileToken
				+ ", contentType=" + contentType + ", md5=" + md5 + "]";
	}

}
