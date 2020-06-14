package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

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
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
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
 * <li> HasChildren <i>(Boolean)</i> = 0x20 </li>
 * <li> ACL <i>(AccessControlList)</i> = 0x40 </li>
 * <li> File Handles <i>(List&lt;FileHandle&gt;)</i> = 0x800 </li>
 * <li> TableEntity Metadata <i>(TableBundle)</i> = 0x1000</li>
 * <li> Root Wiki ID <i>(String)</i> = 0x2000</li>
 * <li> Benefactor ACL <i>(AccessControlList)</i> = 0x4000</li>
 * <li> DOI Association <i>(DoiAssociation)</i> = 0x8000</li>
 * <li> File Name <i>(String)</i> = 0x10000</li>
 * <li> Thread Count <i>(Long)</i> = 0x20000</li>
 * <li> Restriction Information <i>(RestrictionInformationResponse)</i> = 0x40000</li>
 * </ul>
 * </p>
 * <p>
 * For example, if the Entity and its Annotations are desired, the request mask value should be
 * 0x1 + 0x2 = 0x3.
 * </p>
 */
@Deprecated
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class EntityBundleController {
	
	@Autowired
	ServiceProvider serviceProvider;
	
	/**
	 * Get an entity and related data with a single GET.
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
	@Deprecated
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID_BUNDLE, method = RequestMethod.GET)
	public @ResponseBody
	EntityBundle getEntityBundle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, 
			@RequestParam int mask)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException {
		return serviceProvider.getEntityBundleService().getEntityBundle(userId, id, mask);
	}	

	/**
	 * Get an entity at a specific version and its related data with a single GET.
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
	@Deprecated
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_VERSION_NUMBER_BUNDLE, method = RequestMethod.GET)
	public @ResponseBody
	EntityBundle getEntityBundle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id,
			@PathVariable Long versionNumber,
			@RequestParam int mask)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException {
		return serviceProvider.getEntityBundleService().getEntityBundle(userId, id, versionNumber, mask);
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
	@Deprecated
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.GONE)
	@RequestMapping(value = UrlHelpers.ENTITY_BUNDLE, method = RequestMethod.POST)
	public @ResponseBody
	String createEntityBundle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String generatedBy,
			@RequestBody EntityBundleCreate ebc)
			throws ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException {
		return "See "+UrlHelpers.ENTITY_BUNDLE_V2_CREATE;
	}
	
	/**
	 * Update an entity and associated components with a single POST.
	 * Specifically, this operation supports update of an Entity, its
	 * Annotations, and its ACL.
	 * 
	 * Upon successful creation, an EntityBundle is returned containing the
	 * requested components, as defined by the partsMask in the request object.
	 * 
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
	@Deprecated
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.GONE)
	@RequestMapping(value = UrlHelpers.ENTITY_ID_BUNDLE, method = RequestMethod.PUT)
	public @ResponseBody
	String updateEntityBundle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String generatedBy,
			@PathVariable String id,
			@RequestBody EntityBundleCreate ebc)
			throws ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException {
		return "See "+UrlHelpers.ENTITY_ID_BUNDLE_V2;
	}
}
