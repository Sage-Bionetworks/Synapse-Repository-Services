package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.Objects;

public class Column {

	private String name;
	private Integer vectorIndex;

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

	@Override
	public int hashCode() {
		return Objects.hash(name, vectorIndex);
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
		return Objects.equals(name, other.name) && Objects.equals(vectorIndex, other.vectorIndex);
	}

	@Override
	public String toString() {
		return "Column [name=" + name + ", vectorIndex=" + vectorIndex + "]";
	}

}
