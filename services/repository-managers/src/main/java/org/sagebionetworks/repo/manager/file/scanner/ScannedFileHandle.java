package org.sagebionetworks.repo.manager.file.scanner;

import java.util.Objects;

public class ScannedFileHandle {

	private String objectId;
	private Long fileHandleId;

	public ScannedFileHandle(String objectId, Long fileHandleId) {
		this.objectId = objectId;
		this.fileHandleId = fileHandleId;
	}

	public String getObjectId() {
		return objectId;
	}

	public Long getFileHandleId() {
		return fileHandleId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fileHandleId, objectId);
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
		ScannedFileHandle other = (ScannedFileHandle) obj;
		return Objects.equals(fileHandleId, other.fileHandleId) && Objects.equals(objectId, other.objectId);
	}

	@Override
	public String toString() {
		return "ScannedFileHandle [objectId=" + objectId + ", fileHandleId=" + fileHandleId + "]";
	}

}
