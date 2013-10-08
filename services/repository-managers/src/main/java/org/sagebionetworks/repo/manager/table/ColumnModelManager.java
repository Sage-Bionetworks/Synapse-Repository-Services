package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;

public interface ColumnModelManager {

	/**
	 * List ColumnModels that have a name starting with the given prefix.
	 * @param user
	 * @param namePrefix  If null all columns will be listed, otherwise only columns with a name starting with this prefix will be returned.
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedResults<ColumnModel> listColumnModels(UserInfo user, String namePrefix, long limit, long offset);
	
	/**
	 * Create a new immutable ColumnModel object.
	 * 
	 * @param user
	 * @param columnModel
	 * @return
	 */
	public ColumnModel createColumnModel(UserInfo user, ColumnModel columnModel);
	
	
}
