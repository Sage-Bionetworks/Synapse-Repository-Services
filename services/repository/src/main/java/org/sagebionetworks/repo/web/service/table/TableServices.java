package org.sagebionetworks.repo.web.service.table;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewType;
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
	 * Create new ColumnModels
	 * 
	 * @param userId
	 * @param list
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public List<ColumnModel> createColumnModels(Long userId, List<ColumnModel> columnModels) throws DatastoreException, NotFoundException;

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
	 * Delete rows in a table.
	 * 
	 * @param userId
	 * @param rowsToDelete
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws IOException
	 */
	public RowReferenceSet deleteRows(Long userId, RowSelection rowsToDelete) throws DatastoreException, NotFoundException, IOException;

	/**
	 * Get the file handles
	 * 
	 * @param userId
	 * @param fileHandlesToFind
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public TableFileHandleResults getFileHandles(Long userId, RowReferenceSet fileHandlesToFind) throws IOException, NotFoundException;

	/**
	 * get file redirect urls for the rows from the column
	 * 
	 * @param userId
	 * @param rowRef
	 * @return
	 * @throws NotFoundException
	 * @throws IOException
	 */
	public String getFileRedirectURL(Long userId, String tableId, RowReference rowRef, String columnId) throws IOException, NotFoundException;

	/**
	 * get file preview redirect urls for the rows from the column
	 * 
	 * @param userId
	 * @param rowRef
	 * @return
	 * @throws NotFoundException
	 * @throws IOException
	 */
	public String getFilePreviewRedirectURL(Long userId, String tableId, RowReference rowRef, String columnId) throws IOException,
			NotFoundException;

	/**
	 * Get the max number of rows allowed for a page (get, post, or query) for the given column models.
	 * @param models
	 * @return
	 */
	public Long getMaxRowsPerPage(List<ColumnModel> models);
	
	
	/**
	 * Get the default columns for a view of the given type.
	 * @param viewTypeMask 
	 * @return
	 */
	public List<ColumnModel> getDefaultViewColumnsForType(Long viewTypeMask);

	/**
	 * Get the possible ColumnModel definitions based on annotations for a given view.
	 * @param viewId The id of the view to fetch annotation definitions for.
	 * @param nextPageToken Optional: Controls pagination.
	 * @return A ColumnModel for each distinct annotation for the given scope.
	 */
	ColumnModelPage getPossibleColumnModelsForView(String viewId,
			String nextPageToken);

	/**
	 * Get the possible ColumnModel definitions based on annotation within a given scope.
	 * @param scope Defined as the list of container ids for a view.
	 * @param nextPageToken Optional: Controls pagination.
	 * @return A ColumnModel for each distinct annotation for the given scope.
	 */
	ColumnModelPage getPossibleColumnModelsForScopeIds(ViewScope scope,
			String nextPageToken);
}
