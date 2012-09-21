package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.StackStatusManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDaemonLauncher;
import org.sagebionetworks.repo.manager.backup.migration.DependencyManager;
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
	
	@Override
	public PaginatedResults<MigratableObjectCount> getAllBackupObjectsCounts(
			String userId, Integer offset, Integer limit, Boolean  includeDependencies)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
		QueryResults<MigratableObjectCount> queryResults = dependencyManager.getAllObjectsCounts(offset, limit, includeDependencies);
		PaginatedResults<MigratableObjectCount> result = new PaginatedResults<MigratableObjectCount>();
		result.setResults(queryResults.getResults());
		result.setTotalNumberOfResults(queryResults.getTotalNumberOfResults());
		return result;
	}
		
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

}
