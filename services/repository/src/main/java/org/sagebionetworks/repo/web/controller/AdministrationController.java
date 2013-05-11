package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MigratableObjectCount;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.service.ServiceProvider;
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
 * This controller is used for Administration of Synapse.
 * 
 * @author John
 *
 */
@Controller
public class AdministrationController extends BaseController {
	
	@Autowired
	ServiceProvider serviceProvider;
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.GET_ALL_BACKUP_OBJECTS, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<MigratableObjectData> getAllBackupObjects(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = UrlHelpers.INCLUDE_DEPENDENCIES_PARAM, required = false, defaultValue = "true") Boolean  includeDependencies,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException {
		return serviceProvider.getAdministrationService().getAllBackupObjects(userId, offset, limit, includeDependencies);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.GET_ALL_BACKUP_OBJECTS_COUNTS, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<MigratableObjectCount> getAllBackupObjectsCounts(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, NotFoundException {
		return serviceProvider.getAdministrationService().getAllBackupObjectsCounts(userId);
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
		
		return serviceProvider.getAdministrationService().startBackup(userId, type, header, request);
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

		return serviceProvider.getAdministrationService().startRestore(file, userId, type, header, request);
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

		serviceProvider.getAdministrationService().deleteMigratableObject(userId, objectId, type, header, request);
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

		return serviceProvider.getAdministrationService().getStatus(daemonId, userId, header, request);
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

		serviceProvider.getAdministrationService().terminateDaemon(daemonId, userId, header, request);
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

		return serviceProvider.getAdministrationService().getStackStatus(userId, header, request);
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

		return serviceProvider.getAdministrationService().updateStatusStackStatus(userId, header, request);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.CHANGE_MESSAGES }, method = RequestMethod.GET)
	public @ResponseBody
	ChangeMessages listChangeMessages(String userId,
			@RequestParam Long startChangeNumber, @RequestParam String type,
			@RequestParam Long limit) throws DatastoreException,
			NotFoundException {
		// The type can be null
		ObjectType typeEnum = null;
		if (type != null) {
			typeEnum = ObjectType.valueOf(type);
		}
		// Pass it along
		return serviceProvider.getAdministrationService().listChangeMessages(userId, startChangeNumber, typeEnum, limit);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.REBROADCAST_MESSAGES }, method = RequestMethod.POST)
	public @ResponseBody
	PublishResults rebroadcastChangeMessagesToQueue(String userId,
			@RequestParam String queueName,
			@RequestParam Long startChangeNumber, @RequestParam String type,
			@RequestParam Long limit) throws DatastoreException,
			NotFoundException {
		// The type can be null
		ObjectType typeEnum = null;
		if (type != null) {
			typeEnum = ObjectType.valueOf(type);
		}
		// Pass it along
		return serviceProvider.getAdministrationService().rebroadcastChangeMessagesToQueue(userId, queueName, startChangeNumber, typeEnum, limit);
	}

	/**
	 * Refires all the change messages
	 **/
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.REFIRE_MESSAGES }, method = RequestMethod.POST)
	public @ResponseBody
	void refireChangeMessagesToQueue(String userId,
			@RequestParam Long startChangeNumber,
			@RequestParam Long limit) throws DatastoreException,
			NotFoundException {
		// Pass it along
		serviceProvider.getAdministrationService().reFireChangeMessages(userId, startChangeNumber, limit);
	}

	
	/**
	 * Clears the Synapse DOI table.
	 */
	@RequestMapping(value = {UrlHelpers.ADMIN_DOI_CLEAR}, method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void
	clearDoi(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		serviceProvider.getAdministrationService().clearDoi(userId);
	}
}
