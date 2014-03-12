package org.sagebionetworks.repo.web.service.table;

import java.io.IOException;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for working with TableEntities
 * 
 * @author John
 *
 */
public interface TableServices {

	/**
	 * Create a new ColumnModel.
	 * 
	 * @param userId
	 * @param model
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public ColumnModel createColumnModel(Long userId, ColumnModel model) throws DatastoreException, NotFoundException;

	/**
	 * Get a ColumnModel for a given ID
	 * 
	 * @param userId
	 * @param columnId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public ColumnModel getColumnModel(Long userId, String columnId) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the ColumnModels for a TableEntity
	 * @param userId
	 * @param entityId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public PaginatedColumnModels getColumnModelsForTableEntity(Long userId, String entityId) throws DatastoreException, NotFoundException;

	/**
	 * List all of the the ColumnModels.
	 * @param userId
	 * @param prefix
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedColumnModels listColumnModels(Long userId, String prefix, Long limit, Long offset) throws DatastoreException, NotFoundException;
	
	/**
	 * Append rows to a table.
	 * 
	 * @param userId
	 * @param rows
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws IOException 
	 */
	public RowReferenceSet appendRows(Long userId, RowSet rows) throws DatastoreException, NotFoundException, IOException;

	/**
	 * Run a query for a user.
	 * @param userId
	 * @param query
	 * @return
	 * @throws NotFoundException 
	 * @throws TableUnavilableException 
	 * @throws DatastoreException 
	 */
	public RowSet query(Long userId, Query query, boolean isConsistent, boolean countOnly) throws NotFoundException, DatastoreException, TableUnavilableException;
	
}
