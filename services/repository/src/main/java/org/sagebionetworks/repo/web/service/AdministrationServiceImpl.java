package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.StackStatusManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDaemonLauncher;
import org.sagebionetworks.repo.manager.doi.DoiAdminManager;
import org.sagebionetworks.repo.manager.dynamo.DynamoAdminManager;
import org.sagebionetworks.repo.manager.message.MessageSyndication;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSessionToken;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

/**
 * This controller is used for Administration of Synapse.
 *
 * @author John
 */
public class AdministrationServiceImpl implements AdministrationService  {

	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private BackupDaemonLauncher backupDaemonLauncher;	
	
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
	private DynamoAdminManager dynamoAdminManager;

	/**
	 * Spring will use this constructor
	 */
	public AdministrationServiceImpl(){
		
	}
	/**
	 * IoC constructor
	 * 
	 * @param backupDaemonLauncher
	 * @param objectTypeSerializer
	 * @param userManager
	 * @param stackStatusManager
	 * @param dependencyManager
	 * @param messageSyndication
	 */
	public AdministrationServiceImpl(BackupDaemonLauncher backupDaemonLauncher,
			ObjectTypeSerializer objectTypeSerializer, UserManager userManager,
			StackStatusManager stackStatusManager,
			MessageSyndication messageSyndication) {
		super();
		this.backupDaemonLauncher = backupDaemonLauncher;
		this.objectTypeSerializer = objectTypeSerializer;
		this.userManager = userManager;
		this.stackStatusManager = stackStatusManager;
		this.messageSyndication = messageSyndication;
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AdministrationService#getStatus(java.lang.String, java.lang.String, org.springframework.http.HttpHeaders, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public BackupRestoreStatus getStatus(String daemonId, Long userId,
			HttpHeaders header,	HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {

		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Get the status of this daemon
		return backupDaemonLauncher.getStatus(userInfo, daemonId);
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AdministrationService#terminateDaemon(java.lang.String, java.lang.String, org.springframework.http.HttpHeaders, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public void terminateDaemon(String daemonId, Long userId,
			HttpHeaders header,	HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {

		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Terminate the daemon
		backupDaemonLauncher.terminate(userInfo, daemonId);
	}
	
	
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
		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
		return messageSyndication.listChanges(startChangeNumber, type, limit);
	}

	@Override
	public PublishResults rebroadcastChangeMessagesToQueue(Long userId, String queueName, Long startChangeNumber, ObjectType type, Long limit) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
		return messageSyndication.rebroadcastChangeMessagesToQueue(queueName, type, startChangeNumber, limit);
	}

	@Override
	public FireMessagesResult reFireChangeMessages(Long userId,  Long startChangeNumber, Long limit) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
		long lastMsgNum = messageSyndication.rebroadcastChangeMessages(startChangeNumber, limit);
		FireMessagesResult res = new FireMessagesResult();
		res.setNextChangeNumber(lastMsgNum);
		return res;
	}

	@Override
	public void clearDoi(Long userId) throws NotFoundException, UnauthorizedException, DatastoreException {
		doiAdminManager.clear(userId);
	}

	@Override
	public FireMessagesResult getCurrentChangeNumber(Long userId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
		long lastChgNum = messageSyndication.getCurrentChangeNumber();
		FireMessagesResult res = new FireMessagesResult();
		res.setNextChangeNumber(lastChgNum);
		return res;
	}

	@Override
	public void clearDynamoTable(Long userId, String tableName,
			String hashKeyName, String rangeKeyName) throws NotFoundException,
			UnauthorizedException, DatastoreException {
		dynamoAdminManager.clear(userId, tableName, hashKeyName, rangeKeyName);
	}
	
	@Override
	public EntityId createTestUser(Long userId, NewIntegrationTestUser userSpecs) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		
		DBOCredential cred = new DBOCredential();
		DBOTermsOfUseAgreement touAgreement = null;
		DBOSessionToken token = null;
		if (userSpecs.getPassword() != null) {
			cred.setPassHash(PBKDF2Utils.hashPassword(userSpecs.getPassword(), null));
		}
		if (userSpecs.getSession() != null) {
			cred.setSessionToken(userSpecs.getSession().getSessionToken());
			cred.setAgreesToTermsOfUse(userSpecs.getSession().getAcceptsTermsOfUse());
			cred.setValidatedOn(new Date());

			touAgreement = new DBOTermsOfUseAgreement();
			touAgreement.setDomain(DomainType.SYNAPSE);
			touAgreement.setAgreesToTermsOfUse(userSpecs.getSession().getAcceptsTermsOfUse());

			token = new DBOSessionToken();
			token.setSessionToken(userSpecs.getSession().getSessionToken());
			token.setValidatedOn(new Date());
			token.setDomain(DomainType.SYNAPSE);
		}
		
		NewUser nu = new NewUser();
		nu.setEmail(userSpecs.getEmail());
		nu.setUserName(userSpecs.getUsername());
		UserInfo user = userManager.createUser(userInfo, nu, cred, touAgreement, token);
		
		EntityId id = new EntityId();
		id.setId(user.getId().toString());
		return id;
	}
	@Override
	public void deleteUser(Long userId, String id) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		userManager.deletePrincipal(userInfo, Long.parseLong(id));
	}
}
