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
	 * Gets all the ancestors for the specified node. The returned ancestors are
	 * ordered in that the first the ancestor is the root and the last
	 * ancestor is the parent. The root will get an empty list of ancestors.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ANCESTORS, method = RequestMethod.GET)
	public @ResponseBody EntityIdList getAncestors(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String entityId)
			throws DatastoreException, UnauthorizedException {
		return this.serviceProvider.getNodeTreeQueryService().getAncestors(userId, entityId);
	}

	/**
	 * Gets the parent of the specified node. Root will get the dummy ROOT as its parent.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_PARENT, method = RequestMethod.GET)
	public @ResponseBody EntityId getParent(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String entityId)
			throws DatastoreException, UnauthorizedException {
		return this.serviceProvider.getNodeTreeQueryService().getParent(userId, entityId);
	}

	/**
	 * Gets the paginated list of descendants for the specified node.
	 *
	 * @param pageSize
	 *            Paging parameter. The max number of descendants to fetch per page.
	 * @param lastDescIdExcl
	 *            Paging parameter. The last descendant ID (exclusive).
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_DESCENDANTS, method = RequestMethod.GET)
	public @ResponseBody EntityIdList getDescendants(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String entityId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Integer pageSize,
			@RequestParam(value = ServiceConstants.PAGINATION_LAST_ENTITY_ID, required = false) String lastDescIdExcl)
			throws DatastoreException, UnauthorizedException {
		if (pageSize == null) {
			pageSize = Integer.valueOf(20);
		}
		return this.serviceProvider.getNodeTreeQueryService().getDescendants(
				userId, entityId, pageSize, lastDescIdExcl);
	}

	/**
	 * Gets the paginated list of descendants of a particular generation for the specified node.
	 *
	 * @param generation
	 *            How many generations away from the node. Children are exactly 1 generation away.
	 * @param pageSize
	 *            Paging parameter. The max number of descendants to fetch per page.
	 * @param lastDescIdExcl
	 *            Paging parameter. The last descendant ID (exclusive).
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_DESCENDANTS_GENERATION, method = RequestMethod.GET)
	public @ResponseBody EntityIdList getDescendants(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String entityId,
			@PathVariable(value = UrlHelpers.GENERATION) Integer generation, 
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Integer pageSize,
			@RequestParam(value = ServiceConstants.PAGINATION_LAST_ENTITY_ID, required = false) String lastDescIdExcl)
			throws DatastoreException, UnauthorizedException {
		if (pageSize == null) {
			pageSize = Integer.valueOf(20);
		}
		return this.serviceProvider.getNodeTreeQueryService().getDescendants(
				userId, entityId, generation, pageSize, lastDescIdExcl);
	}

	/**
	 * Gets the children of the specified node.
	 *
	 * @param pageSize
	 *            Paging parameter. The max number of descendants to fetch per page.
	 * @param lastDescIdExcl
	 *            Paging parameter. The last descendant ID (exclusive).
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_CHILDREN, method = RequestMethod.GET)
	public @ResponseBody EntityIdList getChildren(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String entityId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Integer pageSize,
			@RequestParam(value = ServiceConstants.PAGINATION_LAST_ENTITY_ID, required = false) String lastDescIdExcl)
			throws DatastoreException, UnauthorizedException {
		if (pageSize == null) {
			pageSize = Integer.valueOf(20);
		}
		return this.serviceProvider.getNodeTreeQueryService().getChildren(
				userId, entityId, pageSize, lastDescIdExcl);
	}
}
