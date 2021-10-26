package org.sagebionetworks.table.cluster.search;

import java.util.List;
import java.util.Objects;

/**
 * DTO for the content of a row in a table that is searcheable
 */
public class TableRowData {
	
	private Long rowId;

	private List<TableCellData> rowData;

	public TableRowData(Long rowId, List<TableCellData> rowData) {
		this.rowId = rowId;
		this.rowData = rowData;
	}

	public Long getRowId() {
		return rowId;
	}
	
	public List<TableCellData> getRowData() {
		return rowData;
	}

	@Override
	public int hashCode() {
		return Objects.hash(rowData, rowId);
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
		return Objects.equals(rowData, other.rowData) && Objects.equals(rowId, other.rowId);
	}
	
	
	
}
