package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
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
 * 
 * <p>
 * One of the request parameters for an EntityBundle is an integer "mask" or "partsMask". This
 * integer is used as a bit-string of flags to specify which parts to include in the EntityBundle.
 * As of this writing, the mask is defined as follows:
 * <ul>
 * <li>	Entity <i>(Entity)</i> = 0x1 </li>
 * <li> Annotations <i>(Annotations)</i> = 0x2 </li>
 * <li> Permissions <i>(UserEntityPermissions)</i> = 0x4 </li>
 * <li> Entity Path <i>(EntityPath)</i> = 0x8 </li>
 * <li> Entity References <i>(List\<EntityHeader\>)</i> = 0x10 </li>
 * <li> HasChildren <i>(Boolean)</i> = 0x20 </li>
 * <li> ACL <i>(AccessControlList)</i> = 0x40 </li>
 * <li> Access Requirements <i>(List\<AccessRequirement\>) = 0x200 </li>
 * <li> Unmet Access Requirements <i>(List\<AccessRequirement\>) = 0x400 </li>
 * <li> File Handles <i>(List\<FileHandle\>) = 0x800 </li>
 * For example, if the Entity and its Annotations are desired, the request mask value should be
 * 0x1 + 0x2 = 0x3.
 * </p>
 */
@ControllerInfo(displayName="Entity Bundle Services", path="repo/v1")
@Controller
public class EntityBundleController extends BaseController {
	
	@Autowired
	ServiceProvider serviceProvider;
	
	/**
	 * Get an entity and related data with a single GET. Note that childCount is
	 * calculated in the QueryController.
	 * 
	 * @param userId -The user that is doing the get.
	 * @param id - The ID of the entity to fetch.
	 * @param mask - integer flag defining which components to include in the EntityBundle.
	 * @param request
	 * @return The requested Entity if it exists.
	 * @throws NotFoundException - Thrown if the requested entity does not exist.
	 * @throws DatastoreException - Thrown when an there is a server failure. 
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException 
	 * @throws ParseException - Thrown if the childCount query failed
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID_BUNDLE, method = RequestMethod.GET)
	public @ResponseBody
	EntityBundle getEntityBundle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, 
			@RequestParam int mask, HttpServletRequest request
			)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException {
		return serviceProvider.getEntityBundleService().getEntityBundle(userId, id, mask, request);
	}	

	/**
	 * Get an entity at a specific version and its related data with a single GET. Note that childCount is
	 * calculated in the QueryController.
	 * 
	 * @param userId -The user that is doing the get.
	 * @param id - The ID of the entity to fetch.
	 * @param mask - integer flag defining which components to include in the EntityBundle.
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
	@RequestMapping(value = UrlHelpers.ENTITY_VERSION_NUMBER_BUNDLE, method = RequestMethod.GET)
	public @ResponseBody
	EntityBundle getEntityBundle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@PathVariable Long versionNumber,
			@RequestParam int mask, HttpServletRequest request
			)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException {
		return serviceProvider.getEntityBundleService().getEntityBundle(userId, id, versionNumber, mask, request);
	}	
	
	/**
	 * Create an entity and associated components with a single POST.
	 * Specifically, this operation supports creation of an Entity, its
	 * Annotations, and its ACL.
	 * 
	 * Upon successful creation, an EntityBundle is returned containing the
	 * requested components, as defined by the partsMask in the request object.
	 * 
	 * @param userId
	 * @param ebc - the EntityBundleCreate object containing the Entity and Annotations to create.
	 * @param request
	 * @return
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws ACLInheritanceException
	 * @throws ParseException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_BUNDLE, method = RequestMethod.POST)
	public @ResponseBody
	EntityBundle createEntityBundle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String generatedBy,
			@RequestBody EntityBundleCreate ebc,
			HttpServletRequest request
			)
			throws ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException {
		return serviceProvider.getEntityBundleService().createEntityBundle(userId, ebc, generatedBy, request);
	}
	
	/**
	 * Update an entity and associated components with a single POST.
	 * Specifically, this operation supports update of an Entity, its
	 * Annotations, and its ACL.
	 * 
	 * Upon successful creation, an EntityBundle is returned containing the
	 * requested components, as defined by the partsMask in the request object.
	 * 
	 * @param userId
	 * @param ebc - the EntityBundleCreate object containing the Entity and Annotations to update.
	 * @param request
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
	@RequestMapping(value = UrlHelpers.ENTITY_ID_BUNDLE, method = RequestMethod.PUT)
	public @ResponseBody
	EntityBundle updateEntityBundle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String generatedBy,
			@PathVariable String id,
			@RequestBody EntityBundleCreate ebc,
			HttpServletRequest request
			)
			throws ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException {
		return serviceProvider.getEntityBundleService().updateEntityBundle(userId, id, ebc, generatedBy, request);
	}

}
