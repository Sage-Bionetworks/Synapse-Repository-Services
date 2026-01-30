package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.CrdtId;

public class Column {

	private String name;
	private Integer vectorIndex;
	private CrdtId columnOrderNodeId;

	public String getName() {
		return name;
	}

	public Column setName(String name) {
		this.name = name;
		return this;
	}

	public Integer getVectorIndex() {
		return vectorIndex;
	}

	public Column setVectorIndex(Integer vectorIndex) {
		this.vectorIndex = vectorIndex;
		return this;
	}
	
	public CrdtId getColumnOrderNodeId() {
		return columnOrderNodeId;
	}
	
	public Column setColumnOrderNodeId(CrdtId columnOrderNodeId) {
		this.columnOrderNodeId = columnOrderNodeId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, vectorIndex, columnOrderNodeId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Column other = (Column) obj;
		return Objects.equals(name, other.name) && Objects.equals(vectorIndex, other.vectorIndex) && Objects.equals(columnOrderNodeId, other.columnOrderNodeId);
	}

	@Override
	public String toString() {
		return "Column [name=" + name + ", vectorIndex=" + vectorIndex + ", columnOrderNodeId=" + columnOrderNodeId + "]";
	}

}
