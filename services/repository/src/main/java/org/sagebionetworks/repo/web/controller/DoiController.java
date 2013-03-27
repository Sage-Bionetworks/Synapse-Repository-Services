package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Provides REST APIs for managing Synapse DOIs.
 */
@Controller
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
	@RequestMapping(value = {UrlHelpers.ENTITY_DOI}, method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public @ResponseBody Doi
	createDoi(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		return serviceProvider.getDoiService().createDoi(userId, id, DoiObjectType.ENTITY, null);
	}

	/**
	 * Creates a DOI for the specified entity version.
	 * 
	 * @param userId The user creating this DOI
	 * @param id The entity ID
	 * @param versionNumber The version of the entity
	 * @return DOI being created
	 */
	@RequestMapping(value = {UrlHelpers.ENTITY_VERSION_DOI}, method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public @ResponseBody Doi
	createDoi(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@PathVariable Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		return serviceProvider.getDoiService().createDoi(userId, id, DoiObjectType.ENTITY, versionNumber);
	}

	/**
	 * Gets the DOI of the specified entity.
	 *
	 * @param userId The user retrieving the DOI
	 * @param id The ID of the entity
	 */
	@RequestMapping(value = {UrlHelpers.ENTITY_DOI}, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody Doi
	getDoi(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		return serviceProvider.getDoiService().getDoi(userId, id, DoiObjectType.ENTITY, null);
	}

	/**
	 * Gets the DOI of the specified entity version.
	 *
	 * @param userId The user retrieving the DOI.
	 * @param id The ID of the entity
	 * @param versionNumber The version of the entity. Null to indicate the most recent version where applicable.
	 */
	@RequestMapping(value = {UrlHelpers.ENTITY_VERSION_DOI}, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody Doi
	getDoi(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@PathVariable Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		return serviceProvider.getDoiService().getDoi(userId, id, DoiObjectType.ENTITY, versionNumber);
	}
}
