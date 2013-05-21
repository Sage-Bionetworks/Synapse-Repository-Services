package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.StackStatusManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDaemonLauncher;
import org.sagebionetworks.repo.manager.backup.migration.DependencyManager;
import org.sagebionetworks.repo.manager.doi.DoiAdminManager;
import org.sagebionetworks.repo.manager.dynamo.DynamoAdminManager;
import org.sagebionetworks.repo.manager.message.MessageSyndication;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MigratableObjectCount;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.BackupSubmission;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

/**
 * This controller is used for Administration of Synapse.
 *
 * @author John
 */
public class AdministrationServiceImpl implements AdministrationService  {
	
	@Autowired
	private BackupDaemonLauncher backupDaemonLauncher;	
	@Autowired
	ObjectTypeSerializer objectTypeSerializer;	
	@Autowired
	UserManager userManager;
	@Autowired
	StackStatusManager stackStatusManager;	
	@Autowired
	DependencyManager dependencyManager;
	@Autowired
	MessageSyndication messageSyndication;
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
			DependencyManager dependencyManager,
			MessageSyndication messageSyndication) {
		super();
		this.backupDaemonLauncher = backupDaemonLauncher;
		this.objectTypeSerializer = objectTypeSerializer;
		this.userManager = userManager;
		this.stackStatusManager = stackStatusManager;
		this.dependencyManager = dependencyManager;
		this.messageSyndication = messageSyndication;
	}

	@Override
	public PaginatedResults<MigratableObjectData> getAllBackupObjects(
			String userId, Integer offset, Integer limit, Boolean  includeDependencies)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
		QueryResults<MigratableObjectData> queryResults = dependencyManager.getAllObjects(offset, limit, includeDependencies);
		PaginatedResults<MigratableObjectData> result = new PaginatedResults<MigratableObjectData>();
		result.setResults(queryResults.getResults());
		result.setTotalNumberOfResults(queryResults.getTotalNumberOfResults());
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AdministrationService#getAllBackupObjectsCounts(java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.Boolean)
	 */
	@Override
	public PaginatedResults<MigratableObjectCount> getAllBackupObjectsCounts(String userId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
		QueryResults<MigratableObjectCount> queryResults = dependencyManager.getAllObjectsCounts();
		PaginatedResults<MigratableObjectCount> result = new PaginatedResults<MigratableObjectCount>();
		result.setResults(queryResults.getResults());
		result.setTotalNumberOfResults(queryResults.getTotalNumberOfResults());
		return result;
	}
		
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AdministrationService#startBackup(java.lang.String, java.lang.String, org.springframework.http.HttpHeaders, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public BackupRestoreStatus startBackup(String userId, String type, 
			HttpHeaders header,	HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {
		
		// The BackupSubmission is optional.  When included we will only backup the entity Ids included.
		// When the body is null all entities will be backed up.
		Set<String> entityIdsToBackup = null;
		if(request.getInputStream() != null){
			BackupSubmission submission = objectTypeSerializer.deserialize(request.getInputStream(), header,BackupSubmission.class, header.getContentType());
			entityIdsToBackup = submission.getEntityIdsToBackup();
		}
		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		// start a backup daemon
		// This is a full system backup so 
		return backupDaemonLauncher.startBackup(userInfo, entityIdsToBackup, MigratableObjectType.valueOf(type));
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AdministrationService#startRestore(org.sagebionetworks.repo.model.daemon.RestoreSubmission, java.lang.String, java.lang.String, org.springframework.http.HttpHeaders, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public BackupRestoreStatus startRestore(RestoreSubmission file, 
			String userId, String type, HttpHeaders header,	HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {

		if(file == null) throw new IllegalArgumentException("File cannot be null");
		if(file.getFileName() == null) throw new IllegalArgumentException("File.getFileName cannot be null");
		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		// start a restore daemon
		return backupDaemonLauncher.startRestore(userInfo, file.getFileName(), MigratableObjectType.valueOf(type));
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AdministrationService#deleteMigratableObject(java.lang.String, java.lang.String, java.lang.String, org.springframework.http.HttpHeaders, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public void deleteMigratableObject(String userId, String objectId, 
			String type, HttpHeaders header,	HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {

		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		MigratableObjectDescriptor mod = new MigratableObjectDescriptor();
		mod.setId(objectId);
		mod.setType(MigratableObjectType.valueOf(type));
		// start a restore daemon
		backupDaemonLauncher.delete(userInfo, mod);
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AdministrationService#getStatus(java.lang.String, java.lang.String, org.springframework.http.HttpHeaders, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public BackupRestoreStatus getStatus(String daemonId, String userId,
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
	public void terminateDaemon(String daemonId, String userId,
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
	public StackStatus getStackStatus(String userId, HttpHeaders header,
			HttpServletRequest request) {

		// Get the status of this daemon
		return stackStatusManager.getCurrentStatus();
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AdministrationService#updateStatusStackStatus(java.lang.String, org.springframework.http.HttpHeaders, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public StackStatus updateStatusStackStatus(String userId,
			HttpHeaders header,	HttpServletRequest request) 
			throws DatastoreException, NotFoundException, UnauthorizedException, IOException {

		// Get the status of this daemon
		StackStatus updatedValue = objectTypeSerializer.deserialize(request.getInputStream(), header, StackStatus.class, header.getContentType());
		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return stackStatusManager.updateStatus(userInfo, updatedValue);
	}

	@Override
	public ChangeMessages listChangeMessages(String userId, Long startChangeNumber, ObjectType type, Long limit) throws DatastoreException, NotFoundException {
		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
		return messageSyndication.listChanges(startChangeNumber, type, limit);
	}

	@Override
	public PublishResults rebroadcastChangeMessagesToQueue(String userId, String queueName, Long startChangeNumber, ObjectType type, Long limit) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
		return messageSyndication.rebroadcastChangeMessagesToQueue(queueName, type, startChangeNumber, limit);
	}

	@Override
	public FireMessagesResult reFireChangeMessages(String userId,  Long startChangeNumber, Long limit) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
		long lastMsgNum = messageSyndication.rebroadcastChangeMessages(startChangeNumber, limit);
		FireMessagesResult res = new FireMessagesResult();
		res.setNextChangeNumber(lastMsgNum);
		return res;
	}

	@Override
	public void clearDoi(String userId) throws NotFoundException, UnauthorizedException, DatastoreException {
		doiAdminManager.clear(userId);
	}

	@Override
	public FireMessagesResult getCurrentChangeNumber(String userId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
		long lastChgNum = messageSyndication.getCurrentChangeNumber();
		FireMessagesResult res = new FireMessagesResult();
		res.setNextChangeNumber(lastChgNum);
		return res;
	}

	@Override
	public void clearDynamoTable(String userId, String tableName,
			String hashKeyName, String rangeKeyName) throws NotFoundException,
			UnauthorizedException, DatastoreException {
		dynamoAdminManager.clear(userId, tableName, hashKeyName, rangeKeyName);
	}
}
