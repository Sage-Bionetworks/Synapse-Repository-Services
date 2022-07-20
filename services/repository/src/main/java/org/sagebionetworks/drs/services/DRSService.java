package org.sagebionetworks.drs.services;

import org.sagebionetworks.repo.model.drs.ServiceInformation;

public interface DRSService {
    /**
     *
     * @return the DRS service information
     */
    ServiceInformation getServiceInformation();
}
