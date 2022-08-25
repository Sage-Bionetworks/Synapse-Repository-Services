package org.sagebionetworks.drs.services;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
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
    DrsObject getDrsObject(Long userId, String id, Boolean expand)
            throws NotFoundException, DatastoreException, UnauthorizedException,
            IllegalArgumentException, UnsupportedOperationException;
}
