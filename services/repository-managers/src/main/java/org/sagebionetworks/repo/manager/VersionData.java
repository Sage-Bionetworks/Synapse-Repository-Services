package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.file.FileHandle;
/**
 * Track data about a version. Used when converting entity types.
 * @author John
 *
 */
public class VersionData {
	Long versionNumber;
	FileHandle fileHandle;
	String modifiedBy;
	String versionLabel;
	String versionComments;
	
	public VersionData(Long versionNumber, FileHandle fileHandle,
			String modifiedBy, String versionLabel, String versionComments) {
		super();
		this.versionNumber = versionNumber;
		this.fileHandle = fileHandle;
		this.modifiedBy = modifiedBy;
		this.versionLabel = versionLabel;
	}
	public Long getVersionNumber() {
		return versionNumber;
	}
	public FileHandle getFileHandle() {
		return fileHandle;
	}
	public String getModifiedBy() {
		return modifiedBy;
	}
	public String getVersionLabel() {
		return versionLabel;
	}
	public String getVersionComments() {
		return versionComments;
	}
	
}
