package org.sagebionetworks.table.cluster.view.filter;

import java.util.Objects;

/**
 * Generic ID and version pair.
 *
 */
public class IdVersionPair {

	private Long id;
	private Long version;

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public IdVersionPair setId(Long id) {
		this.id = id;
		return this;
	}

	/**
	 * @return the version
	 */
	public Long getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public IdVersionPair setVersion(Long version) {
		this.version = version;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof IdVersionPair)) {
			return false;
		}
		IdVersionPair other = (IdVersionPair) obj;
		return Objects.equals(id, other.id) && Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return "IdVersionPair [id=" + id + ", version=" + version + "]";
	}

}
