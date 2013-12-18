package org.sagebionetworks.repo.manager.doi;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface DoiAdminManager {

	/**
	 * Clears DOI data.
	 */
	void clear(Long userId) throws NotFoundException, UnauthorizedException, DatastoreException;
}
