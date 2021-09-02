/**
 * 
 */
package org.sagebionetworks.repo.manager.team;

import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_INVITER_MESSAGE;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_ONE_CLICK_JOIN;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_SENDER_DISPLAY_NAME;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_TEAM_NAME;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.principal.SynapseEmailService;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.InviteeVerificationSignedToken;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvitationDAO;
import org.sagebionetworks.repo.model.MembershipInvtnSignedToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.ses.EmailQuarantineDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailException;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.InitBinder;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

/**
 * @author brucehoff
 *
 */
public class MembershipInvitationManagerImpl implements MembershipInvitationManager {

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
	@Autowired
	private EmailQuarantineDao emailQuarantineDao;
	@Autowired
	private UserProfileManager userProfileManager;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private MessageManager messageManager;
	
	public static final String INVITATION_MESSAGE_BODY_TEMPLATE = "message/teamMembershipInvitationTemplate.html";
	public static final String INVITATION_MESSAGE_SUBJECT_TEMPLATE = "%s has invited you to join the %s team";

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
	@WriteTransaction
	@Override
	public MembershipInvitation create(UserInfo userInfo,
	                                   MembershipInvitation mi) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException {
		validateForCreate(mi);
		if (!authorizationManager.canAccessMembershipInvitation(userInfo, mi, ACCESS_TYPE.CREATE).isAuthorized())
			throw new UnauthorizedException("Cannot create membership invitation.");
		Date now = new Date();
		populateCreationFields(userInfo, mi, now);
		MembershipInvitation created = membershipInvitationDAO.create(mi);
		return created;
	}
	
	@Override
	public void sendInvitationEmailToSynapseUser(UserInfo userInfo, MembershipInvitation invitation, String acceptInvitationEndpoint, String notificationUnsubscribeEndpoint) {
		ValidateArgument.required(userInfo, "The user");
		ValidateArgument.required(invitation, "The invitation");
		ValidateArgument.required(invitation.getInviteeId(), "The id of the invitee");
		ValidateArgument.required(notificationUnsubscribeEndpoint, "The notification unsubscribe endpoint");
		
		if (acceptInvitationEndpoint == null) {
			acceptInvitationEndpoint = ServiceConstants.ACCEPT_INVITATION_ENDPOINT;
		}
		
		Team team = teamDAO.get(invitation.getTeamId());
		
		String senderUsername = principalAliasDAO.getUserName(userInfo.getId());
		String senderDisplayName = buildSenderDisplayName(userInfo, senderUsername);
		String subject = buildInvitationEmailSubject(senderDisplayName, team.getName());
		// This hack was there in a previous change, no idea why this is needed since as far as I can see this should not be possible, but there is a specific test for this and the commit history does not help
		Date createdOn = invitation.getCreatedOn() == null ? new Date() : invitation.getCreatedOn();
		String oneClickJoinLink = EmailUtils.createOneClickJoinTeamLink(acceptInvitationEndpoint, invitation.getInviteeId(), invitation.getInviteeId(), invitation.getTeamId(), createdOn, tokenGenerator);
		String emailBody = buildInvitationEmailBody(senderDisplayName, team.getName(), oneClickJoinLink, invitation.getMessage());
		
		// The message is for an existing synapse user, we can re-use the MessageToUser machinery
		FileHandle fileHandle;
		
		try {
			fileHandle = fileHandleManager.createCompressedFileFromString(userInfo.getId().toString(), new Date(), emailBody, ContentType.TEXT_HTML.getMimeType());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		MessageToUser mtu = new MessageToUser();
		
		mtu.setSubject(subject);
		mtu.setRecipients(Collections.singleton(invitation.getInviteeId()));
		mtu.setNotificationUnsubscribeEndpoint(notificationUnsubscribeEndpoint);
		mtu.setFileHandleId(fileHandle.getId());
		mtu.setWithUnsubscribeLink(true);
		// Setting this to false will the email as if it was sent by the user
		mtu.setIsNotificationMessage(false);
		mtu.setWithProfileSettingLink(false);
		
		// Since we need want to send the message in behalf of the user we use the messageManager directly rather than the notificationManager (See https://sagebionetworks.jira.com/browse/PLFM-5566)
		messageManager.createMessage(userInfo, mtu);
		
	}
	
	@Override
	public void sendInvitationEmailToEmail(UserInfo userInfo, MembershipInvitation invitation, String acceptInvitationEndpoint) {
		ValidateArgument.required(userInfo, "The user");
		ValidateArgument.required(invitation, "The invitation");
		ValidateArgument.required(invitation.getInviteeEmail(), "The invitee email");
		
		if (!AuthorizationUtils.isCertifiedUser(userInfo)) {
			throw new IllegalArgumentException("You must be a certified user to send email invitations");
		}
		
		if (acceptInvitationEndpoint == null) {
			acceptInvitationEndpoint = ServiceConstants.ACCEPT_INVITATION_ENDPOINT;
		}
		
		if (emailQuarantineDao.isQuarantined(invitation.getInviteeEmail())) {
			throw new QuarantinedEmailException("Cannot send membership invitation email to address: " + invitation.getInviteeEmail());
		}

		Team team = teamDAO.get(invitation.getTeamId());
		
		String senderUsername = principalAliasDAO.getUserName(userInfo.getId());
		String senderDisplayName = buildSenderDisplayName(userInfo, senderUsername);
		String subject = buildInvitationEmailSubject(senderDisplayName, team.getName());
		String oneClickJoinLink = EmailUtils.createMembershipInvtnLink(acceptInvitationEndpoint, invitation.getId(), tokenGenerator);
		String emailBody = buildInvitationEmailBody(senderDisplayName, team.getName(), oneClickJoinLink, invitation.getMessage());
		
		// Since the user might not exist in the system we send an email through SES directly (The MessageToUser machinery works only between existing synapse users)
		SendRawEmailRequest sendEmailRequest = new SendRawEmailRequestBuilder()
				.withRecipientEmail(invitation.getInviteeEmail())
				.withSubject(subject)
				.withBody(emailBody, SendRawEmailRequestBuilder.BodyType.HTML)
				.withIsNotificationMessage(false)
				.withSenderUserName(senderUsername)
				// If we supply the senderDisplayName the source will be senderDisplayName <username@synapse.org>, since the senderUserName here can be the username itself we remove it in that case
				.withSenderDisplayName(senderUsername.equals(senderDisplayName) ? null : senderDisplayName)
				.build();
		
		sesClient.sendRawEmail(sendEmailRequest);
 
	}
	
	String buildSenderDisplayName(UserInfo userInfo, String alternativeName) {
		try {
			UserProfile userProfile = userProfileManager.getUserProfile(userInfo.getId().toString());
			return EmailUtils.getDisplayNameOrUsername(userProfile);
		} catch (NotFoundException ex) {
			// We can have users without profiles
			return alternativeName;
		}
	}
		
	String buildInvitationEmailSubject(String senderDisplayName, String teamName) {
		return String.format(INVITATION_MESSAGE_SUBJECT_TEMPLATE, senderDisplayName, teamName);
	}
	
	String buildInvitationEmailBody(String senderDisplayName, String teamName, String oneClickJoinLink, String senderMessage) {
		Map<String,String> fieldValues = new HashMap<String,String>();
		
		fieldValues.put(TEMPLATE_KEY_TEAM_NAME, teamName);
		fieldValues.put(TEMPLATE_KEY_SENDER_DISPLAY_NAME, senderDisplayName);
		fieldValues.put(TEMPLATE_KEY_ONE_CLICK_JOIN, oneClickJoinLink);
		
		if (StringUtils.isBlank(senderMessage)) {
			fieldValues.put(TEMPLATE_KEY_INVITER_MESSAGE, "");
		} else {
			fieldValues.put(TEMPLATE_KEY_INVITER_MESSAGE, "The inviter sends the following message: <Blockquote> " + senderMessage + " </Blockquote> ");
		}
		
		return EmailUtils.readMailTemplate(INVITATION_MESSAGE_BODY_TEMPLATE, fieldValues);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#get(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public MembershipInvitation get(UserInfo userInfo, String id)
			throws DatastoreException, NotFoundException {
		MembershipInvitation mi = membershipInvitationDAO.get(id);
		if (!authorizationManager.canAccessMembershipInvitation(userInfo, mi, ACCESS_TYPE.READ).isAuthorized())
			throw new UnauthorizedException("Cannot retrieve membership invitation.");
		return mi;
	}

	@Override
	public MembershipInvitation get(String miId, MembershipInvtnSignedToken token) throws DatastoreException, NotFoundException {
		AuthorizationStatus status = authorizationManager.canAccessMembershipInvitation(token, ACCESS_TYPE.READ);
		status.checkAuthorizationOrElseThrow();
		return membershipInvitationDAO.get(miId);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#delete(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@WriteTransaction
	@Override
	public void delete(UserInfo userInfo, String id) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		MembershipInvitation mi;
		try {
			mi = membershipInvitationDAO.get(id);
		} catch (NotFoundException e) {
			return;
		}
		if (!authorizationManager.canAccessMembershipInvitation(userInfo, mi, ACCESS_TYPE.DELETE).isAuthorized()) {
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

	@WriteTransaction
	@Override
	public void updateInviteeId(Long userId, String miId, InviteeVerificationSignedToken token) {
		AuthorizationStatus status = authorizationManager.canAccessMembershipInvitation(userId, token, ACCESS_TYPE.UPDATE);
		status.checkAuthorizationOrElseThrow();
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
				userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).isAuthorized())
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
				userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).isAuthorized())
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
