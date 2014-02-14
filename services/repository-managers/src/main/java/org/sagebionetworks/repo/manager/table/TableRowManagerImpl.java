package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.semaphore.ExclusiveOrSharedSemaphoreRunner;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class TableRowManagerImpl implements TableRowManager {
	
	@Autowired
	AuthorizationManager authorizationManager;

	@Autowired
	TableRowTruthDAO tableRowTruthDao;
	@Autowired
	TableStatusDAO tableStatusDAO;
	@Autowired
	ColumnModelDAO columnModelDAO;
	@Autowired
	ExclusiveOrSharedSemaphoreRunner exclusiveOrSharedSemaphoreRunner;
	

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public RowReferenceSet appendRows(UserInfo user, String tableId, List<ColumnModel> models, RowSet delta) throws DatastoreException, NotFoundException, IOException {
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		if(tableId == null) throw new IllegalArgumentException("TableId cannot be null");
		if(models == null) throw new IllegalArgumentException("Models cannot be null");
		if(delta == null) throw new IllegalArgumentException("RowSet cannot be null");

		// Validate the user has permission to edit the table
		if(!authorizationManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)){
			throw new UnauthorizedException("User does not have permission to update TableEntity: "+tableId);
		}
		// Let the DAO do the rest of the work.
		RowReferenceSet rrs = tableRowTruthDao.appendRowSetToTable(user.getId().toString(), tableId, models, delta);
		// The table has change so we must reset the state.
		tableStatusDAO.resetTableStatusToProcessing(tableId);
		return rrs;
	}

	@Override
	public List<ColumnModel> getColumnModelsForTable(String tableId) throws DatastoreException, NotFoundException {
		return columnModelDAO.getColumnModelsForObject(tableId);
	}

	@Override
	public List<TableRowChange> listRowSetsKeysForTable(String tableId) {
		return tableRowTruthDao.listRowSetsKeysForTable(tableId);
	}

	@Override
	public RowSet getRowSet(String tableId, Long rowVersion) throws IOException, NotFoundException {
		return tableRowTruthDao.getRowSet(tableId, rowVersion);
	}
	@Override
	public <T> T tryRunWithTableExclusiveLock(String tableId, long lockTimeoutMS, Callable<T> runner)
			throws InterruptedException, Exception {
		String key = TableModelUtils.getTableSemaphoreKey(tableId);
		// The semaphore runner does all of the lock work.
		return exclusiveOrSharedSemaphoreRunner.tryRunWithExclusiveLock(key, lockTimeoutMS, runner);
	}

	@Override
	public <T> T tryRunWithTableNonexclusiveLock(String tableId, long lockTimeoutMS, Callable<T> runner)
			throws Exception {
		String key = TableModelUtils.getTableSemaphoreKey(tableId);
		// The semaphore runner does all of the lock work.
		return exclusiveOrSharedSemaphoreRunner.tryRunWithSharedLock(key, lockTimeoutMS, runner);
	}

	@Override
	public TableStatus getTableStatus(String tableId) throws NotFoundException {
		return tableStatusDAO.getTableStatus(tableId);
	}

	@Override
	public void attemptToSetTableStatusToAvailable(String tableId,
			String resetToken) throws ConflictingUpdateException,
			NotFoundException {
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableId, resetToken);
	}

	@Override
	public void attemptToSetTableStatusToFailed(String tableId,
			String resetToken, String errorMessage, String errorDetails)
			throws ConflictingUpdateException, NotFoundException {
		tableStatusDAO.attemptToSetTableStatusToFailed(tableId, resetToken, errorMessage, errorDetails);
	}

	@Override
	public void attemptToUpdateTableProgress(String tableId, String resetToken,
			String progressMessage, Long currentProgress, Long totalProgress)
			throws ConflictingUpdateException, NotFoundException {
		tableStatusDAO.attemptToUpdateTableProgress(tableId, resetToken, progressMessage, currentProgress, totalProgress);
	}
	

}
