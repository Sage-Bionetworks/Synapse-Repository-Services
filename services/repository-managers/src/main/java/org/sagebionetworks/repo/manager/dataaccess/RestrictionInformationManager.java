package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.UserInfo;

public interface RestrictionInformationManager {
	
	/**
	 * Retrieve restriction information for a restrictable object
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	RestrictionInformationResponse getRestrictionInformation(UserInfo userInfo, RestrictionInformationRequest request);

}
