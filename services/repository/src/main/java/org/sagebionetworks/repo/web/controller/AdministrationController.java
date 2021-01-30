package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.feature.FeatureStatus;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.IdGeneratorExport;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
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
 * Since this controller is for internal use only we removed the ControllerInfo annotation.
 *
 */
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class AdministrationController {
	
	@Autowired
	private ServiceProvider serviceProvider;
	
	/**
	 * @return the current status of the stack
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {
			UrlHelpers.ADMIN_STACK_STATUS,
			UrlHelpers.STACK_STATUS
			}, method = RequestMethod.GET)
	public @ResponseBody
	StackStatus getStackStatus() {
		return serviceProvider.getAdministrationService().getStackStatus();
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
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ADMIN_STACK_STATUS
			}, method = RequestMethod.PUT)
	public @ResponseBody
	StackStatus updateStatusStackStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException, IOException {

		return serviceProvider.getAdministrationService().updateStatusStackStatus(userId, header, request);
	}
	
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.CHANGE_MESSAGES }, method = RequestMethod.GET)
	public @ResponseBody
	ChangeMessages listChangeMessages(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
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
	
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.REBROADCAST_MESSAGES }, method = RequestMethod.POST)
	public @ResponseBody
	PublishResults rebroadcastChangeMessagesToQueue(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
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
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.REFIRE_MESSAGES }, method = RequestMethod.GET)
	public @ResponseBody
	FireMessagesResult refireChangeMessagesToQueue(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam Long startChangeNumber,
			@RequestParam Long limit) throws DatastoreException,
			NotFoundException {
		// Pass it along
		return serviceProvider.getAdministrationService().reFireChangeMessages(userId, startChangeNumber, limit);
	}
	
	/**
	 * Get current change message number
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.CURRENT_NUMBER }, method = RequestMethod.GET)
	public @ResponseBody
	FireMessagesResult getCurrentChangeNumber(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws DatastoreException,
			NotFoundException {
		// Pass it along
		return serviceProvider.getAdministrationService().getCurrentChangeNumber(userId);
	}
	
	/**
	 * Create or update change messages
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.CREATE_OR_UPDATE }, method = RequestMethod.POST)
	public @ResponseBody
	ChangeMessages createOrUpdateChangeMessages(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ChangeMessages batch) throws UnauthorizedException, NotFoundException {
				return serviceProvider.getAdministrationService().createOrUpdateChangeMessages(userId, batch);
	}
	
	/**
	 * Clears the Synapse DOI table.
	 */
	@RequiredScope({modify})
	@RequestMapping(value = {UrlHelpers.ADMIN_DOI_CLEAR}, method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void
	clearDoi(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		serviceProvider.getAdministrationService().clearDoi(userId);
	}

	/**
	 * Creates a user with specific state to be used for integration testing.
	 * If the user already exists, just returns the existing one.
	 */
	@RequiredScope({view,modify})
	@RequestMapping(value = {UrlHelpers.ADMIN_USER}, method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody EntityId createOrGetIntegrationTestUser(
			@RequestBody NewIntegrationTestUser userSpecs,
	        @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) 
	        		throws NotFoundException {
		return serviceProvider.getAdministrationService().createOrGetTestUser(userId, userSpecs);
	}

	/**
	 * Deletes a user.  All FKs must be deleted before this will succeed
	 */
	@RequiredScope({modify})
	@RequestMapping(value = {UrlHelpers.ADMIN_USER_ID}, method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteUser(
			@PathVariable String id, 
	        @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) 
	        		throws NotFoundException {
		serviceProvider.getAdministrationService().deleteUser(userId, id);
	}
	
	@RequiredScope({modify})
	@RequestMapping(value = { UrlHelpers.ADMIN_TABLE_REBUILD }, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void rebuildTable(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id") String tableId) throws NotFoundException, IOException {
		serviceProvider.getAdministrationService().rebuildTable(userId, tableId);
	}

	@RequiredScope({modify})
	@RequestMapping(value = {UrlHelpers.ADMIN_CLEAR_LOCKS}, method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void clearLocks(
	        @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) 
	        		throws NotFoundException {
		serviceProvider.getAdministrationService().clearAllLocks(userId);
	}

	
	/**
	 * 
	 * @param userId
	 * @param body
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ADMIN_ASYNCHRONOUS_JOB, method = RequestMethod.POST)
	public @ResponseBody
	AsynchronousJobStatus launchNewJob(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody AsyncMigrationRequest body) throws NotFoundException {
		return serviceProvider.getAsynchronousJobServices().startJob(userId, body);
	}
	
	/**
	 * 
	 * @param userId
	 * @param jobId
	 * @return
	 * @throws NotFoundException
	 * @throws AsynchJobFailedException
	 * @throws NotReadyException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ADMIN_ASYNCHRONOUS_JOB_ID, method = RequestMethod.GET)
	public @ResponseBody
	AsynchronousJobStatus getJobStatus(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String jobId)
			throws NotFoundException, AsynchJobFailedException, NotReadyException {
		return serviceProvider.getAsynchronousJobServices().getJobStatus(userId, jobId);
	}
	
	/**
	 * Create an export script for the ID generator database.  The script can be used to setup a new ID generator.
	 * 
	 * @param userId
	 * @return
	 * @throws NotFoundException
	 * @throws AsynchJobFailedException
	 * @throws NotReadyException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ADMIN_ID_GEN_EXPORT, method = RequestMethod.GET)
	public @ResponseBody
	IdGeneratorExport createIdGeneratorExport(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws NotFoundException, AsynchJobFailedException, NotReadyException {
		return serviceProvider.getAdministrationService().createIdGeneratorExport(userId);
	}
	
	/**
	 * Changes the verified status of the OAuth client with the provided id. Only an administrator or a member of the ACT team can perform this operation.
	 * 
	 * @param userId
	 * @param clientId The id of the client to verify
	 * @param etag The etag of the client, this must match the current etag of the client
	 * @param status The verified status to change to, default true
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ADMIN_OAUTH_CLIENT_VERIFICATION, method = RequestMethod.PUT)
	public @ResponseBody OAuthClient updateOAuthClientVerifiedStatus(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String clientId,
			@RequestParam(defaultValue = "true") Boolean status,
			@RequestParam(required = true) String etag)
			throws NotFoundException, UnauthorizedException {
		return serviceProvider.getOpenIDConnectService().updateOpenIDConnectClientVerifiedStatus(userId, clientId, etag, status);
	}

	/**
	 * Redacts all information about a user to comply with data removal requests.
	 * @param userId Principal ID of the caller. Must be an administrator
	 * @param principalId The principal ID of the user whose information should be cleared
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ADMIN_REDACT_USER, method = RequestMethod.POST)
	public @ResponseBody void clearUserProfile(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
																	 @PathVariable Long principalId)
			throws NotFoundException, UnauthorizedException {
		serviceProvider.getPrincipalService().redactPrincipalInformation(userId, principalId);
	}
	
	/**
	 * Fetches the status of the given feature
	 * 
	 * @param userId
	 * @param feature
	 * @return
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ADMIN_FEATURE_STATUS, method = RequestMethod.GET)
	public @ResponseBody FeatureStatus getFeatureStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, 
			@PathVariable("feature") Feature feature) {
		return serviceProvider.getAdministrationService().getFeatureStatus(userId, feature);
	}
	
	/**
	 * Sets the status for the given feature
	 * 
	 * @param userId
	 * @param feature
	 * @param status
	 * @return
	 */
	@RequiredScope({view, modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ADMIN_FEATURE_STATUS, method = RequestMethod.POST)
	public @ResponseBody FeatureStatus setFeatureStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, 
			@PathVariable("feature") Feature feature,
			@RequestBody FeatureStatus status) {
		return serviceProvider.getAdministrationService().setFeatureStatus(userId, feature, status);
	}

}
