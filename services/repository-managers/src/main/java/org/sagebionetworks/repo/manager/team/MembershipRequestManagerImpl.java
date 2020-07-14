/**
 * 
 */
package org.sagebionetworks.repo.manager.team;

import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_DISPLAY_NAME;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_ONE_CLICK_JOIN;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_REQUESTER_MESSAGE;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_TEAM_ID;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_TEAM_NAME;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_USER_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.entity.ContentType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.dataaccess.RestrictionInformationManager;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRequestDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * @author brucehoff
 *
 */
public class MembershipRequestManagerImpl implements MembershipRequestManager {
	
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired 
	private MembershipRequestDAO membershipRequestDAO;
	@Autowired
	private UserProfileManager userProfileManager;
	@Autowired
	private TeamDAO teamDAO;
	@Autowired
	TokenGenerator tokenGenerator;
	@Autowired
	private RestrictionInformationManager restrictionInformationManager;
	
	public static final String TEAM_MEMBERSHIP_REQUEST_CREATED_TEMPLATE = "message/teamMembershipRequestCreatedTemplate.html";
	private static final String TEAM_MEMBERSHIP_REQUEST_MESSAGE_SUBJECT = "Someone Has Requested to Join Your Team";

	public static void validateForCreate(MembershipRequest mr, UserInfo userInfo) {
		if (mr.getCreatedBy()!=null) throw new InvalidModelException("'createdBy' field is not user specifiable.");
		if (mr.getCreatedOn()!=null) throw new InvalidModelException("'createdOn' field is not user specifiable.");
		if (mr.getId()!=null) throw new InvalidModelException("'id' field is not user specifiable.");
		if (!userInfo.isAdmin() && mr.getUserId()!=null && !mr.getUserId().equals(userInfo.getId().toString()))
			throw new InvalidModelException("May not specify a user id other than yourself.");
		if (mr.getTeamId()==null) throw new InvalidModelException("'teamId' field is required.");
	}
	
	private boolean hasUnmetAccessRequirements(UserInfo memberUserInfo, String teamId) throws NotFoundException {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(teamId);
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		return restrictionInformationManager.getRestrictionInformation(memberUserInfo, request).getHasUnmetAccessRequirement();
	}


	public static void populateCreationFields(UserInfo userInfo, MembershipRequest mr, Date now) {
		mr.setCreatedBy(userInfo.getId().toString());
		mr.setCreatedOn(now);
		if (mr.getUserId()==null) mr.setUserId(userInfo.getId().toString());
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#create(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.MembershipRequest)
	 */
	@Override
	public MembershipRequest create(UserInfo userInfo,
	                                MembershipRequest mr) throws DatastoreException,
			InvalidModelException, UnauthorizedException {
		if (AuthorizationUtils.isUserAnonymous(userInfo)) 
			throw new UnauthorizedException("anonymous user cannot create membership request.");
		validateForCreate(mr, userInfo);
		if (!userInfo.isAdmin()) {
			if (hasUnmetAccessRequirements(userInfo, mr.getTeamId()))
				throw new UnauthorizedException("Requested member has unmet access requirements which must be met before asking to join the Team.");
		}
		Team team = teamDAO.get(mr.getTeamId());
		if (team.getCanPublicJoin() != null && team.getCanPublicJoin()) {
			throw new IllegalArgumentException("This team is already open for the public to join, membership requests are not needed.");
		}

		Date now = new Date();
		populateCreationFields(userInfo, mr, now);
		return membershipRequestDAO.create(mr);
	}

	@Override
	public List<MessageToUserAndBody> createMembershipRequestNotification(MembershipRequest mr,
			String acceptRequestEndpoint, String notificationUnsubscribeEndpoint) {
		ValidateArgument.required(acceptRequestEndpoint, "acceptRequestEndpoint");
		ValidateArgument.required(notificationUnsubscribeEndpoint, "notificationUnsubscribeEndpoint");
		
		List<MessageToUserAndBody> result = new ArrayList<MessageToUserAndBody>();
		
		if (mr.getCreatedOn() == null) {
			mr.setCreatedOn(new Date());
		}
		
		UserProfile userProfile = userProfileManager.getUserProfile(mr.getCreatedBy());
		String displayName = EmailUtils.getDisplayNameWithUsername(userProfile);
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put(TEMPLATE_KEY_DISPLAY_NAME, displayName);
		fieldValues.put(TEMPLATE_KEY_USER_ID, mr.getCreatedBy());
		fieldValues.put(TEMPLATE_KEY_TEAM_NAME, teamDAO.get(mr.getTeamId()).getName());
		fieldValues.put(TEMPLATE_KEY_TEAM_ID, mr.getTeamId());
		
		if (mr.getMessage()==null || mr.getMessage().length()==0) {
			fieldValues.put(TEMPLATE_KEY_REQUESTER_MESSAGE, "");
		} else {
			fieldValues.put(TEMPLATE_KEY_REQUESTER_MESSAGE, 
							"The requester sends the following message: <Blockquote> "+
							mr.getMessage()+" </Blockquote> ");
		}
		
		Set<String> teamAdmins = new HashSet<>(teamDAO.getAdminTeamMemberIds(mr.getTeamId()));
		
		for (String recipientPrincipalId : teamAdmins) {

			fieldValues.put(TEMPLATE_KEY_ONE_CLICK_JOIN, EmailUtils.createOneClickJoinTeamLink(
					acceptRequestEndpoint, recipientPrincipalId, mr.getCreatedBy(), mr.getTeamId(), mr.getCreatedOn(), tokenGenerator));

			String messageContent = EmailUtils.readMailTemplate(TEAM_MEMBERSHIP_REQUEST_CREATED_TEMPLATE, fieldValues);
			
			MessageToUser mtu = new MessageToUser();
			mtu.setRecipients(Collections.singleton(recipientPrincipalId));
			mtu.setSubject(TEAM_MEMBERSHIP_REQUEST_MESSAGE_SUBJECT);
			mtu.setNotificationUnsubscribeEndpoint(notificationUnsubscribeEndpoint);
			result.add(new MessageToUserAndBody(mtu, messageContent, ContentType.TEXT_HTML.getMimeType()));
		}
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#get(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public MembershipRequest get(UserInfo userInfo, String id)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		MembershipRequest mr = membershipRequestDAO.get(id);
		AuthorizationStatus status = authorizationManager.canAccessMembershipRequest(userInfo, mr, ACCESS_TYPE.READ);
		status.checkAuthorizationOrElseThrow();
		return mr;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#delete(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public void delete(UserInfo userInfo, String id) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		MembershipRequest mr;
		try {
			mr = membershipRequestDAO.get(id);
		} catch (NotFoundException e) {
			return;
		}
		authorizationManager.canAccessMembershipRequest(userInfo, mr, ACCESS_TYPE.DELETE)
				.checkAuthorizationOrElseThrow();

		membershipRequestDAO.delete(id);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#getOpenByTeamInRange(java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipRequest> getOpenByTeamInRange(UserInfo userInfo, 
			String teamId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(
				userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).isAuthorized())
			throw new UnauthorizedException("Cannot retrieve membership requests.");
		Date now = new Date();
		long teamIdAsLong = Long.parseLong(teamId);
		List<MembershipRequest> mrList = membershipRequestDAO.getOpenByTeamInRange(teamIdAsLong, now.getTime(), limit, offset);
		long count = membershipRequestDAO.getOpenByTeamCount(teamIdAsLong, now.getTime());
		PaginatedResults<MembershipRequest> results = new PaginatedResults<MembershipRequest>();
		results.setResults(mrList);
		results.setTotalNumberOfResults(count);
		return results;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#getOpenByTeamAndRequestorInRange(java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipRequest> getOpenByTeamAndRequesterInRange(UserInfo userInfo, 
			String teamId, String requestorId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(
				userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).isAuthorized())
			throw new UnauthorizedException("Cannot retrieve membership requests.");
		Date now = new Date();
		long teamIdAsLong = Long.parseLong(teamId);
		long requestorIdAsLong = Long.parseLong(requestorId);
		List<MembershipRequest> mrList = membershipRequestDAO.getOpenByTeamAndRequesterInRange(teamIdAsLong, requestorIdAsLong, now.getTime(), limit, offset);
		long count = membershipRequestDAO.getOpenByTeamAndRequesterCount(teamIdAsLong, requestorIdAsLong, now.getTime());
		PaginatedResults<MembershipRequest> results = new PaginatedResults<MembershipRequest>();
		results.setResults(mrList);
		results.setTotalNumberOfResults(count);
		return results;
	}


	@Override
	public PaginatedResults<MembershipRequest> getOpenSubmissionsByRequesterInRange(
			UserInfo userInfo, String requesterId, long limit, long offset) throws DatastoreException, NotFoundException {
		if (!userInfo.getId().toString().equals(requesterId)) throw new UnauthorizedException("Cannot retrieve another's membership requests.");
		Date now = new Date();
		long requesterIdAsLong = Long.parseLong(requesterId);
		List<MembershipRequest> mrList = membershipRequestDAO.getOpenByRequesterInRange(requesterIdAsLong, now.getTime(), limit, offset);
		long count = membershipRequestDAO.getOpenByRequesterCount(requesterIdAsLong, now.getTime());
		PaginatedResults<MembershipRequest> results = new PaginatedResults<MembershipRequest>();
		results.setResults(mrList);
		results.setTotalNumberOfResults(count);
		return results;
	}

	@Override
	public PaginatedResults<MembershipRequest> getOpenSubmissionsByTeamAndRequesterInRange(
			UserInfo userInfo, String teamId, String requesterId, long limit,
			long offset) throws DatastoreException, NotFoundException {
		if (!userInfo.getId().toString().equals(requesterId)) throw new UnauthorizedException("Cannot retrieve another's membership requests.");
		Date now = new Date();
		long teamIdAsLong = Long.parseLong(teamId);
		long requestorIdAsLong = Long.parseLong(requesterId);
		List<MembershipRequest> mrList = membershipRequestDAO.getOpenByTeamAndRequesterInRange(teamIdAsLong, requestorIdAsLong, now.getTime(), limit, offset);
		long count = membershipRequestDAO.getOpenByTeamAndRequesterCount(teamIdAsLong, requestorIdAsLong, now.getTime());
		PaginatedResults<MembershipRequest> results = new PaginatedResults<MembershipRequest>();
		results.setResults(mrList);
		results.setTotalNumberOfResults(count);
		return results;
	}

	@Override
	public Count getOpenSubmissionsCountForTeamAdmin(UserInfo userInfo) {
		ValidateArgument.required(userInfo, "userInfo");
		List<String> teamIds = teamDAO.getAllTeamsUserIsAdmin(userInfo.getId().toString());
		Count result = new Count();
		if (teamIds.isEmpty()) {
			result.setCount(0L);
		} else {
			result.setCount(membershipRequestDAO.getOpenByTeamsCount(teamIds, System.currentTimeMillis()));
		}
		return result;
	}
}
