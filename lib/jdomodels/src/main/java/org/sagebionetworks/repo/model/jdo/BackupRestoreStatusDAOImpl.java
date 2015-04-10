package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.model.BackupRestoreStatusDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBODaemonStatus;
import org.sagebionetworks.repo.model.dbo.persistence.DBODaemonTerminate;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Implementation of the BackupRestoreStatusDAO. Note: Since a
 * BackupRestoreStatus is used to track the progress of a daemon, and all
 * updates will come from the daemon, the updates must occur in a new
 * transaction separate from the daemon's transactions.
 * 
 * @author jmhill
 * 
 */
public class BackupRestoreStatusDAOImpl implements BackupRestoreStatusDAO {

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	DBOBasicDao dboBasicDao;

	/**
	 * Create a new status object.
	 * 
	 * Note: Requires a new Transaction. Since a BackupRestoreStatus is used to
	 * track the progress of a daemon, and all updates will come from the
	 * daemon, the updates must occur in a new transaction separate from the
	 * daemon's transactions.
	 * 
	 * @return The ID of the newly created status.
	 * @throws DatastoreException
	 */
	@NewWriteTransaction
	@Override
	public String create(BackupRestoreStatus dto) throws DatastoreException {
		// First assign the id
		// Create a new jdo
		DBODaemonStatus jdo = new DBODaemonStatus();
		BackupRestoreStatusUtil.updateJdoFromDto(dto, jdo);
		// Since we will use the ID in the backup file URL we want it to be
		// unique within the domain.
		jdo = dboBasicDao.createNew(jdo);
		DBODaemonTerminate terminateJdo = new DBODaemonTerminate();
		terminateJdo.setOwner(jdo.getId());
		terminateJdo.setForceTerminate(false);
		dboBasicDao.createNew(terminateJdo);
		return jdo.getId().toString();
	}

	/**
	 * Get a status object from its id.
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws DataAccessException
	 */
	@Override
	public BackupRestoreStatus get(String id) throws DatastoreException,
			NotFoundException {
		DBODaemonStatus jdo = getJdo(id);
		return BackupRestoreStatusUtil.createDtoFromJdo(jdo);
	}

	/**
	 * Get the JDO object for this id.
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	private DBODaemonStatus getJdo(String id) throws DatastoreException,
			NotFoundException {
		try {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", id);
			return dboBasicDao.getObjectByPrimaryKey(DBODaemonStatus.class, params);
		} catch (NotFoundException e) {
			throw new NotFoundException(
					"Cannot find a BackupRestoreStatus with ID: " + id);
		}
	}

	/**
	 * Update a status object. Concurrency is not an issue here since only the
	 * daemon that created it will update it.
	 * 
	 * Note: Requires a new Transaction. Since a BackupRestoreStatus is used to
	 * track the progress of a daemon, and all updates will come from the
	 * daemon, the updates must occur in a new transaction separate from the
	 * daemon's transactions.
	 * 
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * 
	 */
	@NewWriteTransaction
	@Override
	public void update(BackupRestoreStatus dto) throws DatastoreException,
			NotFoundException {
		if (dto == null)
			throw new IllegalArgumentException("Status cannot be null");
		if (dto.getId() == null)
			throw new IllegalArgumentException("Status.id cannot be null");
		DBODaemonStatus jdo = getJdo(dto.getId());
		BackupRestoreStatusUtil.updateJdoFromDto(dto, jdo);
		dboBasicDao.update(jdo);
	}

	@NewWriteTransaction
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", id);
		dboBasicDao.deleteObjectByPrimaryKey(DBODaemonStatus.class, params);
	}

	private DBODaemonTerminate getJobTerminate(String id)
			throws DataAccessException, DatastoreException, NotFoundException {
		try {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("owner", id);
			return dboBasicDao.getObjectByPrimaryKey(DBODaemonTerminate.class, params);
		} catch (NotFoundException e) {
			throw new NotFoundException(
					"Cannot find a BackupRestoreStatus with ID: " + id);
		}

	}

	@Override
	public boolean shouldJobTerminate(String id) throws DatastoreException, NotFoundException {
		DBODaemonTerminate terminateJdo = getJobTerminate(id);
		return terminateJdo.getForceTerminate();
	}

	/**
	 * Note: Requires a new Transaction. Value changes will occur through web services
	 * while the value will be checked from the backup daemon.
	 */
	@NewWriteTransaction
	@Override
	public void setForceTermination(String id, boolean terminate)
			throws DatastoreException, NotFoundException {
		DBODaemonTerminate terminateJdo = getJobTerminate(id);
		terminateJdo.setForceTerminate(terminate);
		dboBasicDao.update(terminateJdo);
	}
}
