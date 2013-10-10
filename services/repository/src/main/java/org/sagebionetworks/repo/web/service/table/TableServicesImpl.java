package org.sagebionetworks.repo.web.service.table;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation of the TableServices.
 * 
 * @author John
 *
 */
public class TableServicesImpl implements TableServices {
	
	@Autowired
	UserManager userManager;
	@Autowired
	ColumnModelManager columnModelManager;

	@Override
	public ColumnModel createColumnModel(String userId, ColumnModel columnModel) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return columnModelManager.createColumnModel(user, columnModel);
	}

	@Override
	public ColumnModel getColumnModel(String userId, String columnId) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return columnModelManager.getColumnModel(user, columnId);
	}

}
