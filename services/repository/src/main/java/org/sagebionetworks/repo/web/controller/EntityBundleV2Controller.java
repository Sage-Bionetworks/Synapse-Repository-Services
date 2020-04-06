package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleCreate;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleRequest;
import org.sagebionetworks.repo.queryparser.ParseException;
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
 * <p>
 * The Entity Bundle Services provide bundled access to Entities and their related data components.
 * An EntityBundle can be used to create, fetch, or update an Entity and associated objects with a
 * single web service request.
 * </p>
 */
@ControllerInfo(displayName="Entity Bundle Services V2", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class EntityBundleV2Controller {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Get an entity and related data with a single POST.
	 *
	 * @param userId -The user that is doing the get.
	 * @param id - The ID of the entity to fetch.
	 * @param request
	 * @return The requested Entity if it exists.
	 * @throws NotFoundException - Thrown if the requested entity does not exist.
	 * @throws DatastoreException - Thrown when an there is a server failure.
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 * @throws ParseException - Thrown if the childCount query failed
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID_BUNDLE_V2, method = RequestMethod.POST)
	public @ResponseBody
	EntityBundle getEntityBundle(
			UserInfo userInfo,
			@PathVariable String id,
			@RequestBody EntityBundleRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException {
		return serviceProvider.getEntityBundleService().getEntityBundle(userInfo, id, request);
	}

	/**
	 * Get an entity at a specific version and its related data with a single POST.
	 *
	 * @param userId -The user that is doing the get.
	 * @param id - The ID of the entity to fetch.
	 * @param versionNumber - The version of the entity to fetch
	 * @param request
	 * @return The requested Entity if it exists.
	 * @throws NotFoundException - Thrown if the requested entity does not exist.
	 * @throws DatastoreException - Thrown when an there is a server failure.
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 * @throws ParseException - Thrown if the childCount query failed
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_VERSION_NUMBER_BUNDLE_V2, method = RequestMethod.POST)
	public @ResponseBody
	EntityBundle getEntityBundle(
			UserInfo userInfo,
			@PathVariable String id,
			@PathVariable Long versionNumber,
			@RequestBody EntityBundleRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException {
		return serviceProvider.getEntityBundleService().getEntityBundle(userInfo, id, versionNumber, request);
	}

	/**
	 * Create an entity and associated components with a single POST.
	 * Specifically, this operation supports creation of an Entity, its
	 * Annotations, and its ACL.
	 *
	 * Upon successful creation, an EntityBundle is returned containing the
	 * requested components, as defined by the partsMask in the request object.
	 *
	 * @param userInfo
	 * @param ebc - the EntityBundleCreate object containing the Entity and Annotations to create.
	 * @return
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws ACLInheritanceException
	 * @throws ParseException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_BUNDLE_V2_CREATE, method = RequestMethod.POST)
	public @ResponseBody
	EntityBundle createEntityBundle(
			UserInfo userInfo,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String generatedBy,
			@RequestBody EntityBundleCreate ebc)
			throws ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException {
		return serviceProvider.getEntityBundleService().createEntityBundle(userInfo, ebc, generatedBy);
	}

	/**
	 * Update an entity and associated components with a single PUT.
	 * Specifically, this operation supports update of an Entity, its
	 * Annotations, and its ACL.
	 *
	 * Upon successful creation, an EntityBundle is returned containing the
	 * requested components, as defined by the partsMask in the request object.
	 *
	 * @param userInfo
	 * @param ebc - the EntityBundleCreate object containing the Entity and Annotations to update.
	 * @return
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws ParseException
	 * @throws ACLInheritanceException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID_BUNDLE_V2, method = RequestMethod.PUT)
	public @ResponseBody
	EntityBundle updateEntityBundle(
			UserInfo userInfo,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String generatedBy,
			@PathVariable String id,
			@RequestBody EntityBundleCreate ebc)
			throws ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException {
		return serviceProvider.getEntityBundleService().updateEntityBundle(userInfo, id, ebc, generatedBy);
	}

}
