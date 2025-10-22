package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import java.util.Objects;

import org.sagebionetworks.repo.model.table.ColumnType;

class ColumnMapping {

	private final String columnName;
	private final ColumnType type;
	private final int csvIndex;
	private final int gridIndex;
	private final boolean isUpsertColumn;

	ColumnMapping(String columnName, ColumnType type, int csvIndex, int gridIndex, boolean isUpsertColumn) {
		this.columnName = columnName;
		this.type = type;
		this.csvIndex = csvIndex;
		this.gridIndex = gridIndex;
		this.isUpsertColumn = isUpsertColumn;
	}

	public String getColumnName() {
		return columnName;
	}

	public ColumnType getType() {
		return type;
	}

	public int getCsvIndex() {
		return csvIndex;
	}

	public int getGridIndex() {
		return gridIndex;
	}

	public boolean isUpsertColumn() {
		return isUpsertColumn;
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnName, csvIndex, gridIndex, isUpsertColumn, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ColumnMapping)) {
			return false;
		}
		ColumnMapping other = (ColumnMapping) obj;
		return Objects.equals(columnName, other.columnName) && csvIndex == other.csvIndex && gridIndex == other.gridIndex && isUpsertColumn == other.isUpsertColumn
			&& type == other.type;
	}

	@Override
	public String toString() {
		return "ColumnMapping [columnName=" + columnName + ", type=" + type + ", csvIndex=" + csvIndex + ", gridIndex=" + gridIndex + ", isUpsertColumn=" + isUpsertColumn + "]";
	}

}