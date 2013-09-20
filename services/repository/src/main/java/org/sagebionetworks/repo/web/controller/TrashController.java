package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
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
 * The recycle bin (or trash can) is the special folder that holds the deleted entities for users.
 * <p>
 * Services are provided for users to delete entities into the trash can, to view entities
 * in the trash can, to purge entities from the trash can, and to restore entities out
 * of the trash can.
 */
@ControllerInfo(displayName="Recycle Bin Services", path="repo/v1")
@Controller
public class TrashController extends BaseController {

	@Autowired
	private ServiceProvider serviceProvider;

	/**
	 * Moves an entity and its descendants to the trash can.
	 *
	 * @param id The ID of the entity being moved to the trash can.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {UrlHelpers.TRASHCAN_TRASH}, method = RequestMethod.PUT)
	public void moveToTrash(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String currentUserId,
			@PathVariable String id,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		this.serviceProvider.getTrashService().moveToTrash(currentUserId, id);
	}

	/**
	 * Moves an entity and its descendants out of the trash can back to its original parent. An exception
	 * is thrown if the original parent does not exist any more.  In that case, please use
	 * <a href="${PUT.trashcan.restore.id.parentId}">PUT /trashcan/restored/{id}/{parentId}</a> to
	 * restore to a new parent.
	 *
	 * @param id The ID of the entity being restored out of the trash can.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {UrlHelpers.TRASHCAN_RESTORE}, method = RequestMethod.PUT)
	public void restoreFromTrash(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String currentUserId,
			@PathVariable String id,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		this.serviceProvider.getTrashService().restoreFromTrash(currentUserId, id, null);
	}

	/**
	 * Moves an entity and its descendants out of the trash can to a new parent.
	 *
	 * @param id        The ID of the entity being restored out of the trash can.
	 * @param parentId  The ID of the new parent entity.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {UrlHelpers.TRASHCAN_RESTORE_TO_PARENT}, method = RequestMethod.PUT)
	public void restoreFromTrash(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String currentUserId,
			@PathVariable String id,
			@PathVariable String parentId,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		this.serviceProvider.getTrashService().restoreFromTrash(currentUserId, id, parentId);
	}

	/**
	 * Retrieves the paginated list of trash entities deleted by the current user.
	 *
	 * @param offset Paginated results. Offset to the current page.
	 * @param limit  The maximum number of entities to retrieve per page.
	 * @return The paginated list of trash entities.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {UrlHelpers.TRASHCAN_VIEW}, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<TrashedEntity> viewTrashForUser(
	        @RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Long limit,
			HttpServletRequest request) throws DatastoreException, NotFoundException {
		return serviceProvider.getTrashService().viewTrashForUser(userId, userId, offset, limit, request);
	}

	/**
	 * Purges the specified entity from the trash can. Once purging is done, the entity
	 * will be permanently deleted from the system.
	 *
	 * @param id  The ID of the entity to be purged.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {UrlHelpers.TRASHCAN_PURGE_ENTITY}, method = RequestMethod.PUT)
	public void purgeTrashForUser(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		this.serviceProvider.getTrashService().purgeTrashForUser(userId, id);
	}

	/**
	 * Purges everything in the trash can for the current user. Once purging is done, items in
	 * the trash can will permanently removed from the system.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {UrlHelpers.TRASHCAN_PURGE}, method = RequestMethod.PUT)
	public void purgeTrashForUser(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		this.serviceProvider.getTrashService().purgeTrashForUser(userId);
	}

	// For administrators //

	/**
	 * For administrators to view the entire trash can.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {UrlHelpers.ADMIN_TRASHCAN_VIEW}, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<TrashedEntity> viewTrash(
	        @RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String adminUserId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Long limit,
			HttpServletRequest request) throws DatastoreException, NotFoundException {
		return serviceProvider.getTrashService().viewTrash(adminUserId, offset, limit, request);
	}

	/**
	 * For administrators to purge the entire trash can.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {UrlHelpers.ADMIN_TRASHCAN_PURGE}, method = RequestMethod.PUT)
	public void purge(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String adminUserId,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		this.serviceProvider.getTrashService().purgeTrash(adminUserId);
	}
}
