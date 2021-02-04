package org.sagebionetworks.repo.manager.dataaccess;

import java.util.List;

import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.SubjectStatus;

public interface RestrictionInformationManager {
	
	/**
	 * Retrieve restriction information for a restrictable object
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	public RestrictionInformationResponse getRestrictionInformation(UserInfo userInfo, RestrictionInformationRequest request);

	/**
	 * Get the restriction information for a batch of entity ids for the given user.
	 * @param userInfo
	 * @param entityIds
	 * @return
	 */
	List<SubjectStatus> getEntityRestrictionInformation(UserInfo userInfo, List<Long> entityIds);

}
