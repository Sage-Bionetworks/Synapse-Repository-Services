package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * This is an abstraction for table index CRUD operations.
 * @author John
 *
 */
public interface TableIndexDAO {
	
	/**
	 * Create or update a table with the given schema.
	 * 
	 * @param connection
	 * @param schema
	 * @param tableId
	 */
	public boolean createOrUpdateTable(SimpleJdbcTemplate connection, List<ColumnModel> schema, String tableId);
	
	/**
	 * 
	 * @param connection
	 * @param tableId
	 * @return
	 */
	public boolean deleteTable(SimpleJdbcTemplate connection, String tableId); 
}
