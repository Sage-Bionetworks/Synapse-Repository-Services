package org.sagebionetworks.repo.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.doi.DoiAdminManager;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.manager.message.MessageSyndication;
import org.sagebionetworks.repo.manager.password.PasswordValidator;
import org.sagebionetworks.repo.manager.stack.StackStatusManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.admin.ExpireQuarantinedEmailRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.ses.EmailQuarantineDao;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAO;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.feature.FeatureStatus;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.migration.IdGeneratorExport;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This controller is used for Administration of Synapse.
 *
 * @author John
 */
@Service
public class AdministrationServiceImpl implements AdministrationService  {
	
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private StackStatusManager stackStatusManager;
	
	@Autowired
	private MessageSyndication messageSyndication;
	
	@Autowired
	private DoiAdminManager doiAdminManager;
	
	@Autowired
	private SemaphoreManager semaphoreManager;

	@Autowired
	private TableManagerSupport tableManagerSupport;

	@Autowired
	private DBOChangeDAO changeDAO;
	
	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private PasswordValidator passwordValidator;
	
	@Autowired
	private FeatureManager featureManager;

	@Autowired
	private VerificationDAO verificationDao;
	
	@Autowired
	private EmailQuarantineDao emailQuarantineDao;

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AdministrationService#getStackStatus(java.lang.String, org.springframework.http.HttpHeaders, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public StackStatus getStackStatus() {
		// Get the status of this daemon
		return stackStatusManager.getCurrentStatus();
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AdministrationService#updateStatusStackStatus(java.lang.String, org.springframework.http.HttpHeaders, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public StackStatus updateStatusStackStatus(Long userId,
			StackStatus updatedValue) 
			throws DatastoreException, NotFoundException, UnauthorizedException, IOException {

		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return stackStatusManager.updateStatus(userInfo, updatedValue);
	}

	@Override
	public ChangeMessages listChangeMessages(Long userId, Long startChangeNumber, ObjectType type, Long limit) throws DatastoreException, NotFoundException {
		adminCheck(userId);
		return messageSyndication.listChanges(startChangeNumber, type, limit);
	}

	@Override
	public PublishResults rebroadcastChangeMessagesToQueue(Long userId, String queueName, Long startChangeNumber, ObjectType type, Long limit) throws DatastoreException, NotFoundException {
		adminCheck(userId);
		return messageSyndication.rebroadcastChangeMessagesToQueue(queueName, type, startChangeNumber, limit);
	}

	@Override
	public FireMessagesResult reFireChangeMessages(Long userId,  Long startChangeNumber, Long limit) throws DatastoreException, NotFoundException {
		adminCheck(userId);
		long lastMsgNum = messageSyndication.rebroadcastChangeMessages(startChangeNumber, limit);
		FireMessagesResult res = new FireMessagesResult();
		res.setNextChangeNumber(lastMsgNum);
		return res;
	}

	void adminCheck(Long userId) {
		ValidateArgument.required(userId, "userid");
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Only an administrator may access this service.");
		}
	}

	@Override
	public void clearDoi(Long userId) throws NotFoundException, UnauthorizedException, DatastoreException {
		doiAdminManager.clear(userId);
	}

	@Override
	public FireMessagesResult getCurrentChangeNumber(Long userId) throws DatastoreException, NotFoundException {
		adminCheck(userId);
		long lastChgNum = messageSyndication.getCurrentChangeNumber();
		FireMessagesResult res = new FireMessagesResult();
		res.setNextChangeNumber(lastChgNum);
		return res;
	}
	
	@Override
	public LoginResponse createOrGetTestUser(Long userId, NewIntegrationTestUser userSpecs) throws NotFoundException {
		adminCheck(userId);
		UserInfo userInfo = userManager.getUserInfo(userId);
		
		if (userSpecs.getPassword() != null) {
			passwordValidator.validatePassword(userSpecs.getPassword());
		}
		
		NewUser nu = new NewUser();
		nu.setEmail(userSpecs.getEmail());
		nu.setUserName(userSpecs.getUsername());
		
		// If null, do not sign
		boolean signTermsOfService = Boolean.TRUE.equals(userSpecs.getTou());
		
		UserInfo createdUser = userManager.createOrGetTestUser(userInfo, nu, userSpecs.getPassword(), signTermsOfService);

		if (Boolean.TRUE.equals(userSpecs.getValidatedUser())) {
			VerificationSubmission submission = new VerificationSubmission()
				.setCreatedBy(createdUser.getId().toString())
				.setCreatedOn(Date.from(Instant.now()));
			
			submission = verificationDao.createVerificationSubmission(submission);
			verificationDao.appendVerificationSubmissionState(Long.parseLong(submission.getId()), 
				new VerificationState()
					.setCreatedBy(userId.toString())
					.setCreatedOn(Date.from(Instant.now()))
					.setState(VerificationStateEnum.APPROVED));
		}
		
		return authManager.loginWithNoPasswordCheck(createdUser.getId(), null);
	}
	
	@Override
	public void deleteUser(Long userId, String id) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		userManager.deletePrincipal(userInfo, Long.parseLong(id));
	}

	@Override
	public void rebuildTable(Long userId, String tableId) throws NotFoundException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		tableManagerSupport.rebuildTable(userInfo, idAndVersion);
	}

	@Override
	public void clearAllLocks(Long userId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Ony an admin can make this call
		semaphoreManager.releaseAllLocksAsAdmin(userInfo);
	}

	@Override
	public ChangeMessages createOrUpdateChangeMessages(Long userId,
			ChangeMessages batch) throws UnauthorizedException, NotFoundException {
		adminCheck(userId);
		ChangeMessages messages = new ChangeMessages();
		messages.setList(changeDAO.replaceChange(batch.getList()));
		return messages;
	}

	@Override
	public IdGeneratorExport createIdGeneratorExport(Long userId) {
		adminCheck(userId);
		String script = idGenerator.createRestoreScript();
		IdGeneratorExport export = new IdGeneratorExport();
		export.setExportScript(script);
		return export;
	}
	
	@Override
	public FeatureStatus getFeatureStatus(Long userId, Feature feature) {
		UserInfo user = userManager.getUserInfo(userId);
		return featureManager.getFeatureStatus(user, feature);
	}
	
	@Override
	public FeatureStatus setFeatureStatus(Long userId, Feature feature, FeatureStatus status) {
		UserInfo user = userManager.getUserInfo(userId);
		return featureManager.setFeatureStatus(user, feature, status);
	};
	
	@Override
	public LoginResponse getUserAccessToken(Long userId, Long targetUserId) {
		ValidateArgument.required(targetUserId, "The targetUserId");
		adminCheck(userId);
		return authManager.loginWithNoPasswordCheck(targetUserId, null);
	}
	
	@Override
	public void expireQuarantinedEmail(Long userId, ExpireQuarantinedEmailRequest request) {
		ValidateArgument.required(request, "The request");
		ValidateArgument.requiredNotBlank(request.getEmail(), "The request.email");
		
		adminCheck(userId);
		
		emailQuarantineDao.expireQuarantinedEmail(request.getEmail());		
	}

}
