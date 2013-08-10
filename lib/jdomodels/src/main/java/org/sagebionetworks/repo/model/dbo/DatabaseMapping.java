package org.sagebionetworks.repo.model.dbo;

import org.springframework.jdbc.core.RowMapper;




/**
 * The mapping of a database object (DBO) to its database table.
 * This object is immutable.
 * @author John
 *
 */
public class DatabaseMapping <T extends DatabaseObject> {
	
	private String tableName;
	private String DDLFilename;
	private FieldColumn[] fieldColumns;
	private RowMapper<T> mapper;
	
	/**
	 * The only constructor.
	 * @param tableName
	 * @param fieldColumns
	 */
	public DatabaseMapping(String tableName, String DDLFilename, FieldColumn[] fieldColumns, RowMapper<T> mapper) {
		if(tableName == null) throw new IllegalArgumentException("Table name cannot be null");
		if(DDLFilename == null) throw new IllegalArgumentException("DDLFilename cannot be null");
		if(fieldColumns == null) throw new IllegalArgumentException("FieldColumn[] cannot be null");
		this.tableName = tableName;
		this.DDLFilename = DDLFilename;
		this.fieldColumns = fieldColumns;
		this.mapper = mapper;
	}
	/**
	 * The name of the database table.
	 * @return
	 */
	public String getTableName() {
		return tableName;
	}
	/**
	 * The list of FieldColumn that maps fields and column names.
	 * @return
	 */
	public FieldColumn[] getFieldColumns() {
		return fieldColumns;
	}
	
	/**
	 * The Data Definition Language (DDL) file for this table.
	 * @return
	 */
	public String getDDLFilename() {
		return DDLFilename;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fieldColumns == null) ? 0 : fieldColumns.hashCode());
		result = prime * result
				+ ((tableName == null) ? 0 : tableName.hashCode());
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
		DatabaseMapping other = (DatabaseMapping) obj;
		if (fieldColumns == null) {
			if (other.fieldColumns != null)
				return false;
		} else if (!fieldColumns.equals(other.fieldColumns))
			return false;
		if (tableName == null) {
			if (other.tableName != null)
				return false;
		} else if (!tableName.equals(other.tableName))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DBOMapping [tableName=" + tableName + ", fieldColumns="
				+ fieldColumns + "]";
	}	

}
