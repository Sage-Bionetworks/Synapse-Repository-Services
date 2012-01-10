package org.sagebionetworks.repo.model.dbo;

import org.springframework.jdbc.core.RowMapper;

/**
 * Maps an object to a Table.
 * 
 * @author jmhill
 *
 * @param <T>
 */
public interface TableMapping<T> extends RowMapper<T> {
	
	/**
	 * The name of the database table.
	 * @return
	 */
	public String getTableName();
	/**
	 * The name of the DDL file that defines this table.
	 * @return
	 */
	public String getDDLFileName();
	
	/**
	 * Maps field names to column names.
	 * @return
	 */
	public FieldColumn[] getFieldColumns();
	
	/**
	 * The class for <T>
	 * @return
	 */
	public Class<? extends T> getDBOClass();

}
