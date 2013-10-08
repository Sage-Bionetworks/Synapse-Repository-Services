package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation of the ColumnModelManager.
 * 
 * @author John
 *
 */
public class ColumnModelManagerImpl implements ColumnModelManager {

	@Autowired
	ColumnModelDAO columnModelDao;
	
	@Autowired
	AuthorizationManager authorizationManager;

	@Override
	public PaginatedResults<ColumnModel> listColumnModels(UserInfo user, String namePrefix, long limit, long offset) {
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		// First call is to list the columns.
		List<ColumnModel> list = columnModelDao.listColumnModels(namePrefix, limit, offset);
		// second to get the total number of results with this prefix
		long totalNumberOfResults = columnModelDao.listColumnModelsCount(namePrefix);
		return new PaginatedResults<ColumnModel>(list, totalNumberOfResults);
	}

	@Override
	public ColumnModel createColumnModel(UserInfo user, ColumnModel columnModel) {
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		return null;
	}
	
	
}
