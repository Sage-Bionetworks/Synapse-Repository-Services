package org.sagebionetworks.repo.model.file;

import java.util.Objects;

/**
 * DTO that contains information about a single archived key request operation
 */
public class FileHandleKeyArchiveResult {

	private int archivedCount;
	private boolean wasTagged;

	public FileHandleKeyArchiveResult(int archivedCount, boolean wasTagged) {
		this.archivedCount = archivedCount;
		this.wasTagged = wasTagged;
	}

	public int getArchivedCount() {
		return archivedCount;
	}

	public FileHandleKeyArchiveResult withArchivedCount(int archivedCount) {
		this.archivedCount = archivedCount;
		return this;
	}

	public boolean isWasTagged() {
		return wasTagged;
	}

	public FileHandleKeyArchiveResult withWasTagged(boolean wasTagged) {
		this.wasTagged = wasTagged;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(archivedCount, wasTagged);
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
		FileHandleKeyArchiveResult other = (FileHandleKeyArchiveResult) obj;
		return archivedCount == other.archivedCount && wasTagged == other.wasTagged;
	}

	@Override
	public String toString() {
		return "FileHandleKeyArchiveResult [archivedCount=" + archivedCount + ", wasTagged=" + wasTagged + "]";
	}

}
