package org.sagebionetworks.drs.services;

import org.sagebionetworks.repo.model.drs.ServiceInformation;

/**
 * Service that is exposed to the clients for drs.
 */
public interface DrsService {
    /**
     * @return the drs service information
     */
    ServiceInformation getServiceInformation();
}
