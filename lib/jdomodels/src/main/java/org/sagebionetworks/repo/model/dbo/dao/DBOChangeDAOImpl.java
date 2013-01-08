package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_CHANGE_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHANGES;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOChange;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessageUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
 * The implementation of the change DBOChangeDAO
 * @author John
 *
 */
public class DBOChangeDAOImpl implements DBOChangeDAO {

	private static final String SQL_SELECT_ALL_GREATER_THAN_OR_EQUAL_TO_CHANGE_NUMBER_FILTER_BY_OBJECT_TYPE = "SELECT * FROM "+TABLE_CHANGES+" WHERE "+COL_CHANGES_CHANGE_NUM+" >= ? AND "+COL_CHANGES_OBJECT_TYPE+" = ? ORDER BY "+COL_CHANGES_CHANGE_NUM+" ASC LIMIT ?";

	private static final String SQL_SELECT_ALL_GREATER_THAN_OR_EQUAL_TO_CHANGE_NUMBER = "SELECT * FROM "+TABLE_CHANGES+" WHERE "+COL_CHANGES_CHANGE_NUM+" >= ? ORDER BY "+COL_CHANGES_CHANGE_NUM+" ASC LIMIT ?";

	private static final String SQL_SELECT_MAX_CHANGE_NUMBER = "SELECT MAX("+COL_CHANGES_CHANGE_NUM+") FROM "+TABLE_CHANGES;

	private static final String SQL_DELETE_BY_OBJECT_ID = "DELETE FROM "+TABLE_CHANGES+" WHERE "+COL_CHANGES_OBJECT_ID+" = ?";

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(10);

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ChangeMessage replaceChange(ChangeMessage change) {
		if(change == null) throw new IllegalArgumentException("DBOChange cannot be null");
		if(change.getObjectId() == null) throw new IllegalArgumentException("change.getObjectId() cannot be null");
		if(change.getChangeType() == null) throw new IllegalArgumentException("change.getChangeTypeEnum() cannot be null");
		if(change.getObjectType() == null) throw new IllegalArgumentException("change.getObjectTypeEnum() cannot be null");
		if(ChangeType.CREATE == change.getChangeType() && change.getObjectEtag() == null) throw new IllegalArgumentException("Etag cannot be null for ChangeType: "+change.getChangeType());
		if(ChangeType.UPDATE == change.getChangeType() && change.getObjectEtag() == null) throw new IllegalArgumentException("Etag cannot be null for ChangeType: "+change.getChangeType());
		final DBOChange dbo = ChangeMessageUtils.createDBO(change);
		// First delete the change.
		deleteChange(dbo.getObjectId());
		// Clear the time stamp so that we get a new one automatically.
		// Note: Mysql TIMESTAMP only keeps seconds (not MS) so for consistency we only write second accuracy.
		// We are using (System.currentTimeMillis()/1000)*1000; to convert all MS to zeros.
		long nowMs = (System.currentTimeMillis()/1000)*1000;
		dbo.setTimeStamp(new Timestamp(nowMs));
		dbo.setChangeNumber(null);
		// Now insert the row with the current value
		// Implement with retries to work around deadlocks
		Callable<DBOChange> task = new Callable<DBOChange>() {
			@Override
			public DBOChange call() throws Exception {
				return basicDao.createNew(dbo);
			}
		};
		DBOChange newDbo = this.retryForDeadlock(task, 3);
		return ChangeMessageUtils.createDTO(newDbo);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<ChangeMessage> replaceChange(List<ChangeMessage> batchDTO) {
		if(batchDTO == null) throw new IllegalArgumentException("Batch cannot be null");
		// To prevent deadlock we sort by object id to guarantee a consistent update order.
		batchDTO = ChangeMessageUtils.sortByObjectId(batchDTO);
		// Send each replace them in order.
		List<ChangeMessage> resutls = new ArrayList<ChangeMessage>();
		for(ChangeMessage change: batchDTO){
			resutls.add(replaceChange(change));
		}
		return resutls;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteChange(final Long objectId) {
		if(objectId == null) throw new IllegalArgumentException("ObjectId cannot be null");
		// First remove the row for this object.
		// Implement with retries to work around deadlocks
		Callable<Boolean> task = new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				simpleJdbcTemplate.update(SQL_DELETE_BY_OBJECT_ID, objectId);
				return Boolean.TRUE;
			}
		};
		this.retryForDeadlock(task, 3);
	}

	@Override
	public long getCurrentChangeNumber() {
		return simpleJdbcTemplate.queryForLong(SQL_SELECT_MAX_CHANGE_NUMBER);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAllChanges() {
		simpleJdbcTemplate.update("TRUNCATE TABLE "+TABLE_CHANGES);
	}

	@Override
	public List<ChangeMessage> listChanges(long greaterOrEqualChangeNumber, ObjectType type, long limit) {
		if(limit < 0) throw new IllegalArgumentException("Limit cannot be less than zero");
		List<DBOChange> dboList = null;
		if(type == null){
			// There is no type filter.
			dboList = simpleJdbcTemplate.query(SQL_SELECT_ALL_GREATER_THAN_OR_EQUAL_TO_CHANGE_NUMBER, new DBOChange().getTableMapping(), greaterOrEqualChangeNumber, limit);
		}else{
			// Filter by object type.
			dboList = simpleJdbcTemplate.query(SQL_SELECT_ALL_GREATER_THAN_OR_EQUAL_TO_CHANGE_NUMBER_FILTER_BY_OBJECT_TYPE, new DBOChange().getTableMapping(), greaterOrEqualChangeNumber, type.name(), limit);
		}
		return ChangeMessageUtils.createDTOList(dboList);
	}

	// Retry with exponentially increasing delays
	private <T> T retryForDeadlock(final Callable<T> task, int numOfRetries) {
		// Implement with retries to work around deadlocks
		long delay = 0L;
		while (numOfRetries > 0) {
			try {
				Future<T> future = retryExecutor.schedule(task, delay, TimeUnit.SECONDS);
				return future.get();
			} catch (DeadlockLoserDataAccessException e) {
				if (delay == 0L) {
					delay = 1L;
				} else {
					delay = delay << 1L;
				}
				numOfRetries--;
			} catch (InterruptedException e) {
				if (delay == 0L) {
					delay = 1L;
				} else {
					delay = delay << 1L;
				}
				numOfRetries--;
			} catch (ExecutionException e) {
				throw new DatastoreException(e.getCause());
			}
		}
		throw new DatastoreException("Retry for deadlock failed after retrying for " + numOfRetries + " times.");
	}
}
