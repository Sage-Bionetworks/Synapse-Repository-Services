package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.manager.doi.DoiManagerImpl;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.doi.v2.DoiRequest;
import org.sagebionetworks.repo.model.doi.v2.DoiResponse;
import org.sagebionetworks.repo.web.DeprecatedServiceException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
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
public class DoiController {

	@Autowired
	private ServiceProvider serviceProvider;

	private static final String DEPRECATED_MESSAGE = "The Entity DOI service is deprecated. Please update your client to use the new DOI API";

	/**
	 * This service is deprecated. See: <a href="${POST.doi.async.start}">POST /doi/async/start</a>
	 * @throws DeprecatedServiceException This service is deprecated.
	 */
	@Deprecated
	@RequiredScope({modify})
	@RequestMapping(value = {UrlHelpers.ENTITY_DOI}, method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void createDoi() throws DeprecatedServiceException {
		throw new DeprecatedServiceException(DEPRECATED_MESSAGE);
	}

	/**
	 * This service is deprecated. See: <a href="${POST.doi.async.start}">POST /doi/async/start</a>
	 * @throws DeprecatedServiceException This service is deprecated.
	 */
	@Deprecated
	@RequiredScope({modify})
	@RequestMapping(value = {UrlHelpers.ENTITY_VERSION_DOI}, method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void createDoiForVersion() throws DeprecatedServiceException {
		throw new DeprecatedServiceException(DEPRECATED_MESSAGE);
	}

	/**
	 * This service is deprecated. See: <a href="${GET.doi}">GET /doi</a> and
	 * <a href="${GET.doi.association}">GET /doi/association</a>
	 * @throws DeprecatedServiceException This service is deprecated.
	 */
	@Deprecated
	@RequiredScope({view})
	@RequestMapping(value = {UrlHelpers.ENTITY_DOI}, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public void getDoi() throws DeprecatedServiceException {
		throw new DeprecatedServiceException(DEPRECATED_MESSAGE);
	}

	/**
	 * This service is deprecated. See: <a href="${GET.doi}">GET /doi</a> and
	 * <a href="${GET.doi.association}">GET /doi/association</a>
	 * @throws DeprecatedServiceException This service is deprecated.
	 */
	@Deprecated
	@RequiredScope({view})
	@RequestMapping(value = {UrlHelpers.ENTITY_VERSION_DOI}, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public void getDoiForVersion() throws DeprecatedServiceException {
		throw new DeprecatedServiceException(DEPRECATED_MESSAGE);
	}

	/**
	 * Retrieves the DOI for the object and its associated DOI metadata.
	 * Note: this call calls an external API, which may impact performance
	 * To just retrieve the DOI association, see: <a href="${GET.doi.association}">GET /doi/association</a>
	 * @param objectId The ID of the object to retrieve
	 * @param objectType The type of the object
	 * @param versionNumber The version number of the object
	 * @return The DOI and all its associated DOI metadata, if the DOI has been minted for the object
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@RequiredScope({view})
	@RequestMapping(value = {UrlHelpers.DOI}, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody Doi
	getDoiV2(@RequestParam(value = "id") String objectId,
			 @RequestParam(value = "type") ObjectType objectType,
			 @RequestParam(value = "version", required = false) Long versionNumber) throws NotFoundException, UnauthorizedException, ServiceUnavailableException {
		return serviceProvider.getDoiServiceV2().getDoi(objectId, objectType, versionNumber);
	}

	/**
	 * Retrieves the DOI for the object.
	 * Note: this call only retrieves the DOI association, if it exists. To retrieve the metadata for the object,
	 * see <a href="${GET.doi}">GET /doi</a>
	 * @param objectId The ID of the object to retrieve
	 * @param objectType The type of the object
	 * @param versionNumber The version number of the object
	 * @return The DOI if the DOI has been minted for the object
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@RequiredScope({view})
	@RequestMapping(value = {UrlHelpers.DOI_ASSOCIATION}, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody
	DoiAssociation
	getDoiAssociation(
			@RequestParam(value = "id") String objectId,
			@RequestParam(value = "type") ObjectType objectType,
			@RequestParam(value = "version", required = false) Long versionNumber) throws NotFoundException, UnauthorizedException {
		return serviceProvider.getDoiServiceV2().getDoiAssociation(objectId, objectType, versionNumber);
	}

	/**
	 * Asynchronously creates or updates a DOI in Synapse, with input metadata. Retrieve the results with
	 * <a href="${GET.doi.async.get.asyncToken}">GET /doi/async/get/{asyncToken}</a>. This call may fail if the external
	 * DataCite API is down. If the failure is recoverable, it will retry automatically.
	 * @param userId The ID of the user making the call
	 * @param request A request containing a DOI and its metadata to associate with a Synapse object
	 * @return The asynchronous job ID
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@RequiredScope({view,modify})
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
	 * Get the results of a call to <a href="${POST.doi.async.start}">POST /doi/async/start</a>
	 * @param userId The ID of the user making the call
	 * @param asyncToken The async job token from the create/update call
	 * @return The results of the call
	 * @throws Throwable
	 */
	@RequiredScope({view,modify})
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
	 * @param objectId The ID of the object to retrieve
	 * @param objectType The type of the object
	 * @param versionNumber The version number of the object
	 * @param redirect Whether to return the URL or redirect to the URL
	 * @param response
	 * @return The URL of the object in Synapse.
	 * @throws IOException
	 */
	@RequiredScope({view})
	@RequestMapping(value = {DoiManagerImpl.LOCATE_RESOURCE_PATH}, method = RequestMethod.GET)
	public void locate(@RequestParam(value = DoiManagerImpl.OBJECT_ID_PATH_PARAM) String objectId,
		   @RequestParam(value = DoiManagerImpl.OBJECT_TYPE_PATH_PARAM) ObjectType objectType,
		   @RequestParam(value = DoiManagerImpl.OBJECT_VERSION_PATH_PARAM, required = false) Long versionNumber,
		   @RequestParam(value = "redirect", required = false, defaultValue = "true") Boolean redirect,
				HttpServletResponse response) throws IOException {
		RedirectUtils.handleRedirect(redirect, serviceProvider.getDoiServiceV2().locate(objectId, objectType, versionNumber), response);
	}
}
