package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface DatasetManager {

	/**
	 * Create a new data.
	 * @param userId
	 * @param newEntity
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws UnauthorizedException 
	 */
	Dataset createDataset(String userId, Dataset newEntity) throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException;

}
