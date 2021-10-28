package org.sagebionetworks.table.cluster.search;

import java.util.List;
import java.util.Objects;

/**
 * DTO for the content of a row in a table that is searcheable
 */
public class TableRowData {
	
	private Long rowId;

	private List<TypedCellValue> rowValues;

	public TableRowData(Long rowId, List<TypedCellValue> rowValues) {
		this.rowId = rowId;
		this.rowValues = rowValues;
	}

	public Long getRowId() {
		return rowId;
	}
	
	public List<TypedCellValue> getRowValues() {
		return rowValues;
	}

	@Override
	public int hashCode() {
		return Objects.hash(rowValues, rowId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TableRowData other = (TableRowData) obj;
		return Objects.equals(rowValues, other.rowValues) && Objects.equals(rowId, other.rowId);
	}
	
	
	
}
