package org.sagebionetworks.repo.manager.drs;

import org.sagebionetworks.repo.model.drs.ServiceInformation;

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
}
