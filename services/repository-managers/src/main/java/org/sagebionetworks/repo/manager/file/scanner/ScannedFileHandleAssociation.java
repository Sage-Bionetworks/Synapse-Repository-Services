package org.sagebionetworks.repo.manager.file.scanner;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * DTO that holds a list of discovered associations for a given object, the association might
 * contain multiple file handle. The receiver needs to check if the association contains a file
 * handle.
 * 
 * @author Marco Marasca
 *
 */
public class ScannedFileHandleAssociation {

	private String objectId;
	private List<Long> fileHandleIds;

	public ScannedFileHandleAssociation(String objectId) {
		this.objectId = objectId;
	}
	
	public ScannedFileHandleAssociation(String objectId, Long fileHandleId) {
		this.objectId = objectId;
		this.fileHandleIds = Collections.singletonList(fileHandleId);
	}

	/**
	 * @return The id of the object that was scanned
	 */
	public String getObjectId() {
		return objectId;
	}

	/**
	 * @return A potentially empty or null list of file handles that are associated with the object
	 */
	public List<Long> getFileHandleIds() {
		return fileHandleIds;
	}

	/**
	 * Replace the list of discovered file handle ids
	 * 
	 * @param fileHandleIds
	 * @return this
	 */
	public ScannedFileHandleAssociation withFileHandleIds(List<Long> fileHandleIds) {
		this.fileHandleIds = fileHandleIds;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fileHandleIds, objectId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ScannedFileHandleAssociation other = (ScannedFileHandleAssociation) obj;
		return Objects.equals(fileHandleIds, other.fileHandleIds) && Objects.equals(objectId, other.objectId);
	}

	@Override
	public String toString() {
		return "ScannedFileHandleAssociation [objectId=" + objectId + ", fileHandleIds=" + fileHandleIds + "]";
	}
	
	

}
