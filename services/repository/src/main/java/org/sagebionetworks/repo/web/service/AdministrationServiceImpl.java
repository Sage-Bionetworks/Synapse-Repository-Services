package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.doi.DoiAdminManager;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.manager.message.MessageSyndication;
import org.sagebionetworks.repo.manager.password.PasswordValidator;
import org.sagebionetworks.repo.manager.stack.StackStatusManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSessionToken;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.feature.FeatureStatus;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.sagebionetworks.repo.model.migration.IdGeneratorExport;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

/**
 * This controller is used for Administration of Synapse.
 *
 * @author John
 */
public class AdministrationServiceImpl implements AdministrationService  {

	static private Logger log = LogManager.getLogger(AdministrationServiceImpl.class);
	
	@Autowired
	private ObjectTypeSerializer objectTypeSerializer;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private StackStatusManager stackStatusManager;
	
	@Autowired
	private MessageSyndication messageSyndication;
	
	@Autowired
	private DoiAdminManager doiAdminManager;
	
	@Autowired
	SemaphoreManager semaphoreManager;

	@Autowired
	TableManagerSupport tableManagerSupport;

	@Autowired
	private DBOChangeDAO changeDAO;
	
	@Autowired
	IdGenerator idGenerator;

	@Autowired
	TransactionSynchronizationProxy transactionSynchronizationManager;

	@Autowired
	PasswordValidator passwordValidator;
	
	@Autowired
	FeatureManager featureManager;

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
			HttpHeaders header,	HttpServletRequest request) 
			throws DatastoreException, NotFoundException, UnauthorizedException, IOException {

		// Get the status of this daemon
		StackStatus updatedValue = objectTypeSerializer.deserialize(request.getInputStream(), header, StackStatus.class, header.getContentType());
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
	public EntityId createOrGetTestUser(Long userId, NewIntegrationTestUser userSpecs) throws NotFoundException {
		adminCheck(userId);
		UserInfo userInfo = userManager.getUserInfo(userId);
		
		DBOCredential cred = new DBOCredential();
		DBOTermsOfUseAgreement touAgreement = null;
		DBOSessionToken token = null;
		if (userSpecs.getPassword() != null) {
			passwordValidator.validatePassword(userSpecs.getPassword());
			cred.setPassHash(PBKDF2Utils.hashPassword(userSpecs.getPassword(), null));
		}
		if (userSpecs.getSession() != null) {
			touAgreement = new DBOTermsOfUseAgreement();
			touAgreement.setAgreesToTermsOfUse(userSpecs.getSession().getAcceptsTermsOfUse());

			token = new DBOSessionToken();
			token.setSessionToken(userSpecs.getSession().getSessionToken());
		}
		
		NewUser nu = new NewUser();
		nu.setEmail(userSpecs.getEmail());
		nu.setUserName(userSpecs.getUsername());
		UserInfo user = userManager.createOrGetTestUser(userInfo, nu, cred, touAgreement, token);
		
		EntityId id = new EntityId();
		id.setId(user.getId().toString());
		return id;
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


}
