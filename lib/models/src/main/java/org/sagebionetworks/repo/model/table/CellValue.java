package org.sagebionetworks.repo.model.table;

/**
 * All of the meta data about a single cell within a table change set.
 * 
 * @author John
 *
 */
public class CellValue {
	
	/**
	 * The column definition for this cell.
	 */
	ColumnModel column;
	/**
	 * The actual string value for this cell.
	 */
	String value;
	/**
	 * The row index of this cell within the change set.
	 */
	int rowIndex;
	/**
	 * The column index of this cell within the change set.
	 */
	int columnIndex;
	
	/**
	 * The column definition for this cell.
	 * @return
	 */
	public ColumnModel getColumn() {
		return column;
	}
	/**
	 * The column definition for this cell.
	 * @param column
	 */
	public void setColumn(ColumnModel column) {
		this.column = column;
	}
	/**
	 * The actual string value for this cell.
	 * @return
	 */
	public String getValue() {
		return value;
	}
	/**
	 * The actual string value for this cell.
	 * @param value
	 */
	public void setValue(String value) {
		this.value = value;
	}
	/**
	 * The row index of this cell within the change set.
	 * @return
	 */
	public int getRowIndex() {
		return rowIndex;
	}
	/**
	 * The row index of this cell within the change set.
	 * @param rowIndex
	 */
	public void setRowIndex(int rowIndex) {
		this.rowIndex = rowIndex;
	}
	/**
	 * The column index of this cell within the change set.
	 * @return
	 */
	public int getColumnIndex() {
		return columnIndex;
	}
	/**
	 * The column index of this cell within the change set.
	 * @param columnIndex
	 */
	public void setColumnIndex(int columnIndex) {
		this.columnIndex = columnIndex;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((column == null) ? 0 : column.hashCode());
		result = prime * result + columnIndex;
		result = prime * result + rowIndex;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CellValue other = (CellValue) obj;
		if (column == null) {
			if (other.column != null)
				return false;
		} else if (!column.equals(other.column))
			return false;
		if (columnIndex != other.columnIndex)
			return false;
		if (rowIndex != other.rowIndex)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "CellValue [column=" + column + ", value=" + value
				+ ", rowIndex=" + rowIndex + ", columnIndex=" + columnIndex
				+ "]";
	}

}
