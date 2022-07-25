package org.sagebionetworks.repo.manager.drs;

import org.sagebionetworks.repo.model.drs.ServiceInformation;

/**
 * Manager layer to retrieve the DRS information
 */
public interface DRSManager {

    /**
     * Returns the service information for DRS services.
     *
     * @return The {@link ServiceInformation} containing the information
     */
    ServiceInformation getServiceInformation();
}
