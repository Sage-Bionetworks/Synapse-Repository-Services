package org.sagebionetworks.repo.service.drs;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.drs.AccessUrl;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Service that is exposed to the clients for drs.
 */
public interface DrsService {
    /**
     * @return the drs service information
     */
    ServiceInformation getServiceInformation();

    /**
     * Get a drs object
     *
     * @param userId
     * @param id
     * @param expand expand value true means bundle contains bundle, Synapse does not support nesting bundle.
     *               For file expand parameter is ignored
     * @return
     * @throws NotFoundException
     * @throws DatastoreException
     * @throws UnauthorizedException
     * @throws IllegalArgumentException
     */
    DrsObject getDrsObject(Long userId, String id, boolean expand)
            throws NotFoundException, DatastoreException, UnauthorizedException,
            IllegalArgumentException, UnsupportedOperationException;

    /**
     * Get the presigned url to download the file.
     *
     * @param userId
     * @param drsObjectId
     * @param accessId
     *
     * @return
     * @throws NotFoundException
     * @throws DatastoreException
     * @throws UnauthorizedException
     * @throws IllegalArgumentException
     */
    AccessUrl getAccessUrl(Long userId, String drsObjectId, String accessId) throws NotFoundException, DatastoreException,
            UnauthorizedException, IllegalArgumentException;
}
