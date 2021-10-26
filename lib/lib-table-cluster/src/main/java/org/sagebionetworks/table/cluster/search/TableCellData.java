package org.sagebionetworks.table.cluster.search;

import java.util.Objects;

import org.sagebionetworks.repo.model.table.ColumnModel;

/**
 * DTO for the content of a cell in a table
 */
public class TableCellData {
	
	private ColumnModel columnModel;
	private String data;

	public TableCellData(ColumnModel columnModel, String data) {
		this.columnModel = columnModel;
		this.data = data;
	}
	
	public ColumnModel getColumnModel() {
		return columnModel;
	}
	
	public String getData() {
		return data;
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnModel, data);
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
		TableCellData other = (TableCellData) obj;
		return Objects.equals(columnModel, other.columnModel) && Objects.equals(data, other.data);
	}
	

}
