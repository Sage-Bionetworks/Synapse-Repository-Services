package org.sagebionetworks.repo.manager.table;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.PaginatedIds;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ColumnModelManager {

	/**
	 * List ColumnModels that have a name starting with the given prefix.
	 * @param user
	 * @param namePrefix  If null all columns will be listed, otherwise only columns with a name starting with this prefix will be returned.
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedColumnModels listColumnModels(UserInfo user, String namePrefix, long limit, long offset);
	
	/**
	 * Create a new immutable ColumnModel object.
	 * 
	 * @param user
	 * @param columnModel
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws UnauthorizedException 
	 */
	public ColumnModel createColumnModel(UserInfo user, ColumnModel columnModel) throws UnauthorizedException, DatastoreException, NotFoundException;

	/**
	 * Create new immutable ColumnModel objects
	 * 
	 * @param user
	 * @param columnModels
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public List<ColumnModel> createColumnModels(UserInfo user, List<ColumnModel> columnModels) throws DatastoreException, NotFoundException;
	
	/**
	 * Get a list of column models for the given list of IDs
	 * @param ids
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<ColumnModel> getColumnModel(UserInfo user, List<String> ids, boolean keepOrder) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the columns models bound to a Table.
	 * @param user
	 * @param tableId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<ColumnModel> getColumnModelsForTable(UserInfo user, String tableId) throws DatastoreException, NotFoundException;
	
	/**
	 * Get a single ColumnModel
	 * @param user
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public ColumnModel getColumnModel(UserInfo user, String columnId) throws DatastoreException, NotFoundException;
	
	/**
	 * Bind a set of columns to an object.
	 * @param user
	 * @param columnIds
	 * @param objectId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public boolean bindColumnToObject(UserInfo user, List<String> columnIds, String objectId, boolean isNew) throws DatastoreException, NotFoundException;
	
	/**
	 * Remove all column bindings for an object
	 * 
	 * @param objectId
	 */
	public void unbindAllColumnsAndOwnerFromObject(String objectId);

	/**
	 * List all of the objects that are bound to the given column IDs.
	 * 
	 * @param user
	 * @param columnIds
	 * @param currentOnly
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedIds listObjectsBoundToColumn(UserInfo user, Set<String> columnIds, boolean currentOnly, long limit, long offset);
	
	/**
	 * Clear all data for tests.
	 * @param user
	 */
	public boolean truncateAllColumnData(UserInfo user);
}

