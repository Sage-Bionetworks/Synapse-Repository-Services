package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.net.URL;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.BaseController;
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

@ControllerInfo(displayName = "WikiPage Services 2", path = "repo/v1")
@Controller
public class V2WikiController extends BaseController {
	@Autowired
	ServiceProvider serviceProvider;
	
	/**
	 * Create a WikiPage with an Entity as an owner.
	 * 
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.CREATE</a> permission on the owner.
	 * </p>
	 * <p>
	 * If the passed WikiPage is a root (parentWikiId = null) and the owner
	 * already has a root WikiPage, an error will be returned.
	 * </p>
	 * 
	 * @param userId
	 *            The user's id.
	 * @param ownerId
	 *            The ID of the owner Entity.
	 * @param toCreate
	 *            The WikiPage to create.
	 * @return -
	 * @throws DatastoreException
	 *             - Synapse error.
	 * @throws NotFoundException
	 *             - returned if the user or owner does not exist.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_V2, method = RequestMethod.POST)
	public @ResponseBody
	V2WikiPage createEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @RequestBody V2WikiPage toCreate)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getV2WikiService().createWikiPage(userId, ownerId,
				ObjectType.ENTITY, toCreate);
	}

	/**
	 * Create a WikiPage with an Evaluation as an owner.
	 * 
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.CREATE</a> permission on the owner.
	 * </p>
	 * <p>
	 * If the passed WikiPage is a root (parentWikiId = null) and the owner
	 * already has a root WikiPage, an error will be returned.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owner Evaluation.
	 * @param toCreate
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_V2, method = RequestMethod.POST)
	public @ResponseBody
	V2WikiPage createCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @RequestBody V2WikiPage toCreate)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getV2WikiService().createWikiPage(userId, ownerId,
				ObjectType.EVALUATION, toCreate);
	}

	/**
	 * Get the root WikiPage for an Entity.
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Entity.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_V2, method = RequestMethod.GET)
	public @ResponseBody
	V2WikiPage getEntityRootWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getRootWikiPage(userId,
				ownerId, ObjectType.ENTITY);
	}

	/**
	 * Get the root WikiPage for an Evaluation.
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Evaluation.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_V2, method = RequestMethod.GET)
	public @ResponseBody
	V2WikiPage getCompetitionRootWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getRootWikiPage(userId,
				ownerId, ObjectType.EVALUATION);
	}

	/**
	 * Get a specific WikiPage of of an Entity.
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Entity.
	 * @param wikiId
	 *            The ID of the WikiPage to get.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_V2, method = RequestMethod.GET)
	public @ResponseBody
	V2WikiPage getEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @PathVariable String wikiId)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getV2WikiService().getWikiPage(userId,
				new WikiPageKey(ownerId, ObjectType.ENTITY, wikiId));
	}

	/**
	 * Get a specific WikiPage of of an Evaluation.
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Evaluation.
	 * @param wikiId
	 *            The ID of the WikiPage to get.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_V2, method = RequestMethod.GET)
	public @ResponseBody
	V2WikiPage getCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @PathVariable String wikiId)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getV2WikiService().getWikiPage(userId,
				new WikiPageKey(ownerId, ObjectType.EVALUATION, wikiId));
	}

	// Update methods.

	/**
	 * Update a specific WikiPage of an Entity.
	 * <p>
	 * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle
	 * concurrent updates. Each time a WikiPage is updated a new etag will be
	 * issued to the WikiPage. When an update is request, Synapse will compare
	 * the etag of the passed WikiPage with the current etag of the WikiPage. If
	 * the etags do not match, then the update will be rejected with a
	 * PRECONDITION_FAILED (412) response. When this occurs the caller should
	 * get the latest copy of the WikiPage and re-apply any changes to the
	 * object, then re-attempt the update. This ensures the caller has all
	 * changes applied by other users before applying their own changes.
	 * </p>
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Entity.
	 * @param wikiId
	 *            The ID of the WikiPage to update.
	 * @param toUpdate
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_V2, method = RequestMethod.PUT)
	public @ResponseBody
	V2WikiPage updateEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestBody V2WikiPage toUpdate) throws DatastoreException,
			NotFoundException {
		validateUpateArguments(wikiId, toUpdate);
		return serviceProvider.getV2WikiService().updateWikiPage(userId, ownerId,
				ObjectType.ENTITY, toUpdate);
	}

	/**
	 * Update a specific WikiPage of an Evaluation.
	 * <p>
	 * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle
	 * concurrent updates. Each time a WikiPage is updated a new etag will be
	 * issued to the WikiPage. When an update is request, Synapse will compare
	 * the etag of the passed WikiPage with the current etag of the WikiPage. If
	 * the etags do not match, then the update will be rejected with a
	 * PRECONDITION_FAILED (412) response. When this occurs the caller should
	 * get the latest copy of the WikiPage and re-apply any changes to the
	 * object, then re-attempt the update. This ensures the caller has all
	 * changes applied by other users before applying their own changes.
	 * </p>
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Evaluation.
	 * @param wikiId
	 *            The ID of the WikiPage to update.
	 * @param toUpdate
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_V2, method = RequestMethod.PUT)
	public @ResponseBody
	V2WikiPage updateCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestBody V2WikiPage toUpdate) throws DatastoreException,
			NotFoundException {
		validateUpateArguments(wikiId, toUpdate);
		return serviceProvider.getV2WikiService().updateWikiPage(userId, ownerId,
				ObjectType.EVALUATION, toUpdate);
	}

	/**
	 * Helper to validate update arguments.
	 * 
	 * @param wikiId
	 * @param wikiPage
	 */
	private void validateUpateArguments(String wikiId, V2WikiPage wikiPage) {
		if (wikiPage == null)
			throw new IllegalArgumentException("WikiPage cannot be null");
		if (!wikiId.equals(wikiPage.getId())) {
			throw new IllegalArgumentException(
					"Path variable wikiId does not match the ID of the passed WikiPage");
		}
	}

	// Restore methods
	
	/**
	 * Restore content of a specific WikiPage of an Entity to another state.
	 * <p>
	 * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle
	 * concurrent updates. Each time a WikiPage is updated a new etag will be
	 * issued to the WikiPage. When an update is request, Synapse will compare
	 * the etag of the passed WikiPage with the current etag of the WikiPage. If
	 * the etags do not match, then the update will be rejected with a
	 * PRECONDITION_FAILED (412) response. When this occurs the caller should
	 * get the latest copy of the WikiPage and re-apply any changes to the
	 * object, then re-attempt the update. This ensures the caller has all
	 * changes applied by other users before applying their own changes.
	 * </p>
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param version
	 * 			  A unique number associated with a WikiPage's contents at a certain point in time
	 * @param ownerId
	 *            The ID of the owning Evaluation.
	 * @param wikiId
	 *            The ID of the WikiPage to update.
	 * @param toUpdate
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_AND_VERSION, method = RequestMethod.PUT)
	public @ResponseBody
	V2WikiPage restoreEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(ServiceConstants.WIKI_VERSION) Long version,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestBody V2WikiPage toUpdate) throws DatastoreException,
			NotFoundException {
		validateUpateArguments(wikiId, toUpdate);
		return serviceProvider.getV2WikiService().restoreWikipage(userId, ownerId, ObjectType.ENTITY, toUpdate, version);
	}
	
	/**
	 *  Restore content of a specific WikiPage of a Evaluation to another state.
	 * <p>
	 * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle
	 * concurrent updates. Each time a WikiPage is updated a new etag will be
	 * issued to the WikiPage. When an update is request, Synapse will compare
	 * the etag of the passed WikiPage with the current etag of the WikiPage. If
	 * the etags do not match, then the update will be rejected with a
	 * PRECONDITION_FAILED (412) response. When this occurs the caller should
	 * get the latest copy of the WikiPage and re-apply any changes to the
	 * object, then re-attempt the update. This ensures the caller has all
	 * changes applied by other users before applying their own changes.
	 * </p>
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param version
	 * 			  A unique number associated with a WikiPage's contents at a certain point in time
	 * @param ownerId
	 *            The ID of the owning Evaluation.
	 * @param wikiId
	 *            The ID of the WikiPage to update.
	 * @param toUpdate
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_AND_VERSION, method = RequestMethod.PUT)
	public @ResponseBody
	V2WikiPage restoreCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = "version", required = false) Long version,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestBody V2WikiPage toUpdate) throws DatastoreException,
			NotFoundException {
		validateUpateArguments(wikiId, toUpdate);
		return serviceProvider.getV2WikiService().restoreWikipage(userId, ownerId, ObjectType.EVALUATION, toUpdate, version);
	}
	
	// Delete methods

	/**
	 * Delete a specific WikiPage of an Entity.
	 * <p>
	 * Note: When a WikiPage is deleted, the delete will cascade to all children
	 * WikiPages (recursively) of the deleted WikiPage.
	 * </p>
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.DELETE</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Entity.
	 * @param wikiId
	 *            The ID of the WikiPage to delete.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_V2, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @PathVariable String wikiId)
			throws DatastoreException, NotFoundException {
		serviceProvider.getV2WikiService().deleteWikiPage(userId,
				new WikiPageKey(ownerId, ObjectType.ENTITY, wikiId));
	}

	/**
	 * Delete a specific WikiPage of an Evaluation.
	 * <p>
	 * Note: When a WikiPage is deleted, the delete will cascade to all children
	 * WikiPages (recursively) of the deleted WikiPage.
	 * </p>
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.DELETE</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Evaluations.
	 * @param wikiId
	 *            The ID of the WikiPage to delete.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_V2, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @PathVariable String wikiId)
			throws DatastoreException, NotFoundException {
		serviceProvider.getV2WikiService().deleteWikiPage(userId,
				new WikiPageKey(ownerId, ObjectType.EVALUATION, wikiId));
	}

	// Get Wiki Hierarchy

	/**
	 * Get a paginated list of all <a
	 * href="${org.sagebionetworks.repo.model.wiki.WikiHeader}">WikiHeaders</a>
	 * that belong to the given owner Entity. The resulting list can be used to
	 * build a tree of the WikiPages for this owner. The first WikiHeader will
	 * be for the root WikiPage (parentWikiId = null).
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param offset
	 *            The index of the pagination offset. For a page size of 10, the
	 *            first page would be at offset = 0, and the second page would
	 *            be at offset = 10.
	 * @param limit
	 *            Limits the size of the page returned. For example, a page size
	 *            of 10 require limit = 10.
	 * @param ownerId
	 *            The ID of the owning Entity.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_TREE_V2, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<V2WikiHeader> getEntityWikiHeaderTree(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Long limit,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getWikiHeaderTree(userId,
				ownerId, ObjectType.ENTITY, limit, offset);
	}

	/**
	 * Get a paginated list of all <a
	 * href="${org.sagebionetworks.repo.model.wiki.WikiHeader}">WikiHeaders</a>
	 * that belong to the given owner Evaluation. The resulting list can be used
	 * to build a tree of the WikiPages for this owner. The first WikiHeader
	 * will be for the root WikiPage (parentWikiId = null).
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param offset
	 *            The index of the pagination offset. For a page size of 10, the
	 *            first page would be at offset = 0, and the second page would
	 *            be at offset = 10.
	 * @param limit
	 *            Limits the size of the page returned. For example, a page size
	 *            of 10 require limit = 10.
	 * @param ownerId
	 *            The ID of the owning Evaluation.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_TREE_V2, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<V2WikiHeader> getCompetitionWikiHeaderTree(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Long limit,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getWikiHeaderTree(userId,
				ownerId, ObjectType.EVALUATION, limit, offset);
	}
	
	// Wiki History
	
	/**
	 * Get a paginated list of V2WikiHistorySnapshots for an Entity. This can be
	 * used to construct a history of the changes made to the Entity. The
	 * first snapshot will be the most recent modification.
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param offset
	 * 			The index of the pagination offset. For a page size of 10, the
	 *          first page would be at offset = 0, and the second page would
	 *          be at offset = 10.
	 * @param limit
	 *            Limits the size of the page returned. For example, a page size
	 *            of 10 require limit = 10.
	 * @param wikiId
	 * @param ownerId
	 *            The ID of the owning Entity.
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_HISTORY_V2, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<V2WikiHistorySnapshot> getEntityWikiHistory(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(ServiceConstants.PAGINATION_OFFSET_PARAM) Long offset,
			@RequestParam(ServiceConstants.PAGINATION_LIMIT_PARAM) Long limit,
			@PathVariable String wikiId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getWikiHistory(userId, ownerId, ObjectType.ENTITY, 
				limit, offset, new WikiPageKey(ownerId, ObjectType.ENTITY, wikiId));

	}
	
	/**
	 * Get a paginated list of V2WikiHistorySnapshots for an Evaluation.
	 * This can be used to construct a history of the changes made to the Evaluation. 
	 * The first snapshot will be the most recent modification.
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param offset
	 * 			The index of the pagination offset. For a page size of 10, the
	 *          first page would be at offset = 0, and the second page would
	 *          be at offset = 10.
	 * @param limit
	 *            Limits the size of the page returned. For example, a page size
	 *            of 10 require limit = 10.
	 * @param wikiId
	 * @param ownerId
	 *            The ID of the owning Evaluation.
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_HISTORY_V2, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<V2WikiHistorySnapshot> getCompetitionWikiHistory(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(ServiceConstants.PAGINATION_OFFSET_PARAM) Long offset,
			@RequestParam(ServiceConstants.PAGINATION_LIMIT_PARAM) Long limit,
			@PathVariable String wikiId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getWikiHistory(userId, ownerId, ObjectType.EVALUATION, 
				limit, offset, new WikiPageKey(ownerId, ObjectType.EVALUATION, wikiId));

	}
	
	// Handles
	/**
	 * Get the list of FileHandles for all file attachments of a specific
	 * WikiPage for a given owning Entity. This list will include Previews if
	 * they exist and will provide information about file sizes, content types
	 * and names.
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Entity.
	 * @param wikiId
	 *            The ID of the WikiPage.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_ATTCHMENT_HANDLE_V2, method = RequestMethod.GET)
	public @ResponseBody
	FileHandleResults getEntityWikiAttachmenthHandles(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @PathVariable String wikiId)
			throws DatastoreException, NotFoundException {
		// Get the redirect url
		return serviceProvider.getV2WikiService().getAttachmentFileHandles(
				userId, new WikiPageKey(ownerId, ObjectType.ENTITY, wikiId));
	}

	/**
	 * Get the list of FileHandles for all file attachments of a specific
	 * WikiPage for a given owning Evaluation. This list will include Previews
	 * if they exist and will provide information about file sizes, content
	 * types and names.
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Evaluation.
	 * @param wikiId
	 *            The ID of the WikiPage.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_ATTCHMENT_HANDLE_V2, method = RequestMethod.GET)
	public @ResponseBody
	FileHandleResults getCompetitionWikiAttachmenthHandles(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @PathVariable String wikiId)
			throws DatastoreException, NotFoundException {
		// Get the redirect url
		return serviceProvider
				.getV2WikiService()
				.getAttachmentFileHandles(userId,
						new WikiPageKey(ownerId, ObjectType.EVALUATION, wikiId));
	}

	// Files
	/**
	 * Get a URL that can be used to download a file for a given WikiPage file
	 * attachment.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Entity
	 * @param wikiId
	 *            The ID of the WikiPage
	 * @param fileName
	 *            The name of the file to get. The file names can be found in
	 *            the FileHandles from the <a
	 *            href="${GET.entity.ownerId.wiki.wikiId.attachmenthandles}">GET
	 *            /entity/{ownerId}/wiki/{wikiId}/attachmenthandles</a> method.
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_ATTCHMENT_FILE_V2, method = RequestMethod.GET)
	public @ResponseBody
	void getEntityWikiAttachmentFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = true)  String fileName,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		URL redirectUrl = serviceProvider.getV2WikiService()
				.getAttachmentRedirectURL(userId,
						new WikiPageKey(ownerId, ObjectType.ENTITY, wikiId),
						fileName);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get a URL that can be used to download a file for a given WikiPage file
	 * attachment.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Evaluation
	 * @param wikiId
	 *            The ID of the WikiPage
	 * @param fileName
	 *            The name of the file to get. The file names can be found in
	 *            the FileHandles from the <a
	 *            href="${GET.evaluation.ownerId.wiki.wikiId.attachmenthandles}"
	 *            >GET /evaluation/{ownerId}/wiki/{wikiId}/attachmenthandles</a>
	 *            method.
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_ATTCHMENT_FILE_V2, method = RequestMethod.GET)
	public @ResponseBody
	void getCompetitionAttachmentFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = true) String fileName,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		URL redirectUrl = serviceProvider
				.getV2WikiService()
				.getAttachmentRedirectURL(
						userId,
						new WikiPageKey(ownerId, ObjectType.EVALUATION, wikiId),
						fileName);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get a URL that can be used to download a preview file for a given
	 * WikiPage file attachment.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Entity
	 * @param wikiId
	 *            The ID of the WikiPage
	 * @param fileName
	 *            The name of the file to get. The file names can be found in
	 *            the FileHandles from the <a
	 *            href="${GET.entity.ownerId.wiki.wikiId.attachmenthandles}">GET
	 *            /entity/{ownerId}/wiki/{wikiId}/attachmenthandles</a> method.
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_ATTCHMENT_FILE_PREVIEW_V2, method = RequestMethod.GET)
	public @ResponseBody
	void getEntityWikiAttachmenPreviewFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = true) String fileName,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		URL redirectUrl = serviceProvider.getV2WikiService()
				.getAttachmentPreviewRedirectURL(userId,
						new WikiPageKey(ownerId, ObjectType.ENTITY, wikiId),
						fileName);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get a URL that can be used to download a preview file for a given
	 * WikiPage file attachment.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Evaluation
	 * @param wikiId
	 *            The ID of the WikiPage
	 * @param fileName
	 *            The name of the file to get. The file names can be found in
	 *            the FileHandles from the <a
	 *            href="${GET.evaluation.ownerId.wiki.wikiId.attachmenthandles}"
	 *            >GET /evaluation/{ownerId}/wiki/{wikiId}/attachmenthandles</a>
	 *            method.
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_ATTCHMENT_FILE_PREVIEW_V2, method = RequestMethod.GET)
	public @ResponseBody
	void getCompetitionAttachmenthPreviewFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = true) String fileName,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		URL redirectUrl = serviceProvider
				.getV2WikiService()
				.getAttachmentPreviewRedirectURL(
						userId,
						new WikiPageKey(ownerId, ObjectType.EVALUATION, wikiId),
						fileName);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
}
