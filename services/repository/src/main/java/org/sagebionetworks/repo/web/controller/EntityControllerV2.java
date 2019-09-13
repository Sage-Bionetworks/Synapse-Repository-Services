package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2;
import org.sagebionetworks.repo.web.NotFoundException;
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
 * Version 2 of some Entity Services. API calls are prefixed with /repo/v2/ instead of repo/v1/
 * Currently, only updating Annotations are supported.
 */
@ControllerInfo(displayName = "Entity Services V2", path = "repo/v2")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH_V2)
public class EntityControllerV2 {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Get the annotations for an entity.
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> on the Entity, to get its annotations.
	 * </p>
	 *
	 * @param id
	 *            - The id of the entity to update.
	 * @param userId
	 *            - The user that is doing the update.
	 * @param request
	 *            - Used to read the contents.
	 * @return The annotations for the given entity.
	 * @throws NotFoundException
	 *             - Thrown if the given entity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ANNOTATIONS }, method = RequestMethod.GET)
	public @ResponseBody
	AnnotationsV2 getEntityAnnotations(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Pass it along
		return serviceProvider.getEntityService().getEntityAnnotations(userId,
				id);
	}

	/**
	 * Get an Entity's annotations for a specific version of a FileEntity.
	 *
	 * @param id
	 *            The ID of the Entity.
	 * @param versionNumber
	 *            The version number of the Entity.
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION_ANNOTATIONS }, method = RequestMethod.GET)
	public @ResponseBody
	AnnotationsV2 getEntityAnnotationsV2ForVersion(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Pass it along
		return serviceProvider.getEntityService()
				.getEntityAnnotationsForVersion(userId, id, versionNumber);
	}


	/**
	 * Update an entities annotations.
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> on the Entity, to update its annotations.
	 * </p>
	 *
	 * @param id
	 *            - The id of the entity to update.
	 * @param userId
	 *            - The user that is doing the update.
	 * @param updatedAnnotations
	 *            - The updated annotations
	 * @param request
	 * @return the updated annotations
	 * @throws ConflictingUpdateException
	 *             - Thrown when the passed etag does not match the current etag
	 *             of an entity. This will occur when an entity gets updated
	 *             after getting the current etag.
	 * @throws NotFoundException
	 *             - Thrown if the given entity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 *             - Thrown if the passed entity contents doe not match the
	 *             expected schema.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ANNOTATIONS }, method = RequestMethod.PUT)
	public @ResponseBody
	AnnotationsV2 updateEntityAnnotations(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody AnnotationsV2 updatedAnnotations,
			HttpServletRequest request) throws ConflictingUpdateException,
			NotFoundException, DatastoreException, UnauthorizedException,
			InvalidModelException {
		// Pass it along
		return serviceProvider.getEntityService().updateEntityAnnotations(
				userId, id, updatedAnnotations);
	}


}
