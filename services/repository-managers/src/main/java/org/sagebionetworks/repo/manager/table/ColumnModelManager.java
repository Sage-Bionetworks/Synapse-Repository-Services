package org.sagebionetworks.repo.manager.table;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;

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
	 * Get a list of column models for the given list of IDs.
	 * @param ids
	 * @return The result order will match the order of the requested IDs.
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<ColumnModel> getAndValidateColumnModels(List<String> ids) throws DatastoreException, NotFoundException;
	
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
	 * Bind a set of columns to the default version (version = null) of the given object.
	 * @param user
	 * @param columnIds
	 * @param idAndVersion
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public List<ColumnModel> bindColumnsToDefaultVersionOfObject(List<String> columnIds, String objectId) throws DatastoreException, NotFoundException;
	
	/**
	 * Bind a set of columns to the given version of the given object
	 * @param user
	 * @param columnIds
	 * @param idAndVersion
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public List<ColumnModel> bindColumnsToVersionOfObject(List<String> columnIds, IdAndVersion idAndVersion) throws DatastoreException, NotFoundException;
	
	/**
	 * Bind the current schema of an object to a targeted version of that object.
	 * @param idAndVersion
	 * @return
	 */
	public List<ColumnModel> bindDefaultColumnsToObjectVersion(IdAndVersion idAndVersion);
	
	/**
	 * Remove all column bindings for an object
	 * 
	 * @param objectId
	 */
	public void unbindAllColumnsAndOwnerFromObject(String objectId);
	
	/**
	 * Clear all data for tests.
	 * @param user
	 */
	public boolean truncateAllColumnData(UserInfo user);
	
	/**
	 * Build a column map for a table using the provided select columns.
	 * @param user
	 * @param tableId
	 * @param selectColumns
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public List<ColumnModel> getCurrentColumns(UserInfo user, String tableId, List<SelectColumn> selectColumns) throws DatastoreException, NotFoundException;

	/**
	 * Validate the given schema is under the max size.
	 * 
	 * @param columnIds
	 */
	List<ColumnModel> validateSchemaSize(List<String> columnIds);
	
	/**
	 * Calculate the new schema if the passed changes are applied to the current schema of the table
	 * and validate the changes.
	 * @param changes
	 * @param orderedColumnIds 
	 * @return
	 */
	List<String> calculateNewSchemaIdsAndValidate(String tableId, List<ColumnChange> changes, List<String> orderedColumnIds);

	/**
	 * Get the details of a schema change.
	 * 
	 * @param changes
	 * @return
	 */
	public List<ColumnChangeDetails> getColumnChangeDetails(
			List<ColumnChange> changes);

	/**
	 * Get the columnIds for a table.
	 * @param idAndVersion
	 * @return
	 */
	public List<String> getColumnIdForTable(IdAndVersion idAndVersion);

	/**
	 * Get the column models bound to this object.
	 * @param idAndVersion
	 * @return
	 */
	public List<ColumnModel> getColumnModelsForObject(IdAndVersion idAndVersion);
}

