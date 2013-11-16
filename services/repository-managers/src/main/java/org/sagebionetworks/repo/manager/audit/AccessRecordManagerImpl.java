package org.sagebionetworks.repo.manager.audit;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.audit.dao.AccessRecordDAO;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * S3 implementation of the AccessRecordManager.
 * 
 * @author jmhill
 *
 */
public class AccessRecordManagerImpl implements AccessRecordManager {

	@Autowired
	private AccessRecordDAO accessRecordDAO;

	@Override
	public String saveBatch(List<AccessRecord> batch) throws IOException {
		// Save the batch to the dao
		String key = accessRecordDAO.saveBatch(batch, true);
		// Send a message that this key exits
		// Return the key
		return key;
	}

	@Override
	public List<AccessRecord> getBatch(String key) throws IOException {
		// Let the dao get it.
		return accessRecordDAO.getBatch(key);
	}
}
