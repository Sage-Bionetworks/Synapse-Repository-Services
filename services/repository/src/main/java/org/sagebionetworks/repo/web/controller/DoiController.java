package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.doi.v2.DoiRequest;
import org.sagebionetworks.repo.model.doi.v2.DoiResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Provides REST APIs for managing Synapse DOIs.
 */
@ControllerInfo(displayName="DOI Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class DoiController extends BaseController {

	@Autowired
	private ServiceProvider serviceProvider;

	/**
	 * Creates a DOI for the specified entity. The DOI will associated with the most recent version where applicable.
	 *
	 * @param userId The user creating this DOI
	 * @param id The entity ID
	 * @return DOI being created
	 */
	@Deprecated
	@RequestMapping(value = {UrlHelpers.ENTITY_DOI}, method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public @ResponseBody Doi
	createDoi(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		return serviceProvider.getDoiService().createDoi(userId, id, ObjectType.ENTITY, null);
	}

	/**
	 * Creates a DOI for the specified entity version.
	 * 
	 * @param userId The user creating this DOI
	 * @param id The entity ID
	 * @param versionNumber The version of the entity
	 * @return DOI being created
	 */
	@Deprecated
	@RequestMapping(value = {UrlHelpers.ENTITY_VERSION_DOI}, method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public @ResponseBody Doi
	createDoi(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id,
			@PathVariable Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		return serviceProvider.getDoiService().createDoi(userId, id, ObjectType.ENTITY, versionNumber);
	}

	/**
	 * Gets the DOI of the specified entity.
	 *
	 * @param userId The user retrieving the DOI
	 * @param id The ID of the entity
	 */
	@Deprecated
	@RequestMapping(value = {UrlHelpers.ENTITY_DOI}, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody Doi
	getDoi(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		return serviceProvider.getDoiService().getDoiForVersion(userId, id, ObjectType.ENTITY, null);
	}

	/**
	 * Gets the DOI of the specified entity version.
	 *
	 * @param userId The user retrieving the DOI.
	 * @param id The ID of the entity
	 * @param versionNumber The version of the entity. Null to indicate the most recent version where applicable.
	 */
	@Deprecated
	@RequestMapping(value = {UrlHelpers.ENTITY_VERSION_DOI}, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody Doi
	getDoi(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id,
			@PathVariable Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		return serviceProvider.getDoiService().getDoiForVersion(userId, id, ObjectType.ENTITY, versionNumber);
	}

	/**
	 * Retrieves the DOI for the object and its associated DOI metadata.
	 * Note: this call calls an external API, which may impact performance
	 * To just retrieve the DOI, see {@link #getDoiAssociation}
	 * @param userId The ID of the user making the call
	 * @param objectId The ID of the object to retrieve
	 * @param objectType The type of the object
	 * @param versionNumber The version number of the object
	 * @return The DOI and all its associated DOI metadata, if the DOI has been minted for the object
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@RequestMapping(value = {UrlHelpers.DOI}, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody
	org.sagebionetworks.repo.model.doi.v2.Doi
	getDoiV2(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			 @RequestParam(value = "id") String objectId,
			 @RequestParam(value = "type") ObjectType objectType,
			 @RequestParam(value = "version", required = false) Long versionNumber) throws NotFoundException, UnauthorizedException, ServiceUnavailableException {
		return serviceProvider.getDoiServiceV2().getDoi(userId, objectId, objectType, versionNumber);
	}

	/**
	 * Retrieves the DOI for the object.
	 * Note: this call only retrieves the DOI association, if it exists. To retrieve the metadata for the object,
	 * see {@link #getDoiV2 }
	 * @param userId The ID of the user making the call
	 * @param objectId The ID of the object to retrieve
	 * @param objectType The type of the object
	 * @param versionNumber The version number of the object
	 * @return The DOI if the DOI has been minted for the object
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@RequestMapping(value = {UrlHelpers.DOI_ASSOCIATION}, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody
	DoiAssociation
	getDoiAssociation(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
					  @RequestParam(value = "id") String objectId,
					  @RequestParam(value = "type") ObjectType objectType,
					  @RequestParam(value = "version", required = false) Long versionNumber) throws NotFoundException, UnauthorizedException {
		return serviceProvider.getDoiServiceV2().getDoiAssociation(userId, objectId, objectType, versionNumber);
	}

	/**
	 * Asynchronously creates or updates a DOI in Synapse, with input metadata.
	 * @param userId The ID of the user making the call
	 * @param request A request containing a DOI and its metadata to associate with a Synapse object
	 * @return The asynchronous job ID
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@RequestMapping(value = {UrlHelpers.DOI_ASYNC_START}, method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody
	AsyncJobId
	startCreateOrUpdateDoi(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
					  @RequestBody DoiRequest request) throws NotFoundException, UnauthorizedException {
		AsynchronousJobStatus job = serviceProvider
				.getAsynchronousJobServices().startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Get the results of a call to {@link #startCreateOrUpdateDoi}
	 * @param userId The ID of the user making the call
	 * @param asyncToken The async job token from the create/update call
	 * @return The results of the call
	 * @throws Throwable
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DOI_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody
	DoiResponse getCreateOrUpdateDoiResults(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String asyncToken) throws Throwable {
		AsynchronousJobStatus jobStatus = serviceProvider
				.getAsynchronousJobServices().getJobStatusAndThrow(userId,
						asyncToken);
		return (DoiResponse) jobStatus.getResponseBody();
	}

	/**
	 * Retrieves the Synapse web portal URL to the object entered.
	 * Note: This call does not check to see if the object exists in Synapse.
	 * @param userId The ID of the user making the call
	 * @param objectId The ID of the object to retrieve
	 * @param objectType The type of the object
	 * @param versionNumber The version number of the object
	 * @param redirect Whether to return the URL or redirect to the URL
	 * @param response
	 * @return The URL of the object in Synapse.
	 * @throws IOException
	 */
	@RequestMapping(value = {UrlHelpers.DOI_LOCATE}, method = RequestMethod.GET)
	public @ResponseBody
	void locate(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
		   @RequestParam(value = "id") String objectId,
		   @RequestParam(value = "type") ObjectType objectType,
		   @RequestParam(value = "version", required = false) Long versionNumber,
		   @RequestParam(value = "redirect", required = false, defaultValue = "true") Boolean redirect,
				HttpServletResponse response) throws IOException {
		RedirectUtils.handleRedirect(redirect, serviceProvider.getDoiServiceV2().locate(userId, objectId, objectType, versionNumber), response);
	}
}
