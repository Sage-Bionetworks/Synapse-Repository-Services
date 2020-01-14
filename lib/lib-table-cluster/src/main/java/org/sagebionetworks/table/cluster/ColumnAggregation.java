package org.sagebionetworks.table.cluster;
/**
 * Results of of a column model aggregation query.
 *
 */
public class ColumnAggregation {

	String columnName;
	String columnTypeConcat;
	Long maxStringElementSize;
	Long listSize;
	
	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	public String getColumnTypeConcat() {
		return columnTypeConcat;
	}
	public void setColumnTypeConcat(String columnTypeConcat) {
		this.columnTypeConcat = columnTypeConcat;
	}
	public Long getMaxStringElementSize() {
		return maxStringElementSize;
	}
	public void setMaxStringElementSize(Long maxStringElementSize) {
		this.maxStringElementSize = maxStringElementSize;
	}
	public Long getListSize() {
		return listSize;
	}
	public void setListSize(Long listSize) {
		this.listSize = listSize;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
		result = prime * result + ((columnTypeConcat == null) ? 0 : columnTypeConcat.hashCode());
		result = prime * result + ((maxStringElementSize == null) ? 0 : maxStringElementSize.hashCode());
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
		ColumnAggregation other = (ColumnAggregation) obj;
		if (columnName == null) {
			if (other.columnName != null)
				return false;
		} else if (!columnName.equals(other.columnName))
			return false;
		if (columnTypeConcat == null) {
			if (other.columnTypeConcat != null)
				return false;
		} else if (!columnTypeConcat.equals(other.columnTypeConcat))
			return false;
		if (maxStringElementSize == null) {
			if (other.maxStringElementSize != null)
				return false;
		} else if (!maxStringElementSize.equals(other.maxStringElementSize))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "ColumnAggregation [columnName=" + columnName + ", columnTypeConcat=" + columnTypeConcat + ", maxSize="
				+ maxStringElementSize + "]";
	}
	
	
}
