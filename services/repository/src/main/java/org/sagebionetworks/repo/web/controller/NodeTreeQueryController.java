package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
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

@ControllerInfo(displayName="Entity Ancestors Services", path="repo/v1")
@Controller
public class NodeTreeQueryController extends BaseController {

	@Autowired
	private ServiceProvider serviceProvider;

	/**
	 * Gets all the ancestors for the specified entity. The returned ancestors are
	 * ordered in that the first ancestor is the root and the last
	 * ancestor is the parent. The root itself will retrieve an empty list of ancestors.
	 *
	 * @param id The ID of the entity whose ancestors will be retrieved.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ANCESTORS, method = RequestMethod.GET)
	public @ResponseBody EntityIdList getAncestors(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String id)
			throws DatastoreException, UnauthorizedException {
		return this.serviceProvider.getNodeTreeQueryService().getAncestors(userId, id);
	}

	/**
	 * Gets the parent of the specified entity. Root will get the dummy ROOT as its parent.
	 *
	 * @param id The ID of the entity whose parent will be retrieved.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_PARENT, method = RequestMethod.GET)
	public @ResponseBody EntityId getParent(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String id)
			throws DatastoreException, UnauthorizedException {
		return this.serviceProvider.getNodeTreeQueryService().getParent(userId, id);
	}

	/**
	 * Gets the paginated list of descendants for the specified entity.
	 *
	 * @param id
	 *            The ID of the entity whose descendants will be retrieved.
	 * @param limit
	 *            Paging parameter. The max number of descendants to fetch per page.
	 * @param lastEntityId
	 *            Paging parameter. The ID of the last descendant on the current page. The next page will start after this last descendant.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_DESCENDANTS, method = RequestMethod.GET)
	public @ResponseBody EntityIdList getDescendants(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String id,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_LAST_ENTITY_ID, required = false) String lastEntityId)
			throws DatastoreException, UnauthorizedException {
		if (limit == null) {
			limit = Integer.valueOf(20);
		}
		return this.serviceProvider.getNodeTreeQueryService().getDescendants(
				userId, id, limit, lastEntityId);
	}

	/**
	 * Gets the paginated list of descendants of a particular generation for the specified entity.
	 *
	 * @param id
	 *            The ID of the entity whose descendants will be retrieved.
	 * @param generation
	 *            How many generations away from the node. Example, children are exactly 1 generation away.
	 * @param limit
	 *            Paging parameter. The max number of descendants to fetch per page.
	 * @param lastEntityId
	 *            Paging parameter. The ID of the last descendant on the current page. The next page will start after this last descendant.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_DESCENDANTS_GENERATION, method = RequestMethod.GET)
	public @ResponseBody EntityIdList getDescendants(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String id,
			@PathVariable(value = UrlHelpers.GENERATION) Integer generation, 
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_LAST_ENTITY_ID, required = false) String lastEntityId)
			throws DatastoreException, UnauthorizedException {
		if (limit == null) {
			limit = Integer.valueOf(20);
		}
		return this.serviceProvider.getNodeTreeQueryService().getDescendants(
				userId, id, generation, limit, lastEntityId);
	}

	/**
	 * Gets the children of the specified node.
	 *
	 * @param id
	 *            The ID of the entity whose descendants will be retrieved.
	 * @param generation
	 *            How many generations away from the node. Example, children are exactly 1 generation away.
	 * @param limit
	 *            Paging parameter. The max number of descendants to fetch per page.
	 * @param lastEntityId
	 *            Paging parameter. The ID of the last descendant on the current page. The next page will start after this last descendant.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_CHILDREN, method = RequestMethod.GET)
	public @ResponseBody EntityIdList getChildren(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String id,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_LAST_ENTITY_ID, required = false) String lastEntityId)
			throws DatastoreException, UnauthorizedException {
		if (limit == null) {
			limit = Integer.valueOf(20);
		}
		return this.serviceProvider.getNodeTreeQueryService().getChildren(
				userId, id, limit, lastEntityId);
	}
}
