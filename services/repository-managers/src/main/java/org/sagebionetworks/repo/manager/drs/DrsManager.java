package org.sagebionetworks.repo.manager.drs;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
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
}
