package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.file.FileHandleAssociation;

/**
 * Describes a FileHandleAssociation and a user's authorization status for that association.
 */
public class FileHandleAuthorizationStatus {
	
	FileHandleAssociation association;
	AuthorizationStatus status;

	public FileHandleAuthorizationStatus(FileHandleAssociation association,
			AuthorizationStatus status) {
		super();
		this.association = association;
		this.status = status;
	}
	public FileHandleAssociation getAssociation() {
		return association;
	}
	public void setAssociation(FileHandleAssociation association) {
		this.association = association;
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
				+ ((association == null) ? 0 : association.hashCode());
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
		if (association == null) {
			if (other.association != null)
				return false;
		} else if (!association.equals(other.association))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		return true;
	}
	
	
}
