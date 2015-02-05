package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.StackStatusManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDaemonLauncher;
import org.sagebionetworks.repo.manager.doi.DoiAdminManager;
import org.sagebionetworks.repo.manager.dynamo.DynamoAdminManager;
import org.sagebionetworks.repo.manager.message.MessageSyndication;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.manager.table.TableRowManager;
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
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSessionToken;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
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
	
	@Autowired
	SemaphoreManager semaphoreManager;

	@Autowired
	private TableRowManager tableRowManager;

	@Autowired
	private ConnectionFactory tableConnectionFactory;

	@Autowired
	private RepositoryMessagePublisher repositoryMessagePublisher;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private DBOChangeDAO changeDAO;

	@Autowired
	private TableStatusDAO tableStatusDAO;

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
			MessageSyndication messageSyndication,
			DBOChangeDAO changeDAO) {
		super();
		this.backupDaemonLauncher = backupDaemonLauncher;
		this.objectTypeSerializer = objectTypeSerializer;
		this.userManager = userManager;
		this.stackStatusManager = stackStatusManager;
		this.messageSyndication = messageSyndication;
		this.changeDAO = changeDAO;
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
			Date date = new Date();

			touAgreement = new DBOTermsOfUseAgreement();
			touAgreement.setDomain(DomainType.SYNAPSE);
			touAgreement.setAgreesToTermsOfUse(userSpecs.getSession().getAcceptsTermsOfUse());

			token = new DBOSessionToken();
			token.setSessionToken(userSpecs.getSession().getSessionToken());
			token.setValidatedOn(date);
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

	@Override
	public void rebuildTable(Long userId, String tableId) throws NotFoundException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin())
			throw new UnauthorizedException("Only an administrator may access this service.");
		// purge
		tableRowManager.removeCaches(tableId);
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(tableId);
		if (indexDao != null) {
			indexDao.deleteTable(tableId);
			indexDao.deleteStatusTable(tableId);
		}
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableId);
		TableEntity tableEntity = entityManager.getEntity(userInfo, tableId, TableEntity.class);
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.UPDATE);
		message.setObjectType(ObjectType.TABLE);
		message.setObjectId(KeyFactory.stringToKey(tableId).toString());
		message.setObjectEtag(resetToken);
		message.setParentId(KeyFactory.stringToKey(tableEntity.getParentId()).toString());
		message = changeDAO.replaceChange(message);

		// and send out update message
		repositoryMessagePublisher.fireChangeMessage(message);
	}

	@Override
	public void addIndexesToTable(Long userId, String tableId) throws NotFoundException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin())
			throw new UnauthorizedException("Only an administrator may access this service.");
		// purge
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(tableId);
		if (indexDao != null) {
			indexDao.addIndexes(tableId);
		}
	}

	public void removeIndexesFromTable(Long userId, String tableId) throws NotFoundException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin())
			throw new UnauthorizedException("Only an administrator may access this service.");
		// purge
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(tableId);
		if (indexDao != null) {
			indexDao.removeIndexes(tableId);
		}
	}

	@Override
	public void clearAllLocks(Long userId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Ony an admin can make this call
		semaphoreManager.releaseAllLocksAsAdmin(userInfo);
	}

	private static final Object waitObject = new Object();
	@Override
	public void waitForTesting(Long userId, boolean release) throws Exception {
		if (StackConfiguration.isProductionStack()) {
			throw new UnauthorizedException("Should never be called on production stack.");
		}
		if (release) {
			synchronized (waitObject) {
				waitObject.notifyAll();
			}
		} else {
			UserInfo userInfo = userManager.getUserInfo(userId);
			if (!userInfo.isAdmin()) {
				throw new UnauthorizedException("Only an administrator may access this service.");
			}
			synchronized (waitObject) {
				waitObject.wait(30000);
			}
		}
	}

	@Override
	public ChangeMessages createOrUpdateChangeMessages(Long userId,
			ChangeMessages batch) throws UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Only an administrator may access this service.");
		}
		ChangeMessages messages = new ChangeMessages();
		messages.setList(changeDAO.replaceChange(batch.getList()));
		return messages;
	}
}
