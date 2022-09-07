package org.sagebionetworks.repo.manager.drs;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.drs.AccessUrl;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Manager layer to retrieve the drs information
 */
public interface DrsManager {

    /**
     * Returns the service information for drs services.
     *
     * @return The {@link ServiceInformation} containing the information
     */
    ServiceInformation getServiceInformation();

    /**
     * Returns the drs object, which can be a blob or bundle.
     *
     * @return The {@link DrsObject} containing the information
     */
    DrsObject getDrsObject(Long userId, String id, boolean expand) throws NotFoundException, DatastoreException,
            UnauthorizedException, IllegalArgumentException, UnsupportedOperationException;

    /**
     * Returns the presigned url, which can be used to download the file
     *
     * @return The {@link AccessUrl} containing the presigned url
     */
    AccessUrl getAccessUrl(Long userId, String drsObjectId, String accessId) throws NotFoundException, DatastoreException,
            UnauthorizedException, IllegalArgumentException;
}
