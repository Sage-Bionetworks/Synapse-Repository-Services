package org.sagebionetworks.repo.manager;


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
}
