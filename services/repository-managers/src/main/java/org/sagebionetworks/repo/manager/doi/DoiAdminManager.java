package org.sagebionetworks.repo.manager.doi;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface DoiAdminManager {

	/**
	 * Clears DOI data.
	 */
	void clear(String userName) throws NotFoundException, UnauthorizedException, DatastoreException;
}
