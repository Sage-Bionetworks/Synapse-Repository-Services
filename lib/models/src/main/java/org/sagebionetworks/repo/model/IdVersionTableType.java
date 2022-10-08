package org.sagebionetworks.repo.model;

import java.util.Objects;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;

public class IdVersionTableType {
	
	private final IdAndVersion idAndVersion;
	private final TableType type;
	
	public IdVersionTableType(IdAndVersion idAndVersion, TableType type) {
		super();
		this.idAndVersion = idAndVersion;
		this.type = type;
	}


	/**
	 * @return the idAndVersion
	 */
	public IdAndVersion getIdAndVersion() {
		return idAndVersion;
	}


	/**
	 * @return the type
	 */
	public TableType getType() {
		return type;
	}


	@Override
	public int hashCode() {
		return Objects.hash(idAndVersion, type);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof IdVersionTableType)) {
			return false;
		}
		IdVersionTableType other = (IdVersionTableType) obj;
		return Objects.equals(idAndVersion, other.idAndVersion) && type == other.type;
	}


	@Override
	public String toString() {
		return "IdVersionTableType [idAndVersion=" + idAndVersion + ", type=" + type + "]";
	}

}
