package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.model.RestrictionInformationBatchRequest;
import org.sagebionetworks.repo.model.RestrictionInformationBatchResponse;
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

	/**
	 * Retrieve a batch of restriction information for a list of restrictable objects (of the same type)
	 * @param userInfo
	 * @param request
	 * @return
	 */
	RestrictionInformationBatchResponse getRestrictionInformationBatch(UserInfo userInfo, RestrictionInformationBatchRequest request);

}
