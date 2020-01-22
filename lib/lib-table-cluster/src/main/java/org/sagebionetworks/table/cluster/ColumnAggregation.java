package org.sagebionetworks.table.cluster;

import java.util.Objects;

/**
 * Results of of a column model aggregation query.
 *
 */
public class ColumnAggregation {

	String columnName;
	String columnTypeConcat;
	Long maxStringElementSize;
	Long maxListSize;
	
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
	public Long getMaxListSize() {
		return maxListSize;
	}
	public void setMaxListSize(Long maxListSize) {
		this.maxListSize = maxListSize;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ColumnAggregation that = (ColumnAggregation) o;
		return Objects.equals(columnName, that.columnName) &&
				Objects.equals(columnTypeConcat, that.columnTypeConcat) &&
				Objects.equals(maxStringElementSize, that.maxStringElementSize) &&
				Objects.equals(maxListSize, that.maxListSize);
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnName, columnTypeConcat, maxStringElementSize, maxListSize);
	}

	@Override
	public String toString() {
		return "ColumnAggregation{" +
				"columnName='" + columnName + '\'' +
				", columnTypeConcat='" + columnTypeConcat + '\'' +
				", maxStringElementSize=" + maxStringElementSize +
				", listSize=" + maxListSize +
				'}';
	}
}
