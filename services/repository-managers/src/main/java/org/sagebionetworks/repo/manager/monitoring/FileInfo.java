package org.sagebionetworks.repo.manager.monitoring;

import java.util.Objects;

import org.sagebionetworks.util.ValidateArgument;

public class FileInfo implements Comparable<FileInfo> {

	public static final float BYTES_PER_GB = 1024 * 1024 * 1024;

	private final long sizeBytes;
	private final String name;

	public FileInfo(long sizeBytes, String name) {
		ValidateArgument.required(name, "name");
		this.sizeBytes = sizeBytes;
		this.name = name;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public String getName() {
		return name;
	}

	@Override
	public int compareTo(FileInfo o) {
		int sizeComp = Long.compare(this.sizeBytes, o.sizeBytes);
		if (sizeComp != 0) {
			return sizeComp;
		}
		return name.compareTo(o.name);
	}

	@Override
	public String toString() {
		return String.format("%10.3f GB '%s'", (float)sizeBytes / BYTES_PER_GB, name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, sizeBytes);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileInfo other = (FileInfo) obj;
		return Objects.equals(name, other.name) && sizeBytes == other.sizeBytes;
	}
	
}
