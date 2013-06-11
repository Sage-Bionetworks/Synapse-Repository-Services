package org.sagebionetworks.repo.manager.file;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.UploadDaemonStatusDao;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Very simple map back stub of the UploadDaemonStatusDao.
 * @author John
 *
 */
public class StubUploadDeamonStatusDao implements UploadDaemonStatusDao {
	
	private static long sequence = 0;
	private Map<String, UploadDaemonStatus> map = new HashMap<String, UploadDaemonStatus>();

	@Override
	public UploadDaemonStatus create(UploadDaemonStatus status) {
		status.setDaemonId(""+sequence++);
		return status;
	}

	@Override
	public UploadDaemonStatus get(String id) throws DatastoreException,	NotFoundException {
		return map.get(id);
	}

	@Override
	public void delete(String id) {
		this.map.remove(id);
	}

	@Override
	public boolean update(UploadDaemonStatus status) {
		this.map.put(status.getDaemonId(), status);
		return true;
	}

}
