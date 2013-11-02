package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.springframework.beans.factory.annotation.Autowired;

public class TableRowManagerImpl implements TableRowManager {

	@Autowired
	FileHandleDao fileHandleDao;
	

	@Override
	public RowReferenceSet storeChangeSet(UserInfo user, String tableId, List<ColumnModel> models, RowSet delta) {
		// TODO Auto-generated method stub
		return null;
	}
	

}
