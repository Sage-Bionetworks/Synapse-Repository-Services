package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.StackStatusManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDaemonLauncher;
import org.sagebionetworks.repo.manager.doi.DoiAdminManager;
import org.sagebionetworks.repo.manager.message.MessageSyndication;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.admin.FileUpdateRequest;
import org.sagebionetworks.repo.model.admin.FileUpdateResult;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSessionToken;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * This controller is used for Administration of Synapse.
 *
 * @author John
 */
public class AdministrationServiceImpl implements AdministrationService  {

	static private Logger log = LogManager.getLogger(AdministrationServiceImpl.class);
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
	SemaphoreManager semaphoreManager;

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

	@Autowired
	TransactionSynchronizationProxy transactionSynchronizationManager;

	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private NodeDAO nodeDao;
	
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
			touAgreement.setAgreesToTermsOfUse(userSpecs.getSession().getAcceptsTermsOfUse());

			token = new DBOSessionToken();
			token.setSessionToken(userSpecs.getSession().getSessionToken());
			token.setValidatedOn(date);
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
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(tableId);
		if (indexDao != null) {
			indexDao.deleteTable(tableId);
			indexDao.deleteSecondayTables(tableId);
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

	// we want to test throwing exceptions from within a transaction
	@Override
	@WriteTransaction
	public void throwExceptionTransactional(String exception) throws Throwable {
		throwException(exception);
	}

	// we want to test throwing exceptions from within a transaction
	@Override
	@WriteTransaction
	public void doNothing() {
	}

	private static class TransactionSynchronizationStub implements TransactionSynchronization {
		private String exception;

		@Override
		public void suspend() {
		}

		@Override
		public void resume() {
		}

		@Override
		public void flush() {
		}

		@Override
		public void beforeCommit(boolean readOnly) {
			if (exception != null) {
				try {
					String exceptionToThrow = exception;
					exception = null;
					doThrowException(exceptionToThrow);
				} catch (RuntimeException e) {
					throw e;
				} catch (Throwable t) {
					// do nothing (which will make the test fail)
					log.error("Cannot handle non-runtime exceptions here: " + t.getMessage(), t);
				}
			}
		}

		@Override
		public void beforeCompletion() {
		}

		@Override
		public void afterCommit() {
		}

		@Override
		public void afterCompletion(int status) {
		}

		public void setExceptionToThrow(String exception) {
			if (this.exception != null) {
				log.error("Exception already set: " + this.exception);
				this.exception = null; // no exception thrown means test will fail
				return;
			}
			this.exception = exception;
		}
	}

	@Override
	@WriteTransaction
	public void throwExceptionTransactionalBeforeCommit(String exception) {
		List<TransactionSynchronization> currentList = transactionSynchronizationManager.getSynchronizations();
		TransactionSynchronizationStub handler = null;
		for (TransactionSynchronization sync : currentList) {
			if (sync instanceof TransactionSynchronizationStub) {
				handler = (TransactionSynchronizationStub) sync;
				break;
			}
		}
		if (handler == null) {
			handler = new TransactionSynchronizationStub();
			transactionSynchronizationManager.registerSynchronization(handler);
		}
		handler.setExceptionToThrow(exception);
		doNothing();
	}

	@Override
	public void throwException(String exception) throws Throwable {
		doThrowException(exception);
	}

	public static void doThrowException(String exception) throws Throwable {
		try {
			Throwable t = null;
			Class<?> exceptionClass = Class.forName(exception);
			if (exceptionClass == ACLInheritanceException.class) {
				throw new ACLInheritanceException("", "100");
			}
			if (exceptionClass == AsynchJobFailedException.class) {
				throw new AsynchJobFailedException(new AsynchronousJobStatus());
			}
			Constructor<?>[] constructors = exceptionClass.getConstructors();
			for (Constructor<?> constructor : constructors) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				if (parameterTypes.length == 2) {
					if (parameterTypes[0] == String.class && parameterTypes[1] == Throwable.class) {
						t = (Throwable) constructor.newInstance("test exception", null);
						log.info("Throwing test exception", t);
						throw t;
					} else if (parameterTypes[0] == Object.class && parameterTypes[1] == Class.class) {
						t = (Throwable) constructor.newInstance(2, Integer.class);
						log.info("Throwing test exception", t);
						throw t;
					} else if (parameterTypes[0] == String.class && parameterTypes[1] == Class.class) {
						t = (Throwable) constructor.newInstance("test exception", Integer.class);
						log.info("Throwing test exception", t);
						throw t;
					} else if (parameterTypes[0] == Method.class && parameterTypes[1] == Throwable.class) {
						t = (Throwable) constructor.newInstance(null, null);
						log.info("Throwing test exception", t);
						throw t;
					}
				}
			}
			for (Constructor<?> constructor : constructors) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				if (parameterTypes == null || parameterTypes.length == 0) {
					t = (Throwable) exceptionClass.newInstance();
					log.info("Throwing test exception", t);
					throw t;
				}
			}
			for (Constructor<?> constructor : constructors) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				if (parameterTypes.length == 1) {
					if (parameterTypes[0] == String.class) {
						t = (Throwable) constructor.newInstance(new Object[] { "test exception" });
						log.info("Throwing test exception", t);
						throw t;
					}
				}
			}
			for (Constructor<?> constructor : constructors) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				if (parameterTypes.length == 2) {
					t = (Throwable) constructor.newInstance(new Object[] { null, null });
					log.info("Throwing test exception", t);
					throw t;
				}
			}
			for (Constructor<?> constructor : constructors) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				if (parameterTypes.length == 1) {
					t = (Throwable) constructor.newInstance(new Object[] { null });
					log.info("Throwing test exception", t);
					throw t;
				}
			}
		} catch (ClassNotFoundException e) {
			// do nothing (which will make the test fail)
			log.error("Cannot instantiate exception: " + e.getMessage(), e);
		} catch (InstantiationException e) {
			// do nothing (which will make the test fail)
			log.error("Cannot instantiate exception: " + e.getMessage(), e);
		} catch (IllegalAccessException e) {
			// do nothing (which will make the test fail)
			log.error("Cannot instantiate exception: " + e.getMessage(), e);
		} catch (InvocationTargetException e) {
			// do nothing (which will make the test fail)
			log.error("Cannot instantiate exception: " + e.getMessage(), e);
		}
		// no error
		// do nothing (which will make the test fail)
		log.error("No exception could be instantiated: " + exception);
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

	@WriteTransactionReadCommitted
	@Override
	public FileUpdateResult updateFile(Long userId, FileUpdateRequest request) {
		ValidateArgument.required(userId, "userId");
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Only an administrator may access this service.");
		}
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getEntityId(), "entityId");
		ValidateArgument.required(request.getVersion(), "version");
		ValidateArgument.required(request.getEtag(), "etag");
		FileUpdateResult result = new FileUpdateResult();
		result.setIsSuccessful(false);
		try {
			nodeDao.lockNodeAndIncrementEtag(request.getEntityId(), request.getEtag());
			FileEntity toUpdate = entityManager.getEntityForVersion(userInfo, request.getEntityId(), request.getVersion(), FileEntity.class);
			String fileNameOverride = toUpdate.getFileNameOverride();
			if (fileNameOverride == null) {
				result.setReason(request.getEntityId()+"."+request.getVersion() +" does not have fileNameOverride field set.");
				return result;
			}
			String fileHandleId = toUpdate.getDataFileHandleId();

			// make the copy
			FileHandle toCopy = fileHandleDao.get(fileHandleId);
			String newFileHandleId = idGenerator.generateNewId(TYPE.FILE_IDS).toString();
			toCopy.setId(newFileHandleId);
			toCopy.setFileName(fileNameOverride);
			toCopy.setEtag(UUID.randomUUID().toString());
			fileHandleDao.createBatch(Arrays.asList(toCopy));
			// null out fileNameOverride and update entity version's fileHandleId and etag
			NamedAnnotations annotations = nodeDao.getAnnotationsForVersion(request.getEntityId(), request.getVersion());
			annotations.getPrimaryAnnotations().deleteAnnotation("fileNameOverride");
			nodeDao.replaceVersion(request.getEntityId(), request.getVersion(), annotations, newFileHandleId);
		} catch (NotFoundException e) {
			result.setReason(request.getEntityId()+"."+request.getVersion() +" cannot be found.");
			return result;
		} catch (ConflictingUpdateException e2) {
			result.setReason("Cannot get a lock for "+request.getEntityId()+"."+request.getVersion());
			return result;
		}
		result.setIsSuccessful(true);
		return result;
	}
}
