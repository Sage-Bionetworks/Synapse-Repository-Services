package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.StackStatusManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDaemonLauncher;
import org.sagebionetworks.repo.manager.backup.migration.DependencyManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.BackupSubmission;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 
 * This controller is used for Administration of Synapse.
 * 
 * 
 * 
 * @author John
 *
 */
@Controller
public class AdministrationController extends BaseController {
	
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
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.GET_ALL_BACKUP_OBJECTS, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<MigratableObjectData> getAllBackupObjects(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = UrlHelpers.INCLUDE_DEPENDENCIES_PARAM, required = false, defaultValue = "true") Boolean  includeDependencies

			) throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
		QueryResults<MigratableObjectData> queryResults = dependencyManager.getAllObjects(offset, limit, includeDependencies);
		PaginatedResults<MigratableObjectData> result = new PaginatedResults<MigratableObjectData>();
		result.setResults(queryResults.getResults());
		result.setTotalNumberOfResults(queryResults.getTotalNumberOfResults());
		return result;
	}
	
	
	/**
	 * Start a backup daemon.  Monitor the status of the daemon with the getStatus method.
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY_BACKUP_DAMEON
			}, method = RequestMethod.POST)
	public @ResponseBody
	BackupRestoreStatus startBackup(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.MIGRATION_TYPE_PARAM, required=true) String type,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request)
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
	
	/**
	 * Start a system restore daemon using the passed file name.  The file must be in the 
	 * the bucket belonging to this stack.
	 * 
	 * @param fileName
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY_RESTORE_DAMEON
			}, method = RequestMethod.POST)
	public @ResponseBody
	BackupRestoreStatus startRestore(
			@RequestBody RestoreSubmission file,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.MIGRATION_TYPE_PARAM, required=true) String type,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {

		if(file == null) throw new IllegalArgumentException("File cannot be null");
		if(file.getFileName() == null) throw new IllegalArgumentException("File.getFileName cannot be null");
		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		// start a restore daemon
		return backupDaemonLauncher.startRestore(userInfo, file.getFileName(), MigratableObjectType.valueOf(type));
	}
	
	/**
	 * Delete a migratable object
	 * 
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY_RESTORE_DAMEON
			}, method = RequestMethod.DELETE)
	public void deleteMigratableObject(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.MIGRATION_OBJECT_ID_PARAM, required=true) String objectId,
			@RequestParam(value = UrlHelpers.MIGRATION_TYPE_PARAM, required=true) String type,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request)
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
	
	/**
	 * Start a search document daemon.  Monitor the status of the daemon with the getStatus method.
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY_SEARCH_DOCUMENT_DAMEON
			}, method = RequestMethod.POST)
	public @ResponseBody
	BackupRestoreStatus startSearchDocument(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request)
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
		// start a search document daemon
		return backupDaemonLauncher.startSearchDocument(userInfo, entityIdsToBackup);
	}
	
	/**
	 * Get the status of a running daemon (either a backup or restore)
	 * @param daemonId
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY_DAEMON_ID
			}, method = RequestMethod.GET)
	public @ResponseBody
	BackupRestoreStatus getStatus(
			@PathVariable String daemonId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {

		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Get the status of this daemon
		return backupDaemonLauncher.getStatus(userInfo, daemonId);
	}
	
	/**
	 * Terminate a running daemon.  This has no effect if the daemon is already terminated.
	 * @param daemonId
	 * @param userId
	 * @param header
	 * @param request
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY_DAEMON_ID
			}, method = RequestMethod.DELETE)
	public @ResponseBody
	void terminateDaemon(
			@PathVariable String daemonId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {

		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Terminate the daemon
		backupDaemonLauncher.terminate(userInfo, daemonId);
	}
	
	
	/**
	 * Get the current status of the stack
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.STACK_STATUS
			}, method = RequestMethod.GET)
	public @ResponseBody
	StackStatus getStackStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) {

		// Get the status of this daemon
		return stackStatusManager.getCurrentStatus();
	}
	
	/**
	 * Update the current status of the stack.
	 * 
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.STACK_STATUS
			}, method = RequestMethod.PUT)
	public @ResponseBody
	StackStatus updateStatusStackStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException, IOException {

		// Get the status of this daemon
		StackStatus updatedValue = objectTypeSerializer.deserialize(request.getInputStream(), header, StackStatus.class, header.getContentType());
		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return stackStatusManager.updateStatus(userInfo, updatedValue);
	}

}
