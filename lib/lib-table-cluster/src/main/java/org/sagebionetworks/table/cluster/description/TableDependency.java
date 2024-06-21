package org.sagebionetworks.table.cluster.description;

import java.util.Objects;
import java.util.Optional;

public class TableDependency implements Comparable<TableDependency> {

	private IndexDescription indexDescription;
	private String tableAlias;
	private String indexAlias;

	public IndexDescription getIndexDescription() {
		return indexDescription;
	}

	public TableDependency withIndexDescription(IndexDescription indexDescription) {
		this.indexDescription = indexDescription;
		return this;
	}

	public Optional<String> getTableAlias() {
		return Optional.ofNullable(tableAlias);
	}

	public TableDependency withTableAlias(String tableAlias) {
		this.tableAlias = tableAlias;
		return this;
	}

	public String getIndexAlias() {
		return indexAlias;
	}

	public TableDependency withIndexAlias(String indexAlias) {
		this.indexAlias = indexAlias;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(indexAlias, indexDescription, tableAlias);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TableDependency other = (TableDependency) obj;
		return Objects.equals(indexAlias, other.indexAlias) && Objects.equals(indexDescription, other.indexDescription)
				&& Objects.equals(tableAlias, other.tableAlias);
	}

	@Override
	public String toString() {
		return "TableDependency [indexDescription=" + indexDescription + ", tableAlias=" + tableAlias + "]";
	}

	@Override
	public int compareTo(TableDependency o) {
		return this.indexDescription.compareTo(o.getIndexDescription());
	}
	
	

}
