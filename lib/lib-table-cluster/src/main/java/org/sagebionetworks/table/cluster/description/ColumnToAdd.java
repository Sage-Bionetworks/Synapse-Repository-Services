package org.sagebionetworks.table.cluster.description;

import java.util.Objects;

import org.sagebionetworks.repo.model.entity.IdAndVersion;

public class ColumnToAdd {

	private final IdAndVersion idAndVersion;
	private final String sql;
	
	public ColumnToAdd(IdAndVersion idAndVersion, String sql) {
		super();
		this.idAndVersion = idAndVersion;
		this.sql = sql;
	}

	/**
	 * @return the idAndVersion
	 */
	public IdAndVersion getIdAndVersion() {
		return idAndVersion;
	}

	/**
	 * @return the sql
	 */
	public String getSql() {
		return sql;
	}

	@Override
	public int hashCode() {
		return Objects.hash(idAndVersion, sql);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ColumnToAdd)) {
			return false;
		}
		ColumnToAdd other = (ColumnToAdd) obj;
		return Objects.equals(idAndVersion, other.idAndVersion) && Objects.equals(sql, other.sql);
	}

	@Override
	public String toString() {
		return "ColumnToAdd [idAndVersion=" + idAndVersion + ", sql=" + sql + "]";
	}

	
}
