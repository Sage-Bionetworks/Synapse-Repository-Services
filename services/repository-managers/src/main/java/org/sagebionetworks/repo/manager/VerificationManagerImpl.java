package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_DISPLAY_NAME;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_REASON;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_USER_ID;
import static org.sagebionetworks.repo.model.verification.VerificationStateEnum.APPROVED;
import static org.sagebionetworks.repo.model.verification.VerificationStateEnum.REJECTED;
import static org.sagebionetworks.repo.model.verification.VerificationStateEnum.SUSPENDED;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dbo.principal.AliasUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.verification.AttachmentMetadata;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;

public class VerificationManagerImpl implements VerificationManager {
	
	@Autowired
	private VerificationDAO verificationDao;
	
	@Autowired
	private UserProfileManager userProfileManager;
	
	@Autowired
	private FileHandleManager fileHandleManager;
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	
	@Autowired
	private AuthorizationManager authorizationManager;

	@Autowired
	private TransactionalMessenger transactionalMessenger;
	
	public static final String VERIFICATION_APPROVED_TEMPLATE = "message/verificationApprovedTemplate.html";
	public static final String VERIFICATION_SUBMISSION_TEMPLATE = "message/verificationSubmissionTemplate.html";
	public static final String VERIFICATION_REJECTED_TEMPLATE = "message/verificationRejectedTemplate.html";
	public static final String VERIFICATION_REJECTED_NO_REASON_TEMPLATE = "message/verificationRejectedNoReasonTemplate.html";
	public static final String VERIFICATION_SUSPENDED_TEMPLATE = "message/verificationSuspendedTemplate.html";
	public static final String VERIFICATION_SUSPENDED_NO_REASON_TEMPLATE = "message/verificationSuspendedNoReasonTemplate.html";

	public static final String VERIFICATION_NOTIFICATION_SUBJECT = "Synapse Identity Verification Request";
	
	public VerificationManagerImpl() {}

	// for testing
	public VerificationManagerImpl(
			VerificationDAO verificationDao,
			UserProfileManager userProfileManager,
			FileHandleManager fileHandleManager,
			PrincipalAliasDAO principalAliasDAO,
			AuthorizationManager authorizationManager,
			TransactionalMessenger transactionalMessenger) {
		this.verificationDao = verificationDao;
		this.userProfileManager = userProfileManager;
		this.fileHandleManager = fileHandleManager;
		this.principalAliasDAO = principalAliasDAO;
		this.authorizationManager = authorizationManager;
		this.transactionalMessenger = transactionalMessenger;
	}

	@WriteTransaction
	@Override
	public VerificationSubmission createVerificationSubmission(
			UserInfo userInfo, VerificationSubmission verificationSubmission) {
		// check whether there is already an active (submitted or approved) verification submission
		VerificationSubmission current = verificationDao.getCurrentVerificationSubmissionForUser(userInfo.getId());
		if (current!=null) {
			List<VerificationState> states = current.getStateHistory();
			VerificationStateEnum state = states.get(states.size()-1).getState();
			if (state==VerificationStateEnum.SUBMITTED) {
				throw new UnauthorizedException("A verification request has already been submitted.");
			} else if (state==VerificationStateEnum.APPROVED) {
				throw new UnauthorizedException("You are already verified.");
			}
		}
		populateCreateFields(verificationSubmission, userInfo, new Date());
		validateVerificationSubmission(verificationSubmission, 
				userProfileManager.getUserProfile(userInfo.getId().toString()),
				getOrcid(userInfo.getId()));
		// 		User must be owner of file handle Ids
		if (verificationSubmission.getAttachments()!=null) {
			Set<String> fileHandleIds = new HashSet<>();
			for (AttachmentMetadata attachmentMetadata : verificationSubmission.getAttachments()) {
				String fileHandleId = attachmentMetadata.getId();
				if (! fileHandleIds.add(fileHandleId)) {
					throw new IllegalArgumentException("Duplicate file handle: " + fileHandleId);
				}
				// this will throw an UnauthorizedException if the user is not the owner
				FileHandle fileHandle = fileHandleManager.getRawFileHandle(userInfo, fileHandleId);
				// now fill in the file metadata
				attachmentMetadata.setFileName(fileHandle.getFileName());
			}
		}
		transactionalMessenger.sendMessageAfterCommit(userInfo.getId().toString(), ObjectType.VERIFICATION_SUBMISSION, "etag", ChangeType.CREATE);
		return verificationDao.createVerificationSubmission(verificationSubmission);
	}
	
	public static void populateCreateFields(VerificationSubmission verificationSubmission, UserInfo userInfo, Date now) {
		verificationSubmission.setCreatedBy(userInfo.getId().toString());
		verificationSubmission.setCreatedOn(now);
		verificationSubmission.setStateHistory(null);
	}
	
	private String getOrcid(Long userId) {
		List<PrincipalAlias> orcidAliases = principalAliasDAO.listPrincipalAliases(userId, AliasType.USER_ORCID);
		if (orcidAliases.size()>1) throw new IllegalStateException("Cannot have multiple ORCIDs.");
		return orcidAliases.isEmpty() ? null : orcidAliases.get(0).getAlias();
	}
	
	// Validate the content:
	// 		Content must match user profile, emails, ORCID in system at the time the request is made.
	//		Rejected if required fields are blank.
	public static void validateVerificationSubmission(
			VerificationSubmission verificationSubmission, 
			UserProfile userProfile, 
			String orcId) {
		validateField(verificationSubmission.getFirstName(), userProfile.getFirstName(), "First name");
		validateField(verificationSubmission.getLastName(), userProfile.getLastName(), "Last name");
		validateField(verificationSubmission.getCompany(), userProfile.getCompany(), "Company");
		validateField(verificationSubmission.getLocation(), userProfile.getLocation(), "Location");
		validateField(AliasUtils.getUniqueAliasName(verificationSubmission.getOrcid()), 
					AliasUtils.getUniqueAliasName(orcId), "ORCID");
		if (verificationSubmission.getEmails()==null || verificationSubmission.getEmails().isEmpty()) 
			throw new InvalidModelException("Email(s) cannot be omitted.");
		Set<String> verificationEmailSet = new HashSet<String>(verificationSubmission.getEmails());
		Set<String> userProfileEmailSet = new HashSet<String>(userProfile.getEmails());
		if (!verificationEmailSet.equals(userProfileEmailSet))
			throw new InvalidModelException("Email(s) in submission must match those registered with the system.");
	}
	
	public static void validateField(String submissionField, String userProfileField, String fieldName) {
		if (submissionField==null) throw new InvalidModelException(fieldName+" cannot be null.");
		if (!submissionField.equals(userProfileField)) throw new InvalidModelException(fieldName+" does not match value in user profile.");
	}

	@Override
	public void deleteVerificationSubmission(UserInfo userInfo, Long verificationId) {
		if (!userInfo.isAdmin() && 
				userInfo.getId()!=verificationDao.getVerificationSubmitter(verificationId))
			throw new UnauthorizedException("Only the creator of a verification submission may delete it.");
		verificationDao.deleteVerificationSubmission(verificationId);
	}
	
	@Override
	public VerificationPagedResults listVerificationSubmissions(
			UserInfo userInfo, List<VerificationStateEnum> currentVerificationState,
			Long verifiedUserId, long limit, long offset) {
		// check that user is in ACT (or an admin)
		if(!authorizationManager.isACTTeamMemberOrAdmin(userInfo))
			throw new UnauthorizedException("You are not a member of the Synapse Access and Compliance Team.");
		List<VerificationSubmission>  list = verificationDao.listVerificationSubmissions(currentVerificationState, verifiedUserId, limit, offset);
		long totalNumberOfResults = verificationDao.countVerificationSubmissions(currentVerificationState, verifiedUserId);
		VerificationPagedResults result = new VerificationPagedResults();
		result.setResults(list);
		result.setTotalNumberOfResults(totalNumberOfResults);
		return result;
	}

	@WriteTransaction
	@Override
	public void changeSubmissionState(UserInfo userInfo,
			long verificationSubmissionId, VerificationState newState) {
		// check that user is in ACT (or an admin)
		if(!authorizationManager.isACTTeamMemberOrAdmin(userInfo))
			throw new UnauthorizedException("You are not a member of the Synapse Access and Compliance Team.");
		// check that the state transition is allowed, by comparing to the current state
		VerificationStateEnum currentState = verificationDao.getVerificationState(verificationSubmissionId);
		if (!isStateTransitionAllowed(currentState, newState.getState()))
			throw new InvalidModelException("Cannot transition verification submission from "+currentState+" to "+newState.getState());
		populateCreateFields(newState, userInfo, new Date());
		verificationDao.appendVerificationSubmissionState(verificationSubmissionId, newState);
		transactionalMessenger.sendMessageAfterCommit(userInfo.getId().toString(), ObjectType.VERIFICATION_SUBMISSION, "etag", ChangeType.UPDATE);
	}
	
	public static void populateCreateFields(VerificationState state, UserInfo userInfo, Date now) {
		state.setCreatedBy(userInfo.getId().toString());
		state.setCreatedOn(now);
	}

	
	public static boolean isStateTransitionAllowed(VerificationStateEnum currentState, VerificationStateEnum newState) {
		switch (currentState) {
		case SUBMITTED:
			return newState==APPROVED || newState==REJECTED;
		case APPROVED:
			return newState==SUSPENDED;
		case REJECTED:
			return false;
		case SUSPENDED:
			return false;
		default:
			throw new InvalidModelException("Unexpected state "+currentState);
		}
	}

	@Override
	public List<MessageToUserAndBody> createSubmissionNotification(
			VerificationSubmission verificationSubmission,
			String notificationUnsubscribeEndpoint) {
		Map<String,String> fieldValues = new HashMap<String,String>();
		UserProfile submitterUserProfile = userProfileManager.getUserProfile(verificationSubmission.getCreatedBy());
		String submitterDisplayName = EmailUtils.getDisplayNameWithUsername(submitterUserProfile);
		fieldValues.put(TEMPLATE_KEY_DISPLAY_NAME, submitterDisplayName);
		fieldValues.put(TEMPLATE_KEY_USER_ID, verificationSubmission.getCreatedBy());
		String recipient = TeamConstants.ACT_TEAM_ID.toString();
		String messageContent = EmailUtils.readMailTemplate(VERIFICATION_SUBMISSION_TEMPLATE, fieldValues);
		MessageToUser mtu = new MessageToUser();
		mtu.setSubject(VERIFICATION_NOTIFICATION_SUBJECT);
		mtu.setRecipients(Collections.singleton(recipient));
		
		List<PrincipalAlias> actTeamAliases = principalAliasDAO.listPrincipalAliases(TeamConstants.ACT_TEAM_ID, AliasType.TEAM_NAME);
		// should just be one
		if (actTeamAliases.size()!=1) throw new IllegalStateException("Expected one but found "+actTeamAliases.size());
		String actName = actTeamAliases.get(0).getAlias();
		mtu.setTo(EmailUtils.getEmailAddressForPrincipalName(actName));
		mtu.setNotificationUnsubscribeEndpoint(notificationUnsubscribeEndpoint);
		return Collections.singletonList(new MessageToUserAndBody(
				mtu, messageContent, ContentType.TEXT_HTML.getMimeType()));
	}

	@Override
	public List<MessageToUserAndBody> createStateChangeNotification(
			long verificationSubmissionId, VerificationState newState,
			String notificationUnsubscribeEndpoint) {
		String submitterId = new Long(verificationDao.getVerificationSubmitter(verificationSubmissionId)).toString();
		Map<String,String> fieldValues = new HashMap<String,String>();
		UserProfile submitterUserProfile = userProfileManager.getUserProfile(submitterId);
		String submitterDisplayName = EmailUtils.getDisplayNameWithUsername(submitterUserProfile);
		fieldValues.put(TEMPLATE_KEY_DISPLAY_NAME, submitterDisplayName);
		fieldValues.put(TEMPLATE_KEY_USER_ID, submitterId);
		fieldValues.put(TEMPLATE_KEY_REASON, newState.getReason());
		
		String templateName;
		switch (newState.getState()) {
		case APPROVED:
			templateName = VERIFICATION_APPROVED_TEMPLATE;
			break;
		case REJECTED:
			if (StringUtils.isEmpty(newState.getReason())) {
				templateName = VERIFICATION_REJECTED_NO_REASON_TEMPLATE;
			} else {
				templateName = VERIFICATION_REJECTED_TEMPLATE;
			}
			break;
		case SUSPENDED:
			if (StringUtils.isEmpty(newState.getReason())) {
				templateName = VERIFICATION_SUSPENDED_NO_REASON_TEMPLATE;
			} else {
				templateName = VERIFICATION_SUSPENDED_TEMPLATE;
			}
			break;
		default:
			throw new IllegalStateException("Unexpected state "+newState.getState());
		}
		String messageContent = EmailUtils.readMailTemplate(templateName, fieldValues);
		
		MessageToUser mtu = new MessageToUser();
		mtu.setSubject(VERIFICATION_NOTIFICATION_SUBJECT);
		mtu.setRecipients(Collections.singleton(submitterId));
		mtu.setNotificationUnsubscribeEndpoint(notificationUnsubscribeEndpoint);
		return Collections.singletonList(new MessageToUserAndBody(
				mtu, messageContent, ContentType.TEXT_HTML.getMimeType()));
	}

}
