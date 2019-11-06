package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.auth.AuthorizationStatus;


/**
 * Describes a FileHandleAssociation and a user's authorization status for that association.
 */
public class FileHandleAuthorizationStatus {
	
	String fileHandleId;
	AuthorizationStatus status;
	
	public FileHandleAuthorizationStatus(String fileHandleId,
			AuthorizationStatus status) {
		super();
		this.fileHandleId = fileHandleId;
		this.status = status;
	}

	public String getFileHandleId() {
		return fileHandleId;
	}
	
	public void setFileHandleId(String fileHandleId) {
		this.fileHandleId = fileHandleId;
	}
	
	public AuthorizationStatus getStatus() {
		return status;
	}
	
	public void setStatus(AuthorizationStatus status) {
		this.status = status;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
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
		FileHandleAuthorizationStatus other = (FileHandleAuthorizationStatus) obj;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FileHandleAuthorizationStatus [fileHandleId=" + fileHandleId
				+ ", status=" + status + "]";
	}
	
}

