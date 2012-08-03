package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.StackStatusManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDaemonLauncher;
import org.sagebionetworks.repo.manager.backup.migration.DependencyManager;
import org.sagebionetworks.repo.model.DatastoreException;
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

public class AdministrationServiceImpl implements AdministrationService {
	
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
	

	public PaginatedResults<MigratableObjectData> getAllBackupObjects(String userId, Integer offset, Integer limit, Boolean includeDependencies) throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
		QueryResults<MigratableObjectData> queryResults = dependencyManager.getAllObjects(offset, limit, includeDependencies);
		PaginatedResults<MigratableObjectData> result = new PaginatedResults<MigratableObjectData>();
		result.setResults(queryResults.getResults());
		result.setTotalNumberOfResults(queryResults.getTotalNumberOfResults());
		return result;
		
	}
	
	public BackupRestoreStatus startBackup(String userId, String type, HttpHeaders header, HttpServletRequest request) throws IOException, DatastoreException, NotFoundException, UnauthorizedException {
		
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
	
	public BackupRestoreStatus startRestore(String userId, RestoreSubmission file, String type) throws DatastoreException, NotFoundException, UnauthorizedException {
		if(file == null) throw new IllegalArgumentException("File cannot be null");
		if(file.getFileName() == null) throw new IllegalArgumentException("File.getFileName cannot be null");
		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		// start a restore daemon
		return backupDaemonLauncher.startRestore(userInfo, file.getFileName(), MigratableObjectType.valueOf(type));
	}
	
	public void deleteMigratableObject(String userId, String objectId, String type) throws UnauthorizedException, DatastoreException, NotFoundException {

		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		MigratableObjectDescriptor mod = new MigratableObjectDescriptor();
		mod.setId(objectId);
		mod.setType(MigratableObjectType.valueOf(type));
		// start a restore daemon
		backupDaemonLauncher.delete(userInfo, mod);		
	}
	 
	public BackupRestoreStatus startSearchDocument(String userId, HttpHeaders header, HttpServletRequest request) throws UnauthorizedException, DatastoreException, IOException, NotFoundException {
		
		// The BackupSubmission is optional.  When included we will only backup the entity Ids included.
		// When the body is null all entities will be backed up.
		Set<String> entityIdsToBackup = null;
		if(request.getInputStream() != null){
			BackupSubmission submission = objectTypeSerializer.deserialize(request.getInputStream(), header,BackupSubmission.class, header.getContentType());
			entityIdsToBackup = submission.getEntityIdsToBackup();
		}
		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		// start a search document daemon
		return backupDaemonLauncher.startSearchDocument(userInfo, entityIdsToBackup);
		
	}
	
	public BackupRestoreStatus getStatus(String userId, String daemonId) throws UnauthorizedException, DatastoreException, IOException, NotFoundException {

		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Get the status of this daemon
		return backupDaemonLauncher.getStatus(userInfo, daemonId);
		
	}
	
	public void terminateDaemon(String userId, String daemonId) throws UnauthorizedException, DatastoreException, IOException, NotFoundException {

		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Terminate the daemon
		backupDaemonLauncher.terminate(userInfo, daemonId);
		
	}
	
	public StackStatus getStackStatus() {

		// Get the status of this daemon
		return stackStatusManager.getCurrentStatus();		
	}
	
	public StackStatus updateStatusStackStatus(String userId, HttpHeaders header, HttpServletRequest request) throws UnauthorizedException, DatastoreException, IOException, NotFoundException {
		// Get the status of this daemon
		StackStatus updatedValue = objectTypeSerializer.deserialize(request.getInputStream(), header, StackStatus.class, header.getContentType());
		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return stackStatusManager.updateStatus(userInfo, updatedValue);		
	}


}
