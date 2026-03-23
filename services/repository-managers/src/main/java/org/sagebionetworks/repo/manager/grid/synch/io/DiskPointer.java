package org.sagebionetworks.repo.manager.grid.synch.io;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a pointer to a serialized row stored on disk during
 * synchronization. Used to enable O(n) memory usage by avoiding loading all
 * source rows into memory at once.
 *
 * <p>
 * During Phase 2 (row synchronization), source rows are serialized to disk and
 * indexed by key. This pointer allows the synchronization logic to:
 * <ul>
 * <li>Quickly locate rows on disk using offset and length</li>
 * <li>Verify data integrity using the hash</li>
 * <li>Match rows between copy and source using the key</li>
 * <li>Only load rows into memory when needed for comparison or merging</li>
 * </ul>
 */
public class DiskPointer {

	private final String key;
	private final byte[] hash;
	private final long offset;
	private final int length;

	/**
	 * Creates a pointer to a serialized row on disk.
	 *
	 * @param key    the unique identifier for the row (used to match rows between
	 *               copy and source)
	 * @param hash   the hash of the serialized row data (for integrity
	 *               verification)
	 * @param offset the byte offset in the file where the row data starts
	 * @param length the number of bytes of the serialized row data
	 */
	public DiskPointer(String key, byte[] hash, long offset, int length) {
		super();
		this.key = key;
		this.hash = hash;
		this.offset = offset;
		this.length = length;
	}

	/**
	 * Gets the unique identifier for this row. Used to match rows between copy and
	 * source during synchronization.
	 *
	 * @return the row's unique key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Gets the hash of the serialized row data. Used to verify data integrity when
	 * reading from disk.
	 *
	 * @return the hash bytes of the row data
	 */
	public byte[] getHash() {
		return hash;
	}

	/**
	 * Gets the byte offset in the file where this row's data starts.
	 *
	 * @return the offset in bytes from the start of the file
	 */
	public long getOffset() {
		return offset;
	}

	/**
	 * Gets the length of the serialized row data in bytes.
	 *
	 * @return the number of bytes to read from the offset
	 */
	public int getLength() {
		return length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(hash);
		result = prime * result + Objects.hash(key, length, offset);
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
		DiskPointer other = (DiskPointer) obj;
		return Arrays.equals(hash, other.hash) && Objects.equals(key, other.key) && length == other.length
				&& offset == other.offset;
	}

}
