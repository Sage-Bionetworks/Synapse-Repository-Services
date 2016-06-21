package org.sagebionetworks.table.cluster;

import java.util.Comparator;

/**
 * Information about a column in the database.
 *
 */
public class DatabaseColumnInfo {
	
	String columnId;
	boolean hasIndex;
	Long cardinality;
	
	public String getColumnId() {
		return columnId;
	}
	public void setColumnId(String columnId) {
		this.columnId = columnId;
	}
	public boolean hasIndex() {
		return hasIndex;
	}
	public void setHasIndex(boolean hasIndex) {
		this.hasIndex = hasIndex;
	}
	public Long getCardinality() {
		return cardinality;
	}
	public void setCardinality(Long cardinality) {
		this.cardinality = cardinality;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((cardinality == null) ? 0 : cardinality.hashCode());
		result = prime * result
				+ ((columnId == null) ? 0 : columnId.hashCode());
		result = prime * result + (hasIndex ? 1231 : 1237);
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
		DatabaseColumnInfo other = (DatabaseColumnInfo) obj;
		if (cardinality == null) {
			if (other.cardinality != null)
				return false;
		} else if (!cardinality.equals(other.cardinality))
			return false;
		if (columnId == null) {
			if (other.columnId != null)
				return false;
		} else if (!columnId.equals(other.columnId))
			return false;
		if (hasIndex != other.hasIndex)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DatabaseColumnInfo [columnId=" + columnId + ", hasIndex="
				+ hasIndex + ", cardinality=" + cardinality + "]";
	}
	
	/**
	 * Comparator based on DatabaseColumnInfo.cardinality;
	 */
	public static Comparator<DatabaseColumnInfo> CARDINALITY_COMPARATOR = new Comparator<DatabaseColumnInfo>() {
		@Override
		public int compare(DatabaseColumnInfo one, DatabaseColumnInfo two) {
			return Long.compare(one.cardinality, two.cardinality);
		}
	};


	
}
