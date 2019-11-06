package org.sagebionetworks.repo.model.dao.table;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
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
	public List<ColumnModel> getColumnModels(List<String> ids) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the columns currently bound to an object in the order they were bound.
	 * 
	 * @param tableId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<ColumnModel> getColumnModelsForObject(IdAndVersion idAndVersion) throws DatastoreException;
	
	/**
	 * Get the column IDs for the given Object.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<String> getColumnModelIdsForObject(IdAndVersion idAndVersion);
	
	/**
	 * Delete a column model using its ID.  Note: Only a column model that is not currently in use can be deleted.
	 * 
	 * @param id
	 * @return 
	 */
	public int deleteColumModel(String id);
	
	/**
	 * Bind the passed columns to the given object and version.
	 * @param columnId The ID of the column to bind.
	 * @param objectIdAndVersion The ID of the object to bind, with an optional version.
	 * 
	 * @throws NotFoundException 
	 */
	public void bindColumnToObject(List<ColumnModel> columnModels, IdAndVersion objectIdAndVersion) throws NotFoundException;
	
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
	
}
