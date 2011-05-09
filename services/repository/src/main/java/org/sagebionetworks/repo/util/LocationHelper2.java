package org.sagebionetworks.repo.util;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;

public interface LocationHelper2 {

	String getS3Url(String userId, String path) throws DatastoreException, UnauthorizedException;

}
