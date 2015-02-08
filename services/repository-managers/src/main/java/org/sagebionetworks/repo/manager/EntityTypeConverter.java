package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Converts entities from one type to another.
 * @author John
 *
 */
public interface EntityTypeConverter {

	/**
	 * Convert an old entity to its new type.
	 * 
	 * @param user
	 * @param entity
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws UnauthorizedException 
	 */
	public Entity convertOldTypeToNew(UserInfo user, Entity entity) throws UnauthorizedException, DatastoreException, NotFoundException;
	
	/**
	 * Create a file handle for each version of the passed locationable.
	 * @param user
	 * @param entity
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<VersionData> createFileHandleForForEachVersion(UserInfo user, Locationable entity) throws DatastoreException, NotFoundException;
}
