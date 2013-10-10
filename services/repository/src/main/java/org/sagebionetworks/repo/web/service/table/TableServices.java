package org.sagebionetworks.repo.web.service.table;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.table.ColumnModel;
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
	public ColumnModel createColumnModel(String userId, ColumnModel model) throws DatastoreException, NotFoundException;

	/**
	 * Get a ColumnModel for a given ID
	 * 
	 * @param userId
	 * @param columnId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public ColumnModel getColumnModel(String userId, String columnId) throws DatastoreException, NotFoundException;
	
	
}
