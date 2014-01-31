package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class TableRowManagerImpl implements TableRowManager {
	
	@Autowired
	AuthorizationManager authorizationManager;

	@Autowired
	TableRowTruthDAO tableRowTruthDao;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public RowReferenceSet appendRows(UserInfo user, Long tableId, List<ColumnModel> models, RowSet delta) throws DatastoreException, NotFoundException, IOException {
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		if(tableId == null) throw new IllegalArgumentException("TableId cannot be null");
		if(models == null) throw new IllegalArgumentException("Models cannot be null");
		if(delta == null) throw new IllegalArgumentException("RowSet cannot be null");

		// Validate the user has permission to edit the table
		if(!authorizationManager.canAccess(user, tableId.toString(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)){
			throw new UnauthorizedException("User does not have permission to update TableEntity: "+tableId);
		}
		// Let the DAO do the rest of the work.
		return tableRowTruthDao.appendRowSetToTable(user.getId().toString(), tableId, models, delta);
	}
	

}
