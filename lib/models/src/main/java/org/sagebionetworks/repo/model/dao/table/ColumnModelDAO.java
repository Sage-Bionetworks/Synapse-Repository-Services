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
public interface ColumnModelDAO {
	
	/**
	 * List all column models filtered by the given name prefix.
	 * @param namePrefix - When provided only columns with a name that starts with prefix will be returned.
	 * @param limit - Pagination parameter to limit the number of columns returned.
	 * @param offset - Pagination parameter that is the index of the first column in the page to be returned.
	 * @return
	 */
	public List<ColumnModel> listColumnModels(String namePrefix, long limit, long offset);
	
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
	 * Delete a column model using its ID.  Note: Only a column model that is not currently in use can be deleted.
	 * 
	 * @param id
	 */
	public void delete(String id);
	
	/**
	 * Bind a set of ColumnModels to an object. This indicates that the passed object now depends on this passed column.
	 * Once an object is bound to a column it cannot be unbound.  A ColumnModel can no longer be deleted once bound to an object.
	 * @param columnId The ID of the column to bind.
	 * @param objectId The ID of the object to bind.
	 * 
	 * @return True if the this object was not already bound to this object.
	 */
	public int bindColumnToObject(Set<String> columnIds, String objectId);
	
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
	 * This should only be called by tests.
	 * 
	 */
	public void truncateBoundColumns();
	

}
