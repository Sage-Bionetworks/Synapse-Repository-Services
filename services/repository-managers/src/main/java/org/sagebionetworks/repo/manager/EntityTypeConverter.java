package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.LocationableTypeConversionResult;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.S3FileHandle;
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
	public LocationableTypeConversionResult convertOldTypeToNew(UserInfo user, String entityId) throws UnauthorizedException, DatastoreException, NotFoundException;
	
	/**
	 * Create a file handle for each version of the passed locationable.
	 * @param user
	 * @param entity
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 * @throws UnsupportedEncodingException 
	 */
	public List<VersionData> createFileHandleForForEachVersion(UserInfo user, Entity entity) throws DatastoreException, NotFoundException, UnsupportedEncodingException, IOException;
	
	/**
	 * Attempt to create a FileHandle given a locationable path.
	 * 
	 * @param path
	 * @return
	 */
	public S3FileHandle createFileHandleFromPathIfExists(Locationable location, LocationData data);
	
}
