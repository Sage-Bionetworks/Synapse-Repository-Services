package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.web.UrlHelpers.ID_PATH_VARIABLE;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptorResponse;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementConversionRequest;
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
 * These services manage the Access Requirements/Restrictions (ARs) which may be placed on Entities,
 * or Teams.  An Access Requirement specifies the type of access being restricted as well
 * as how the requirement is fulfilled. 
 * <p>
 * ARs complement Access Control Lists (ACLs) for managing access to Synapse objects.
 * While ACLs are managed by entity owners, ARs are managed by the Synapse Access and Compliance Team (ACT), which is
 * responsible for governance of sensitive data.  Before one may access data associated with an 
 * AR, there must be a corresponding Access Approval.  For certain ARs --
 * of the "self-sign" variety -- one may grant ones own approval by agreeing to associated
 * 'terms of use.'  For other Access Requirements -- of the 'ACT' variety -- approval may be granted
 * only by the ACT.
 * </p>
 * <p>
 * As stated above, an AR specifies the type of access being controlled.  Generally
 * entities are restricted with DOWNLOAD access.  A Synapse user may be able to see that a Synapse
 * File exists, but be unable to download the content due to such an AR.  Teams are
 * restricted using the PARTICIPATE access type:  Prior to joining a Team a user must fulfill any
 * associated ARs controlling this type of access.
 * </p>
 * <p>
 * Entity ARs are inherited from ancestors.  E.g. an AR applied to a Folder will control all Files in the Folder, 
 * or within sub-folders of the Folder.  Access Requirements are cumulative:  A File will be controlled both
 * by ARs applied to it directly and by ARs applied to any and all of its ancestors.
 * </p>
 * <p>
 * Access Requirements are fulfilled on a per-user basis using the <a href="#org.sagebionetworks.repo.web.controller.AccessApprovalController">
 * Access Approval Services</a>.
 * </p>
 * 
 *
 */
@ControllerInfo(displayName="Access Requirement Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class AccessRequirementController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Add an Access Requirement to an Entity, or Team.  
	 * This service may only be used by the Synapse Access and Compliance Team.
	 * @param userId
	 * @param accessRequirement the Access Requirement to create
	 * @return
	 * @throws Exception
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT, method = RequestMethod.POST)
	public @ResponseBody
	AccessRequirement createAccessRequirement(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody AccessRequirement accessRequirement) throws Exception {
		return serviceProvider.getAccessRequirementService().createAccessRequirement(userId, accessRequirement);
	}	
	/**
	 * Get an Access Requirement to an Entity, or Team based on its ID.  
	 * 
	 * @param requirementId
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WITH_REQUIREMENT_ID, method = RequestMethod.GET)
	public @ResponseBody
	AccessRequirement 
	getAccessRequirement(
			@PathVariable String requirementId
			) throws DatastoreException, UnauthorizedException, NotFoundException {	
		return serviceProvider.getAccessRequirementService().getAccessRequirement(requirementId);
	}

	/**
	 * Modify an existing Access Requirement.
	 * This service may only be used by the Synapse Access and Compliance Team.
	 * @param userId
	 * @param requirementId the ID of the Access Requirement to be modified.
	 * @param accessRequirement  The modified Access Requirement.
	 * @return
	 * @throws Exception
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WITH_REQUIREMENT_ID, method = RequestMethod.PUT)
	public @ResponseBody
	AccessRequirement updateAccessRequirement(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String requirementId,
			@RequestBody AccessRequirement accessRequirement
			) throws Exception {
		return serviceProvider.getAccessRequirementService().updateAccessRequirement(userId, requirementId, accessRequirement);
	}
	

	/**
	 * Add a temporary access restriction that prevents access pending review by the Synapse Access and Compliance Team.  
	 * This service may be used only by an administrator of the specified entity.
	 * @param userId
	 * @param id the ID of the entity to which an Access Requirement will be applied
	 * @return
	 * @throws Exception
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_LOCK_ACCESS_REQURIEMENT, method = RequestMethod.POST)
	public @ResponseBody
	AccessRequirement createLockAccessRequirement(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id
			) throws Exception {
		return serviceProvider.getAccessRequirementService().createLockAccessRequirement(userId, id);
	}
	

	/**
	 * Retrieve all unfulfilled Access Requirements (of type DOWNLOAD) for an entity.
	 * @param userId
	 * @param entityId the id of the entity whose unmet Access Requirements are retrieved
	 * @param accessType the type of access to filter on
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ACCESS_REQUIREMENT_UNFULFILLED_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<AccessRequirement>
	 getUnfulfilledEntityAccessRequirement(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = ID_PATH_VARIABLE) String entityId,
			@RequestParam(value = AuthorizationConstants.ACCESS_TYPE_PARAM, required = false) ACCESS_TYPE accessType
			) throws DatastoreException, UnauthorizedException, NotFoundException {
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);
		return serviceProvider.getAccessRequirementService().getUnfulfilledAccessRequirements(userId, subjectId, accessType);
	}

	/**
	 * Retrieve all unfulfilled Access Requirements (of type PARTICIPATE) for a Team.
	 * @param userId
	 * @param id the ID of the Team whose unfulfilled Access Requirements are retrived.
	 * @param accessType the type of access to filter on
	 * @param loginRequest
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.TEAM_ACCESS_REQUIREMENT_UNFULFILLED_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<AccessRequirement>
	 getUnfulfilledTeamAccessRequirement(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.ACCESS_TYPE_PARAM, required = true) ACCESS_TYPE accessType
	) throws DatastoreException, UnauthorizedException, NotFoundException {
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(id);
		subjectId.setType(RestrictableObjectType.TEAM);
		return serviceProvider.getAccessRequirementService().getUnfulfilledAccessRequirements(userId, subjectId, accessType);
	}
	/**
	 * Retrieve paginated list of ALL Access Requirements associated with an entity.
	 * @param userId
	 * @param entityId the id of the entity whose Access Requirements are retrieved
	 * @param limit - Limits the size of the page returned. For example, a page size of 10 require limit = 10. The maximum limit for this call is 50.
	 * @param offset - The index of the pagination offset. For a page size of 10, the first page would be at offset = 0, and the second page would be at offset = 10.
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WITH_ENTITY_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<AccessRequirement>
	 getEntityAccessRequirements(
				@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
				@PathVariable(value = ID_PATH_VARIABLE) String entityId,
				@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Long limit,
				@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false) Long offset
			) throws DatastoreException, UnauthorizedException, NotFoundException {
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);
		return serviceProvider.getAccessRequirementService().getAccessRequirements(userId, subjectId, limit, offset);
	}

	/**
	 * Retrieve paginated list of ALL Access Requirements associated with a Team.
	 * @param userId
	 * @param id the ID of the Team whose Access Requirements are retrieved.
	 * @param loginRequest
	 * @param limit - Limits the size of the page returned. For example, a page size of 10 require limit = 10. The maximum limit for this call is 50.
	 * @param offset - The index of the pagination offset. For a page size of 10, the first page would be at offset = 0, and the second page would be at offset = 10.
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WITH_TEAM_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<AccessRequirement>
	 getTeamAccessRequirements(
				@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
				@PathVariable String id,
				@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Long limit,
				@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false) Long offset
				) throws DatastoreException, UnauthorizedException, NotFoundException {
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(id);
		subjectId.setType(RestrictableObjectType.TEAM);
		return serviceProvider.getAccessRequirementService().getAccessRequirements(userId, subjectId, limit, offset);
	}

	/**
	 * Delete an Access Requirement.
	 * This service may only be used by the Synapse Access and Compliance Team.
	 * @param userId
	 * @param requirementId the ID of the requirement to delete
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WITH_REQUIREMENT_ID, method = RequestMethod.DELETE)
	public void deleteAccessRequirements(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String requirementId
			) throws DatastoreException, UnauthorizedException, NotFoundException {	
		serviceProvider.getAccessRequirementService().deleteAccessRequirements(userId, requirementId);
	}

	/**
	 * Convert an ACTAccessRequirement to a ManagedACTAccessRequirement.
	 * Only ACT member can perform this action.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 * @throws ConflictingUpdateException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_CONVERSION, method = RequestMethod.PUT)
	public @ResponseBody AccessRequirement convertAccessRequirement(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody AccessRequirementConversionRequest request
			) throws ConflictingUpdateException, UnauthorizedException, NotFoundException {	
		return serviceProvider.getAccessRequirementService().convertAccessRequirements(userId, request);
	}

	/**
	 * Retrieve a page of subjects for a given Access Requirement ID.
	 * 
	 * @param requirementId
	 * @param nextPageToken
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WITH_REQUIREMENT_ID_SUBJECTS, method = RequestMethod.GET)
	public @ResponseBody RestrictableObjectDescriptorResponse getSubjects(
			@PathVariable String requirementId,
			@RequestParam(value = "nextPageToken", required = false) String nextPageToken) {
		return serviceProvider.getAccessRequirementService().getSubjects(requirementId, nextPageToken);
	}
}
