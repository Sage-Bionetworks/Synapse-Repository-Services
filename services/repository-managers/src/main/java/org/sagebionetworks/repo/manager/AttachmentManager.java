package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AttachmentManager {
	
	/**
	 * We are looking for attachments that need previews.
	 * @param entity
	 * @throws InvalidModelException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public void checkAttachmentsForPreviews(Entity entity) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;

	/**
	 * We are looking for user profile attachments that need previews.
	 * @param entity
	 * @throws InvalidModelException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public void checkAttachmentsForPreviews(UserProfile profile) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;
	
	
}
