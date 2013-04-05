package org.sagebionetworks.repo.model.dbo;

/**
 * Binds a java field name to a database column.
 * 
 * This object is immutable.
 * 
 * @author jmhill
 *
 */
public class FieldColumn {
	
	private String fieldName;
	private String columnName;
	private boolean isPrimaryKey = false;
	private boolean isEtag = false;
	private boolean isBackupId = false;
	
	/**
	 * The only constructor.
	 * @param fieldName
	 * @param columnName
	 */
	public FieldColumn(String fieldName, String columnName) {
		this(fieldName, columnName, false);;
	}
	
	public FieldColumn(String fieldName, String columnName, boolean isPrimary) {
		super();
		this.fieldName = fieldName;
		this.columnName = columnName;
		this.isPrimaryKey = isPrimary;
	}

	/**
	 * The name of the Java field.
	 * @return
	 */
	public String getFieldName() {
		return fieldName;
	}
	
	/**
	 * The name of the database column.
	 * @return
	 */
	public String getColumnName() {
		return columnName;
	}

	public boolean isPrimaryKey() {
		return isPrimaryKey;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((columnName == null) ? 0 : columnName.hashCode());
		result = prime * result
				+ ((fieldName == null) ? 0 : fieldName.hashCode());
		result = prime * result + (isPrimaryKey ? 1231 : 1237);
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
		FieldColumn other = (FieldColumn) obj;
		if (columnName == null) {
			if (other.columnName != null)
				return false;
		} else if (!columnName.equals(other.columnName))
			return false;
		if (fieldName == null) {
			if (other.fieldName != null)
				return false;
		} else if (!fieldName.equals(other.fieldName))
			return false;
		if (isPrimaryKey != other.isPrimaryKey)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FieldColumn [fieldName=" + fieldName + ", columnName="
				+ columnName + ", isPrimaryKey=" + isPrimaryKey + "]";
	}

	

}
