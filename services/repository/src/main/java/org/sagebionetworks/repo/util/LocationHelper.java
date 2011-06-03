package org.sagebionetworks.repo.util;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface LocationHelper {

	String getS3Url(String userId, String path) throws DatastoreException, NotFoundException;
	String createS3Url(String userId, String path, String md5) throws DatastoreException, NotFoundException;

}
