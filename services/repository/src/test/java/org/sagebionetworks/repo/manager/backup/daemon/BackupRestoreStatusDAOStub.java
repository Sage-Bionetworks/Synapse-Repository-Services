package org.sagebionetworks.repo.manager.backup.daemon;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.BackupRestoreStatus;
import org.sagebionetworks.repo.model.BackupRestoreStatusDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.jdo.BackupRestoreStatusUtil;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * A simple stub implementation of the BackupRestoreStatusDAO for testing.
 * It allows us to drive the backup Daemon.
 * 
 * @author John
 *
 */
public class BackupRestoreStatusDAOStub implements BackupRestoreStatusDAO{
	
	private Map<String, BackupRestoreStatus> statusMap = Collections.synchronizedMap(new HashMap<String, BackupRestoreStatus>());
	private Map<String, Boolean> terminationMap = Collections.synchronizedMap(new HashMap<String, Boolean>());
	private long sequence = 0;

	@Override
	public String create(BackupRestoreStatus status) throws DatastoreException {
		// Make a clone
		BackupRestoreStatus clone = BackupRestoreStatusUtil.cloneStatus(status);
		String id = new Long(sequence++).toString();
		clone.setId(id);
		statusMap.put(id, clone);
		terminationMap.put(id, false);
		return clone.getId();
	}


	@Override
	public BackupRestoreStatus get(String id) throws DatastoreException,
			NotFoundException {
		BackupRestoreStatus status = statusMap.get(id);
		if(status == null) throw new NotFoundException();
		if(status.getId() == null) throw new IllegalArgumentException("Status.id cannot be null");
		return status;
	}

	@Override
	public void update(BackupRestoreStatus status) throws DatastoreException, NotFoundException {
		if(status == null) throw new IllegalArgumentException("Status cannot be null");
		if(status.getId() == null) throw new IllegalArgumentException("Status.id cannot be null");
		BackupRestoreStatus clone = BackupRestoreStatusUtil.cloneStatus(status);
		if(clone.getId() == null) throw new IllegalArgumentException("Status.id cannot be null");
		statusMap.put(status.getId(), clone);
	}

	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		statusMap.remove(id);
		terminationMap.remove(id);
	}

	@Override
	public void setForceTermination(String id, boolean terminate)
			throws DatastoreException, NotFoundException {
		terminationMap.put(id, terminate);
	}

	@Override
	public boolean shouldJobTerminate(String id) throws DatastoreException,
			NotFoundException {
		return terminationMap.get(id);
	}

}
