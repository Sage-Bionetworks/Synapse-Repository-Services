package org.sagebionetworks.table.cluster;

import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * Information about a database extracted using 'SHOW COLUMNS FROM T123;'
 *
 */
public class ColumnDefinition {
		
	private String name;
	private ColumnType columnType;
	private Long maxSize;
	private boolean hasIndex;
	
	/**
	 * Column name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Column name.
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Column type. 
	 * @return
	 */
	public ColumnType getColumnType() {
		return columnType;
	}
	
	/**
	 * Column type. 
	 * @param columnType
	 */
	public void setColumnType(ColumnType columnType) {
		this.columnType = columnType;
	}
	/**
	 * For columns with max size.  For example
	 * 'varchar(115)' has a max size of 115.
	 * 
	 * @return
	 */
	public Long getMaxSize() {
		return maxSize;
	}
	
	/**
	 * For columns with max size.  For example
	 * 'varchar(115)' has a max size of 115.
	 * @param maxSize
	 */
	public void setMaxSize(Long maxSize) {
		this.maxSize = maxSize;
	}
	
	/**
	 * Does the column have an Index?
	 * @return
	 */
	public boolean hasIndex() {
		return hasIndex;
	}
	
	/**
	 * Does the column have an Index?
	 * @param hasIndex
	 */
	public void setHasIndex(boolean hasIndex) {
		this.hasIndex = hasIndex;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((columnType == null) ? 0 : columnType.hashCode());
		result = prime * result + (hasIndex ? 1231 : 1237);
		result = prime * result + ((maxSize == null) ? 0 : maxSize.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		ColumnDefinition other = (ColumnDefinition) obj;
		if (columnType != other.columnType)
			return false;
		if (hasIndex != other.hasIndex)
			return false;
		if (maxSize == null) {
			if (other.maxSize != null)
				return false;
		} else if (!maxSize.equals(other.maxSize))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ColumnDefinition [name=" + name + ", columnType=" + columnType
				+ ", maxSize=" + maxSize + ", hasIndex=" + hasIndex + "]";
	}
	
	
}