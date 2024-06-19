package org.sagebionetworks.table.cluster.utils;

import java.util.Objects;
import java.util.Optional;

import org.sagebionetworks.repo.model.entity.IdAndVersion;

public class TableIdAndAlias {

	private IdAndVersion idAndVersion;
	private String alias;

	public IdAndVersion getIdAndVersion() {
		return idAndVersion;
	}

	public TableIdAndAlias withIdAndVersion(IdAndVersion idAndVersion) {
		this.idAndVersion = idAndVersion;
		return this;
	}

	public Optional<String> getAlias() {
		return Optional.ofNullable(alias);
	}

	public TableIdAndAlias withAlias(String alias) {
		this.alias = alias;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(alias, idAndVersion);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TableIdAndAlias other = (TableIdAndAlias) obj;
		return Objects.equals(alias, other.alias) && Objects.equals(idAndVersion, other.idAndVersion);
	}

	@Override
	public String toString() {
		return "Dependency [idAndVersion=" + idAndVersion + ", alias=" + alias + "]";
	}

}
