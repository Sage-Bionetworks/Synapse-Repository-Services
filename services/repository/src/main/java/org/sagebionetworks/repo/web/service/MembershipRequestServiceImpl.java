/**
 * 
 */
package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.MembershipRequestManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author brucehoff
 *
 */
public class MembershipRequestServiceImpl implements MembershipRequestService {

	@Autowired
	private MembershipRequestManager membershipRequestManager;
	@Autowired
	private UserManager userManager;
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipRequestService#create(java.lang.String, org.sagebionetworks.repo.model.MembershipRqstSubmission)
	 */
	@Override
	public MembershipRqstSubmission create(String userId,
			MembershipRqstSubmission dto) throws UnauthorizedException,
			InvalidModelException, DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return membershipRequestManager.create(userInfo, dto);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipRequestService#getOpenRequests(java.lang.String, java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipRequest> getOpenRequests(String userId,
			String requestorId, String teamId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (requestorId==null) {
			return membershipRequestManager.getOpenByTeamInRange(userInfo, teamId, limit, offset);
		} else {
			return membershipRequestManager.getOpenByTeamAndRequestorInRange(userInfo, teamId, requestorId, limit, offset);
		}
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipRequestService#get(java.lang.String, java.lang.String)
	 */
	@Override
	public MembershipRqstSubmission get(String userId, String dtoId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return membershipRequestManager.get(userInfo, dtoId);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipRequestService#delete(java.lang.String, java.lang.String)
	 */
	@Override
	public void delete(String userId, String dtoId) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		membershipRequestManager.delete(userInfo, dtoId);
	}

}
