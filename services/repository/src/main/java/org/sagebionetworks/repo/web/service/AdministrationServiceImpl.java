package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.StackStatusManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDaemonLauncher;
import org.sagebionetworks.repo.manager.doi.DoiAdminManager;
import org.sagebionetworks.repo.manager.dynamo.DynamoAdminManager;
import org.sagebionetworks.repo.manager.message.MessageSyndication;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.ObjectType;
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
