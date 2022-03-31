package org.sagebionetworks.repo.model.dbo.file.download.v2;

import java.util.Objects;

import org.sagebionetworks.repo.model.download.Action;

/**
 * An action that the user must take to gain access to this file.
 *
 */
public class FileActionRequired {

	private long fileId;
	private Action action;
	/**
	 * @return the fileId
	 */
	public long getFileId() {
		return fileId;
	}
	/**
	 * @param fileId the fileId to set
	 */
	public FileActionRequired withFileId(long fileId) {
		this.fileId = fileId;
		return this;
	}
	/**
	 * @return the action
	 */
	public Action getAction() {
		return action;
	}
	/**
	 * @param action the action to set
	 */
	public FileActionRequired withAction(Action action) {
		this.action = action;
		return this;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(action, fileId);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof FileActionRequired)) {
			return false;
		}
		FileActionRequired other = (FileActionRequired) obj;
		return Objects.equals(action, other.action) && fileId == other.fileId;
	}
	
	@Override
	public String toString() {
		return "FileActionRequired [fileId=" + fileId + ", action=" + action + "]";
	}

	
}
