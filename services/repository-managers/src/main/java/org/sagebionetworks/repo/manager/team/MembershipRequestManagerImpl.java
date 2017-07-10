/**
 * 
 */
package org.sagebionetworks.repo.manager.team;

import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_DISPLAY_NAME;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_USER_ID;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_TEAM_ID;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_ONE_CLICK_JOIN;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_REQUESTER_MESSAGE;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_TEAM_NAME;

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
import org.sagebionetworks.repo.manager.AccessRequirementUtil;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.MembershipRqstSubmissionDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
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
	private MembershipRqstSubmissionDAO membershipRqstSubmissionDAO;
	@Autowired
	private UserProfileManager userProfileManager;
	@Autowired
	private TeamDAO teamDAO;
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;
	
	public static final String TEAM_MEMBERSHIP_REQUEST_CREATED_TEMPLATE = "message/teamMembershipRequestCreatedTemplate.html";
	private static final String TEAM_MEMBERSHIP_REQUEST_MESSAGE_SUBJECT = "Someone Has Requested to Join Your Team";

	public static void validateForCreate(MembershipRqstSubmission mrs, UserInfo userInfo) {
		if (mrs.getCreatedBy()!=null) throw new InvalidModelException("'createdBy' field is not user specifiable.");
		if (mrs.getCreatedOn()!=null) throw new InvalidModelException("'createdOn' field is not user specifiable.");
		if (mrs.getId()!=null) throw new InvalidModelException("'id' field is not user specifiable.");
		if (!userInfo.isAdmin() && mrs.getUserId()!=null && !mrs.getUserId().equals(userInfo.getId().toString())) 
			throw new InvalidModelException("May not specify a user id other than yourself.");
		if (mrs.getTeamId()==null) throw new InvalidModelException("'teamId' field is required.");
	}
	
	private boolean hasUnmetAccessRequirements(UserInfo memberUserInfo, String teamId) throws NotFoundException {
		List<Long> unmetRequirements = accessRequirementDAO.getAllUnmetAccessRequirements(
				Collections.singletonList(teamId), RestrictableObjectType.TEAM, memberUserInfo.getGroups(), 
				Collections.singletonList(ACCESS_TYPE.PARTICIPATE));
		return !unmetRequirements.isEmpty();
	}


	public static void populateCreationFields(UserInfo userInfo, MembershipRqstSubmission mrs, Date now) {
		mrs.setCreatedBy(userInfo.getId().toString());
		mrs.setCreatedOn(now);
		if (mrs.getUserId()==null) mrs.setUserId(userInfo.getId().toString());
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#create(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.MembershipRqstSubmission)
	 */
	@Override
	public MembershipRqstSubmission create(UserInfo userInfo,
			MembershipRqstSubmission mrs) throws DatastoreException,
			InvalidModelException, UnauthorizedException {
		if (AuthorizationUtils.isUserAnonymous(userInfo)) 
			throw new UnauthorizedException("anonymous user cannot create membership request.");
		validateForCreate(mrs, userInfo);
		if (!userInfo.isAdmin()) {
			if (hasUnmetAccessRequirements(userInfo, mrs.getTeamId()))
				throw new UnauthorizedException("Requested member has unmet access requirements which must be met before asking to join the Team.");
		}
		Team team = teamDAO.get(mrs.getTeamId());
		if (team.getCanPublicJoin() != null && team.getCanPublicJoin()) {
			throw new IllegalArgumentException("This team is already open for the public to join, membership requests are not needed.");
		}

		Date now = new Date();
		populateCreationFields(userInfo, mrs, now);
		return membershipRqstSubmissionDAO.create(mrs);
	}

	@Override
	public List<MessageToUserAndBody> createMembershipRequestNotification(MembershipRqstSubmission mrs,
			String acceptRequestEndpoint, String notificationUnsubscribeEndpoint) {
		List<MessageToUserAndBody> result = new ArrayList<MessageToUserAndBody>();
		if (acceptRequestEndpoint==null || notificationUnsubscribeEndpoint==null) return result;
		if (mrs.getCreatedOn() == null) mrs.setCreatedOn(new Date());
		UserProfile userProfile = userProfileManager.getUserProfile(mrs.getCreatedBy());
		String displayName = EmailUtils.getDisplayNameWithUsername(userProfile);
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put(TEMPLATE_KEY_DISPLAY_NAME, displayName);
		fieldValues.put(TEMPLATE_KEY_USER_ID, mrs.getCreatedBy());
		fieldValues.put(TEMPLATE_KEY_TEAM_NAME, teamDAO.get(mrs.getTeamId()).getName());
		fieldValues.put(TEMPLATE_KEY_TEAM_ID, mrs.getTeamId());
		if (mrs.getMessage()==null || mrs.getMessage().length()==0) {
			fieldValues.put(TEMPLATE_KEY_REQUESTER_MESSAGE, "");
		} else {
			fieldValues.put(TEMPLATE_KEY_REQUESTER_MESSAGE, 
							"The requester sends the following message: <Blockquote> "+
							mrs.getMessage()+" </Blockquote> ");
		}
		
		Set<String> teamAdmins = new HashSet<String>(teamDAO.getAdminTeamMembers(mrs.getTeamId()));
		for (String recipientPrincipalId : teamAdmins) {
			fieldValues.put(TEMPLATE_KEY_ONE_CLICK_JOIN, EmailUtils.createOneClickJoinTeamLink(
					acceptRequestEndpoint, recipientPrincipalId, mrs.getCreatedBy(), mrs.getTeamId(), mrs.getCreatedOn()));
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
	public MembershipRqstSubmission get(UserInfo userInfo, String id)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		MembershipRqstSubmission mrs = membershipRqstSubmissionDAO.get(id);
		if (!userInfo.isAdmin() && !userInfo.getId().toString().equals(mrs.getUserId()))
			throw new UnauthorizedException("Cannot retrieve membership request for another user.");
		return mrs;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#delete(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public void delete(UserInfo userInfo, String id) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		MembershipRqstSubmission mrs = null;
		try {
			mrs = membershipRqstSubmissionDAO.get(id);
		} catch (NotFoundException e) {
			return;
		}
		if (!userInfo.isAdmin() && !userInfo.getId().toString().equals(mrs.getUserId()))
			throw new UnauthorizedException("Cannot delete membership request for another user.");
		membershipRqstSubmissionDAO.delete(id);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#getOpenByTeamInRange(java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipRequest> getOpenByTeamInRange(UserInfo userInfo, 
			String teamId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(
				userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).getAuthorized()) 
			throw new UnauthorizedException("Cannot retrieve membership requests.");
		Date now = new Date();
		long teamIdAsLong = Long.parseLong(teamId);
		List<MembershipRequest> mrList = membershipRqstSubmissionDAO.getOpenByTeamInRange(teamIdAsLong, now.getTime(), limit, offset);
		long count = membershipRqstSubmissionDAO.getOpenByTeamCount(teamIdAsLong, now.getTime());
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
				userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).getAuthorized()) 
			throw new UnauthorizedException("Cannot retrieve membership requests.");
		Date now = new Date();
		long teamIdAsLong = Long.parseLong(teamId);
		long requestorIdAsLong = Long.parseLong(requestorId);
		List<MembershipRequest> mrList = membershipRqstSubmissionDAO.getOpenByTeamAndRequesterInRange(teamIdAsLong, requestorIdAsLong, now.getTime(), limit, offset);
		long count = membershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(teamIdAsLong, requestorIdAsLong, now.getTime());
		PaginatedResults<MembershipRequest> results = new PaginatedResults<MembershipRequest>();
		results.setResults(mrList);
		results.setTotalNumberOfResults(count);
		return results;
	}


	@Override
	public PaginatedResults<MembershipRqstSubmission> getOpenSubmissionsByRequesterInRange(
			UserInfo userInfo, String requesterId, long limit, long offset) throws DatastoreException, NotFoundException {
		if (!userInfo.getId().toString().equals(requesterId)) throw new UnauthorizedException("Cannot retrieve another's membership requests.");
		Date now = new Date();
		long requesterIdAsLong = Long.parseLong(requesterId);
		List<MembershipRqstSubmission> mrList = membershipRqstSubmissionDAO.getOpenSubmissionsByRequesterInRange(requesterIdAsLong, now.getTime(), limit, offset);
		long count = membershipRqstSubmissionDAO.getOpenByRequesterCount(requesterIdAsLong, now.getTime());
		PaginatedResults<MembershipRqstSubmission> results = new PaginatedResults<MembershipRqstSubmission>();
		results.setResults(mrList);
		results.setTotalNumberOfResults(count);
		return results;
	}

	@Override
	public PaginatedResults<MembershipRqstSubmission> getOpenSubmissionsByTeamAndRequesterInRange(
			UserInfo userInfo, String teamId, String requesterId, long limit,
			long offset) throws DatastoreException, NotFoundException {
		if (!userInfo.getId().toString().equals(requesterId)) throw new UnauthorizedException("Cannot retrieve another's membership requests.");
		Date now = new Date();
		long teamIdAsLong = Long.parseLong(teamId);
		long requestorIdAsLong = Long.parseLong(requesterId);
		List<MembershipRqstSubmission> mrList = membershipRqstSubmissionDAO.getOpenSubmissionsByTeamAndRequesterInRange(teamIdAsLong, requestorIdAsLong, now.getTime(), limit, offset);
		long count = membershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(teamIdAsLong, requestorIdAsLong, now.getTime());
		PaginatedResults<MembershipRqstSubmission> results = new PaginatedResults<MembershipRqstSubmission>();
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
			result.setCount(membershipRqstSubmissionDAO.getOpenRequestByTeamsCount(teamIds, System.currentTimeMillis()));
		}
		return result;
	}
}
