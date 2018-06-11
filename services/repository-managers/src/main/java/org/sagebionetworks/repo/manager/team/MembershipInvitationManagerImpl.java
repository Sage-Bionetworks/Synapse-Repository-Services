/**
 * 
 */
package org.sagebionetworks.repo.manager.team;

import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_INVITER_MESSAGE;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_ONE_CLICK_JOIN;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_TEAM_ID;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_TEAM_NAME;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.entity.ContentType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder;
import org.sagebionetworks.repo.manager.principal.SynapseEmailService;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.InviteeVerificationSignedToken;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvitationDAO;
import org.sagebionetworks.repo.model.MembershipInvtnSignedToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

/**
 * @author brucehoff
 *
 */
public class MembershipInvitationManagerImpl implements
		MembershipInvitationManager {

	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired 
	private MembershipInvitationDAO membershipInvitationDAO;
	@Autowired
	private TeamDAO teamDAO;
	@Autowired
	private SynapseEmailService sesClient;
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	@Autowired
	private TokenGenerator tokenGenerator;

	public static final String TEAM_MEMBERSHIP_INVITATION_EXTENDED_TEMPLATE = "message/teamMembershipInvitationExtendedTemplate.html";

	private static final String TEAM_MEMBERSHIP_INVITATION_MESSAGE_SUBJECT = "You Have Been Invited to Join a Team";

	protected static final long TWENTY_FOUR_HOURS_IN_MS = 1000 * 60 * 60 * 24;

	public static void validateForCreate(MembershipInvitation mi) {
		if (mi.getCreatedBy()!=null) throw new InvalidModelException("'createdBy' field is not user specifiable.");
		if (mi.getCreatedOn()!=null) throw new InvalidModelException("'createdOn' field is not user specifiable.");
		if (mi.getId()!=null) throw new InvalidModelException("'id' field is not user specifiable.");
		if ((mi.getInviteeId() == null ^ mi.getInviteeEmail() == null) == false) {
			throw new InvalidModelException("One and only one of the fields 'inviteeId' and 'inviteeEmail' is required");
		}
		if (mi.getTeamId()==null) throw new InvalidModelException("'teamId' field is required.");
	}

	public static void populateCreationFields(UserInfo userInfo, MembershipInvitation mi, Date now) {
		mi.setCreatedBy(userInfo.getId().toString());
		mi.setCreatedOn(now);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#create(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.MembershipInvitation)
	 */
	@WriteTransactionReadCommitted
	@Override
	public MembershipInvitation create(UserInfo userInfo,
	                                   MembershipInvitation mi) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException {
		validateForCreate(mi);
		if (!authorizationManager.canAccessMembershipInvitation(userInfo, mi, ACCESS_TYPE.CREATE).getAuthorized())
			throw new UnauthorizedException("Cannot create membership invitation.");
		Date now = new Date();
		populateCreationFields(userInfo, mi, now);
		MembershipInvitation created = membershipInvitationDAO.create(mi);
		return created;
	}
	
	@Override
	public MessageToUserAndBody createInvitationMessageToUser(MembershipInvitation mi,
			String acceptInvitationEndpoint, String notificationUnsubscribeEndpoint) {
		ValidateArgument.required(notificationUnsubscribeEndpoint, "notificationUnsubscribeEndpoint");
		if (acceptInvitationEndpoint == null) {
			acceptInvitationEndpoint = ServiceConstants.ACCEPT_INVITATION_ENDPOINT;
		}
		if (mi.getCreatedOn() == null) mi.setCreatedOn(new Date());
		MessageToUser mtu = new MessageToUser();
		mtu.setSubject(TEAM_MEMBERSHIP_INVITATION_MESSAGE_SUBJECT);
		mtu.setRecipients(Collections.singleton(mi.getInviteeId()));
		mtu.setNotificationUnsubscribeEndpoint(notificationUnsubscribeEndpoint);
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put(TEMPLATE_KEY_TEAM_NAME, teamDAO.get(mi.getTeamId()).getName());
		fieldValues.put(TEMPLATE_KEY_TEAM_ID, mi.getTeamId());
		fieldValues.put(TEMPLATE_KEY_ONE_CLICK_JOIN, EmailUtils.createOneClickJoinTeamLink(
				acceptInvitationEndpoint, mi.getInviteeId(), mi.getInviteeId(), mi.getTeamId(), mi.getCreatedOn(), tokenGenerator));
		if (mi.getMessage()==null || mi.getMessage().length()==0) {
			fieldValues.put(TEMPLATE_KEY_INVITER_MESSAGE, "");
		} else {
			fieldValues.put(TEMPLATE_KEY_INVITER_MESSAGE, 
							"The inviter sends the following message: <Blockquote> "+
							mi.getMessage()+" </Blockquote> ");
		}
		String messageContent = EmailUtils.readMailTemplate(TEAM_MEMBERSHIP_INVITATION_EXTENDED_TEMPLATE, fieldValues);
		return new MessageToUserAndBody(mtu, messageContent, ContentType.TEXT_HTML.getMimeType());
	}

	@Override
	public void sendInvitationToEmail(MembershipInvitation mi, String acceptInvitationEndpoint) {
		if (acceptInvitationEndpoint == null) {
			acceptInvitationEndpoint = ServiceConstants.ACCEPT_EMAIL_INVITATION_ENDPOINT;
		}
		String teamName = teamDAO.get(mi.getTeamId()).getName();
		String subject = "You have been invited to join the team " + teamName;
		Map<String,String> fieldValues = new HashMap<>();
		fieldValues.put(EmailUtils.TEMPLATE_KEY_TEAM_ID, mi.getTeamId());
		fieldValues.put(EmailUtils.TEMPLATE_KEY_TEAM_NAME, teamName);
		fieldValues.put(EmailUtils.TEMPLATE_KEY_ONE_CLICK_JOIN, EmailUtils.createMembershipInvtnLink(acceptInvitationEndpoint, mi.getId(), tokenGenerator));
		fieldValues.put(EmailUtils.TEMPLATE_KEY_INVITER_MESSAGE, mi.getMessage());
		String messageBody = EmailUtils.readMailTemplate("message/teamMembershipInvitationExtendedToEmailTemplate.html", fieldValues);
		SendRawEmailRequest sendEmailRequest = new SendRawEmailRequestBuilder()
			.withRecipientEmail(mi.getInviteeEmail())
			.withSubject(subject)
			.withBody(messageBody, SendRawEmailRequestBuilder.BodyType.HTML)
			.withIsNotificationMessage(true)
			.build();
		sesClient.sendRawEmail(sendEmailRequest);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#get(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public MembershipInvitation get(UserInfo userInfo, String id)
			throws DatastoreException, NotFoundException {
		MembershipInvitation mi = membershipInvitationDAO.get(id);
		if (!authorizationManager.canAccessMembershipInvitation(userInfo, mi, ACCESS_TYPE.READ).getAuthorized())
			throw new UnauthorizedException("Cannot retrieve membership invitation.");
		return mi;
	}

	@Override
	public MembershipInvitation get(String miId, MembershipInvtnSignedToken token) throws DatastoreException, NotFoundException {
		AuthorizationStatus status = authorizationManager.canAccessMembershipInvitation(token, ACCESS_TYPE.READ);
		if (!status.getAuthorized()) {
			throw new UnauthorizedException(status.getReason());
		}
		return membershipInvitationDAO.get(miId);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#delete(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@WriteTransactionReadCommitted
	@Override
	public void delete(UserInfo userInfo, String id) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		MembershipInvitation mi;
		try {
			mi = membershipInvitationDAO.get(id);
		} catch (NotFoundException e) {
			return;
		}
		if (!authorizationManager.canAccessMembershipInvitation(userInfo, mi, ACCESS_TYPE.DELETE).getAuthorized()) {
			throw new UnauthorizedException("Cannot delete membership invitation.");
		}
		membershipInvitationDAO.delete(id);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#getOpenForUserInRange(java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipInvitation> getOpenForUserInRange(
			String principalId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		Date now = new Date();
		long principalIdAsLong = Long.parseLong(principalId);
		List<MembershipInvitation> miList = membershipInvitationDAO.getOpenByUserInRange(principalIdAsLong, now.getTime(), limit, offset);
		long count = membershipInvitationDAO.getOpenByUserCount(principalIdAsLong, now.getTime());
		PaginatedResults<MembershipInvitation> results = new PaginatedResults<MembershipInvitation>();
		results.setResults(miList);
		results.setTotalNumberOfResults(count);
		return results;
	}

	@Override
	public Count getOpenInvitationCountForUser(String principalId) {
		ValidateArgument.required(principalId, "principalId");
		Count result = new Count();
		long count = membershipInvitationDAO.getOpenByUserCount(Long.parseLong(principalId), System.currentTimeMillis());
		result.setCount(count);
		return result;
	}

	@Override
	public InviteeVerificationSignedToken getInviteeVerificationSignedToken(Long userId, String membershipInvitationId) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(membershipInvitationId, "membershipInvitationId");
		MembershipInvitation mi = membershipInvitationDAO.get(membershipInvitationId);
		if (mi.getExpiresOn() != null && mi.getExpiresOn().before(new Date())) {
			throw new IllegalArgumentException("Indicated MembershipInvitation is expired.");
		}
		if (!(mi.getInviteeId() == null && mi.getInviteeEmail() != null)) {
			throw new IllegalArgumentException("The indicated invitation must have a null inviteeId and a non null inviteeEmail.");
		}
		if (!principalAliasDAO.aliasIsBoundToPrincipal(mi.getInviteeEmail(), Long.toString(userId))) {
			// inviteeEmail is not associated with the user
			throw new UnauthorizedException("This membership invitation is not addressed to the authenticated user.");
		}
		InviteeVerificationSignedToken response = new InviteeVerificationSignedToken();
		response.setInviteeId(userId.toString());
		response.setMembershipInvitationId(membershipInvitationId);
		response.setExpiresOn(new Date(new Date().getTime() + TWENTY_FOUR_HOURS_IN_MS));
		tokenGenerator.signToken(response);
		return response;
	}

	@WriteTransactionReadCommitted
	@Override
	public void updateInviteeId(Long userId, String miId, InviteeVerificationSignedToken token) {
		AuthorizationStatus status = authorizationManager.canAccessMembershipInvitation(userId, token, ACCESS_TYPE.UPDATE);
		if (!status.getAuthorized()) {
			throw new UnauthorizedException(status.getReason());
		}
		if (!miId.equals(token.getMembershipInvitationId())) {
			throw new IllegalArgumentException("ID in URI and ID in signed token don't match");
		}
		if (token.getExpiresOn() == null ) {
			throw new IllegalArgumentException("expiresOn field in InviteeVerificationSignedToken is missing");
		}
		if (token.getExpiresOn().before(new Date())) {
			throw new IllegalArgumentException("InviteeVerificationSignedToken is expired");
		}
		MembershipInvitation mi = membershipInvitationDAO.getWithUpdateLock(miId);
		if (!(mi.getInviteeId() == null && mi.getInviteeEmail() != null)) {
			throw new IllegalArgumentException("Cannot update inviteeId. The target invitation must have a null inviteeId and a non null inviteeEmail.");
		}
		membershipInvitationDAO.updateInviteeId(miId, userId);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#getOpenForUserAndTeamInRange(java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipInvitation> getOpenForUserAndTeamInRange(
			String principalId, String teamId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		Date now = new Date();
		long principalIdAsLong = Long.parseLong(principalId);
		long teamIdAsLong = Long.parseLong(teamId);
		List<MembershipInvitation> miList = membershipInvitationDAO.getOpenByTeamAndUserInRange(teamIdAsLong, principalIdAsLong, now.getTime(), limit, offset);
		long count = membershipInvitationDAO.getOpenByTeamAndUserCount(teamIdAsLong, principalIdAsLong, now.getTime());
		PaginatedResults<MembershipInvitation> results = new PaginatedResults<MembershipInvitation>();
		results.setResults(miList);
		results.setTotalNumberOfResults(count);
		return results;
	}

	@Override
	public PaginatedResults<MembershipInvitation> getOpenSubmissionsForTeamInRange(
			UserInfo userInfo, String teamId, long limit, long offset) throws NotFoundException {
		if (!authorizationManager.canAccess(
				userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).getAuthorized()) 
			throw new UnauthorizedException("Cannot retrieve membership invitations for team "+teamId+".");
		Date now = new Date();
		long teamIdAsLong = Long.parseLong(teamId);
		List<MembershipInvitation> miList = membershipInvitationDAO.getOpenByTeamInRange(teamIdAsLong, now.getTime(), limit, offset);
		long count = membershipInvitationDAO.getOpenByTeamCount(teamIdAsLong, now.getTime());
		PaginatedResults<MembershipInvitation> results = new PaginatedResults<MembershipInvitation>();
		results.setResults(miList);
		results.setTotalNumberOfResults(count);
		return results;
	}

	@Override
	public PaginatedResults<MembershipInvitation> getOpenSubmissionsForUserAndTeamInRange(
			UserInfo userInfo, String inviteeId, String teamId, long limit,
			long offset) throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(
				userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).getAuthorized()) 
			throw new UnauthorizedException("Cannot retrieve membership invitations for team "+teamId+".");
		Date now = new Date();
		long teamIdAsLong = Long.parseLong(teamId);
		long inviteeIdAsLong = Long.parseLong(inviteeId);
		List<MembershipInvitation> miList = membershipInvitationDAO.getOpenByTeamAndUserInRange(teamIdAsLong, inviteeIdAsLong, now.getTime(), limit, offset);
		long count = membershipInvitationDAO.getOpenByTeamCount(teamIdAsLong, now.getTime());
		PaginatedResults<MembershipInvitation> results = new PaginatedResults<MembershipInvitation>();
		results.setResults(miList);
		results.setTotalNumberOfResults(count);
		return results;
	}

}
