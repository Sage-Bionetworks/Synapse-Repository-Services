package org.sagebionetworks.repo.manager;

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
	public RestrictionInformationResponse getRestrictionInformation(UserInfo userInfo, RestrictionInformationRequest request);

}
