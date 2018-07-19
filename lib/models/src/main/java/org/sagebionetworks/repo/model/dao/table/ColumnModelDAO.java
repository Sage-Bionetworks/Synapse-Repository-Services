package org.sagebionetworks.repo.model.dao.table;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * An abstraction for creating and getting column model objects.
 * 
 * @author John
 *
 */
public interface ColumnModelDAO extends ColumnNameProvider {
	
	/**
	 * List all column models filtered by the given name prefix.
	 * @param namePrefix - When provided only columns with a name that starts with prefix will be returned.
	 * @param limit - Pagination parameter to limit the number of columns returned.
	 * @param offset - Pagination parameter that is the index of the first column in the page to be returned.
	 * @return
	 */
	public List<ColumnModel> listColumnModels(String namePrefix, long limit, long offset);
	
	/**
	 * Used for pagination to count the number of rows that meet a query.
	 * @param namePrefix
	 * @return
	 */
	public long listColumnModelsCount(String namePrefix);
	
	/**
	 * Create a new column model.  Column models are immutable and cannot be deleted once they are used.
	 * @param model
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public ColumnModel createColumnModel(ColumnModel model) throws DatastoreException, NotFoundException;
	
	/**
	 * Get a the ColumnModel ID for a given Hash.
	 * 
	 * @param hash
	 * @return
	 */
	public String getColumnForHash(String hash);
	/**
	 * Get a ColumnModel from its id.
	 * 
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public ColumnModel getColumnModel(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * Get a a list of ColumnModel from a list of columnModel ID strings
	 * @param ids
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<ColumnModel> getColumnModel(List<String> ids) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the columns currently bound to an object in the order they were bound.
	 * 
	 * @param tableId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<ColumnModel> getColumnModelsForObject(String tableId) throws DatastoreException;
	
	/**
	 * Get the column IDs for the given Object.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<String> getColumnModelIdsForObject(String tableId);
	
	/**
	 * Delete a column model using its ID.  Note: Only a column model that is not currently in use can be deleted.
	 * 
	 * @param id
	 * @return 
	 */
	public int deleteColumModel(String id);
	
	/**
	 * Unbind all of the columns associated with an object.
	 * @param objectId
	 * @return
	 */
	public int unbindAllColumnsFromObject(String objectId);
	
	/**
	 * Bind a list of ColumnModels to an object. This indicates that the passed object now depends on this passed column.
	 * Once an object is bound to a column it cannot be unbound.  A ColumnModel can no longer be deleted once bound to an object.
	 * The order of the list is maintained for the current column models of a table.
	 * @param columnId The ID of the column to bind.
	 * @param objectId The ID of the object to bind.
	 * 
	 * @return True if the this object was not already bound to this object.
	 * @throws NotFoundException 
	 */
	public int bindColumnToObject(List<ColumnModel> columnModels, String objectId) throws NotFoundException;
	
	/**
	 * List all objects that are bound to a set of column IDs.
	 * 
	 * @param columnIds The list of column IDs.
	 * @param currentOnly When true, only objects that are currently using the IDs will be returned.  
	 * @param limit - Pagination parameter to limit the number of columns returned.
	 * @param offset - Pagination parameter that is the index of the first column in the page to be returned.
	 * @return
	 */
	public List<String> listObjectsBoundToColumn(Set<String> columnIds, boolean currentOnly, long limit, long offest);
	
	/**
	 * Used for pagination to determine the total number of results for this query.
	 * @return
	 */
	public long listObjectsBoundToColumnCount(Set<String> columnIds, boolean currentOnly);
	
	/**
	 * Select for update on an owner object.
	 * 
	 * @param objectId
	 * @return The current etag set to the owner.
	 */
	public String lockOnOwner(String objectId);
	
	/**
	 * Delete the owner object.
	 * 
	 * @param objectId
	 */
	public void deleteOwner(String objectId);

	/**
	 * This should only be called by tests.
	 * 
	 */
	public boolean truncateAllColumnData();

	/**
	 * Get the column Ids for a given table.
	 * @param id
	 * @return
	 */
	public List<String> getColumnIdsForObject(String id);
	
}
