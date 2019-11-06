package org.sagebionetworks.repo.model.entity;

import java.util.Optional;

/**
 * Immutable representation of an Entity's ID including an optional version
 * number. Use {@linkplain IdAndVersionBuilder} to create new instances of this
 * class. To parse an EntityId from a string use {@linkplain IdAndVersionParser}
 */
public class IdAndVersion {

	final Long id;
	final Long version;

	/**
	 * Use
	 * 
	 * @param id
	 * @param version
	 */
	IdAndVersion(Long id, Long version) {
		if (id == null) {
			throw new IllegalArgumentException("ID cannot be null");
		}
		this.id = id;
		this.version = version;
	}

	/**
	 * The numeric ID of the Entity.
	 * 
	 * @return
	 */
	public Long getId() {
		return id;
	}

	/**
	 * The numeric version of the Entity. Note: Version is optional an might be
	 * null.
	 * 
	 * @return
	 */
	public Optional<Long> getVersion() {
		return Optional.ofNullable(version);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		IdAndVersion other = (IdAndVersion) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("syn" + id);
		if (version != null) {
			builder.append(".").append(version);
		}
		return builder.toString();
	}

	/**
	 * Parse the provided string into IdAndVersion
	 * 
	 * @param toParse
	 * @return
	 */
	public static IdAndVersion parse(String toParse) {
		return IdAndVersionParser.parseIdAndVersion(toParse);
	}

	/**
	 * Create a new builder used to create an IdAndVersion without parsing.
	 * 
	 * @return
	 */
	public static IdAndVersionBuilder newBuilder() {
		return new IdAndVersionBuilder();
	}
}
