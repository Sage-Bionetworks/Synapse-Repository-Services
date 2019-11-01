/**
 * 
 */
package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.MembershipRequestManager;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
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
	@Autowired
	private NotificationManager notificationManager;
	
	public MembershipRequestServiceImpl() {}
	
	public MembershipRequestServiceImpl(MembershipRequestManager membershipRequestManager,
			UserManager userManager,
			NotificationManager notificationManager) {
		this.membershipRequestManager = membershipRequestManager;
		this.userManager=userManager;
		this.notificationManager=notificationManager;
	}
	

	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipRequestService#create(java.lang.String, org.sagebionetworks.repo.model.MembershipRequest)
	 */
	@Override
	public MembershipRequest create(Long userId,
	                                MembershipRequest dto,
	                                String acceptRequestEndpoint,
	                                String notificationUnsubscribeEndpoint) throws UnauthorizedException,
			InvalidModelException, DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		MembershipRequest created = membershipRequestManager.create(userInfo, dto);
		
		List<MessageToUserAndBody> messages = membershipRequestManager.createMembershipRequestNotification(created,
						acceptRequestEndpoint, notificationUnsubscribeEndpoint);
		
		notificationManager.sendNotifications(userInfo, messages);
		
		return created;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipRequestService#getOpenRequests(java.lang.String, java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipRequest> getOpenRequests(Long userId,
			String requesterId, String teamId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (requesterId==null) {
			return membershipRequestManager.getOpenByTeamInRange(userInfo, teamId, limit, offset);
		} else {
			return membershipRequestManager.getOpenByTeamAndRequesterInRange(userInfo, teamId, requesterId, limit, offset);
		}
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipRequestService#getOpenRequestSubmissions(java.lang.String, java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipRequest> getOpenRequestSubmissions(Long userId,
	                                                                     String requesterId, String teamId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (teamId==null) {
			return membershipRequestManager.getOpenSubmissionsByRequesterInRange(userInfo, requesterId, limit, offset);
		} else {
			return membershipRequestManager.getOpenSubmissionsByTeamAndRequesterInRange(userInfo, teamId, requesterId, limit, offset);
		}
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipRequestService#get(java.lang.String, java.lang.String)
	 */
	@Override
	public MembershipRequest get(Long userId, String dtoId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return membershipRequestManager.get(userInfo, dtoId);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipRequestService#delete(java.lang.String, java.lang.String)
	 */
	@Override
	public void delete(Long userId, String dtoId) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		membershipRequestManager.delete(userInfo, dtoId);
	}
	
	@Override
	public Count getOpenMembershipRequestCount(Long userId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return membershipRequestManager.getOpenSubmissionsCountForTeamAdmin(userInfo);
	}
}

