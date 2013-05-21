package org.sagebionetworks.repo.manager.dynamo;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface DynamoAdminManager {

	/**
	 * Clears all the items in the specified table.
	 */
	void clear(String userName, String tableName, String hashKeyName, String rangeKeyName)
			throws UnauthorizedException, DatastoreException, NotFoundException;
}
