package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface EntityValidator<T extends Entity> extends EntityProvider<T> {
	
	/**
	 * Validate an entity before it is created or updated.
	 * @param entity
	 * @param event
	 * @throws InvalidModelException
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public void validateEntity(T entity, EntityEvent event) throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException;

}
