/**
 * 
 */
package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.MembershipInvitationManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author brucehoff
 *
 */
public class MembershipInvitationServiceImpl implements
		MembershipInvitationService {
	
	@Autowired
	private MembershipInvitationManager membershipInvitationManager;
	@Autowired
	private UserManager userManager;
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipInvitationService#create(java.lang.String, org.sagebionetworks.repo.model.MembershipInvtnSubmission)
	 */
	@Override
	public MembershipInvtnSubmission create(String userId,
			MembershipInvtnSubmission dto) throws UnauthorizedException,
			InvalidModelException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return membershipInvitationManager.create(userInfo, dto);
	}
	
	

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipInvitationService#getOpenInvitations(java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipInvitation> getOpenInvitations(
			String inviteeId, String teamId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		if (teamId==null) {
			return membershipInvitationManager.getOpenForUserInRange(inviteeId, offset, limit);
		} else {
			return membershipInvitationManager.getOpenForUserAndTeamInRange(inviteeId, teamId, offset, limit);
		}
		
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipInvitationService#get(java.lang.String, java.lang.String)
	 */
	@Override
	public MembershipInvtnSubmission get(String userId, String dtoId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return membershipInvitationManager.get(userInfo, dtoId);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipInvitationService#delete(java.lang.String, java.lang.String)
	 */
	@Override
	public void delete(String userId, String dtoId) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		membershipInvitationManager.delete(userInfo, dtoId);
	}

}
