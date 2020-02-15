package org.sagebionetworks.table.model;

import java.util.Objects;
import java.util.Set;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.util.ValidateArgument;

public class ListColumnRowChanges {
	private ColumnModel columnModel;
	private Set<Long> rowIds;


	public ListColumnRowChanges(ColumnModel columnModel, Set<Long> rowIds) {
		ValidateArgument.required(columnModel, "columnModel");
		ValidateArgument.requirement(ColumnTypeListMappings.isList(columnModel.getColumnType()),
				"columnModel must have a LIST columnType");
		ValidateArgument.requiredNotEmpty(rowIds, "rowIds");

		this.columnModel = columnModel;
		this.rowIds = rowIds;
	}

	public ColumnModel getColumnModel() {
		return columnModel;
	}

	public Set<Long> getRowIds() {
		return rowIds;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ListColumnRowChanges that = (ListColumnRowChanges) o;
		return columnModel.equals(that.columnModel) &&
				rowIds.equals(that.rowIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnModel, rowIds);
	}

	@Override
	public String toString() {
		return "ListColumnChanges{" +
				"columnModel=" + columnModel +
				", rowIds=" + rowIds +
				'}';
	}
}
