package org.sagebionetworks.repo.model;

import java.util.Objects;

public class IdAndChecksum {

	private Long id;
	private Long checksum;
	
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public IdAndChecksum withId(Long id) {
		this.id = id;
		return this;
	}
	/**
	 * @return the checksum
	 */
	public Long getChecksum() {
		return checksum;
	}
	/**
	 * @param checksum the checksum to set
	 */
	public IdAndChecksum withChecksum(Long checksum) {
		this.checksum = checksum;
		return this;
	}
	@Override
	public int hashCode() {
		return Objects.hash(checksum, id);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof IdAndChecksum)) {
			return false;
		}
		IdAndChecksum other = (IdAndChecksum) obj;
		return Objects.equals(checksum, other.checksum) && Objects.equals(id, other.id);
	}
	
	@Override
	public String toString() {
		return "IdAndChecksum [id=" + id + ", checksum=" + checksum + "]";
	}	
	
}
