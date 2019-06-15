package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
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
 * The Synapse <a
 * href="${org.sagebionetworks.repo.model.wiki.WikiPage}">WikiPage</a> model
 * object contains the data needed to render an end-user crafted web page. The
 * Synapse Web Client will dynamically render a WikiPage into a combination of
 * HTML, CSS and Javascript which is then finally rendered as a web page in the
 * client's web browser.
 * </p>
 * <p>
 * These services provide support for creating, reading, updating, and
 * deleting (CRUD) the WikiPage model objects.
 * </p>
 * <p>
 * WikiPages are composed of two major parts; the raw markdown text and a list
 * of file attachments. For example, to embed an image from an end-user's
 * machine into a WikiPage, the image file must first be uploaded to Synapse as <a
 * href="${org.sagebionetworks.repo.model.file.FileHandle}">FileHandle</a> (see
 * <a href="${org.sagebionetworks.file.controller.UploadController}">File
 * Services</a>). The FileHandle ID can then be added to a
 * WikiPage.attachmentFileHandleIds list. See <a
 * href="https://www.synapse.org">www.synapse.org</a> for details on the
 * supported markdown syntax.
 * </p>
 * 
 * <p>
 * WikiPages are not stand-alone objects, instead they are a component of
 * another object such as an Entity or Evaluation. For example, when a WikiPage
 * is created for an Entity, the Entity becomes the "owner" of the WikiPage.
 * Access to the WikiPage is always tied to its owner. For example, to GET a
 * WikiPage of an Entity, the caller must have read permission on the Entity.
 * </p>
 * <p>
 * To navigate the hierarchy of WikiPages associated with an owner use the <a
 * href="${GET.entity.ownerId.wikiheadertree}">GET
 * /entity/{ownerId}/wikiheadertree</a> method. The returned list of <a
 * href="${org.sagebionetworks.repo.model.wiki.WikiHeader}">WikiHeaders</a> can
 * be used to construct a full wiki hierarchy tree for that owner.
 * </p>
 * <p>
 * Note: WikiPages can be nested to created a hierarchy of sub-pages. However,
 * there can only be one root WikiPage per owner object, and all sub-pages are
 * considered to be owned by the same object as the root page.
 * </p>
 */
@ControllerInfo(displayName = "WikiPage Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class WikiController {

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
	 * @throws IOException 
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI, method = RequestMethod.POST)
	public @ResponseBody
	WikiPage createEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @RequestBody WikiPage toCreate)
			throws DatastoreException, NotFoundException, IOException {
		return serviceProvider.getWikiService().createWikiPage(userId, ownerId,
				ObjectType.ENTITY, toCreate);
	}
	
	/**
	 * Create a WikiPage with an Access Requirement as an owner.
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
	 * @throws IOException 
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI, method = RequestMethod.POST)
	public @ResponseBody
	WikiPage createAccessRequirementWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @RequestBody WikiPage toCreate)
			throws DatastoreException, NotFoundException, IOException {
		return serviceProvider.getWikiService().createWikiPage(userId, ownerId,
				ObjectType.ACCESS_REQUIREMENT, toCreate);
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
	 * @throws IOException 
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI, method = RequestMethod.POST)
	public @ResponseBody
	WikiPage createCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @RequestBody WikiPage toCreate)
			throws DatastoreException, NotFoundException, IOException {
		return serviceProvider.getWikiService().createWikiPage(userId, ownerId,
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
	 * @throws IOException 
	 * @throws UnauthorizedException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI, method = RequestMethod.GET)
	public @ResponseBody
	WikiPage getEntityRootWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException, UnauthorizedException, IOException {
		return serviceProvider.getWikiService().getRootWikiPage(userId,
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
	 * @param wikiVersion When included returns a specific version of a wiki.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 * @throws UnauthorizedException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI, method = RequestMethod.GET)
	public @ResponseBody
	WikiPage getCompetitionRootWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException, UnauthorizedException, IOException {
		return serviceProvider.getWikiService().getRootWikiPage(userId,
				ownerId, ObjectType.EVALUATION);
	}
	
	/**
	 * Get the root WikiPageKey for an Entity.
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
	 * @throws IOException 
	 * @throws UnauthorizedException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_KEY, method = RequestMethod.GET)
	public @ResponseBody
	WikiPageKey getEntityRootWikiKey(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException, UnauthorizedException, IOException {
		return serviceProvider.getWikiService().getRootWikiKey(userId,
				ownerId, ObjectType.ENTITY);
	}
	
	/**
	 * Get the root WikiPageKey for an Evaluation.
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
	 * @throws IOException 
	 * @throws UnauthorizedException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_KEY, method = RequestMethod.GET)
	public @ResponseBody
	WikiPageKey getEvaluationRootWikiKey(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException, UnauthorizedException, IOException {
		return serviceProvider.getWikiService().getRootWikiKey(userId,
				ownerId, ObjectType.EVALUATION);
	}
	
	/**
	 * Get the root WikiPageKey for an Access Requirement.
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the owner.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Access Requirement.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 * @throws UnauthorizedException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_KEY, method = RequestMethod.GET)
	public @ResponseBody
	WikiPageKey getAccessRequirmentRootWikiKey(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException, UnauthorizedException, IOException {
		return serviceProvider.getWikiService().getRootWikiKey(userId,
				ownerId, ObjectType.ACCESS_REQUIREMENT);
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
	 * @param wikiVersion When included returns a specific version of a wiki.               
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID, method = RequestMethod.GET)
	public @ResponseBody
	WikiPage getEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = false) Long wikiVersion)
			throws DatastoreException, NotFoundException, IOException {
		return serviceProvider.getWikiService().getWikiPage(userId,
				WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ENTITY, wikiId), wikiVersion);
	}

	/**
	 * Get a specific WikiPage of of an Access Requirement.
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
	 * @param wikiVersion When included returns a specific version of a wiki.               
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_ID, method = RequestMethod.GET)
	public @ResponseBody
	WikiPage getAccessRequirementWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = false) Long wikiVersion)
			throws DatastoreException, NotFoundException, IOException {
		return serviceProvider.getWikiService().getWikiPage(userId,
				WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ACCESS_REQUIREMENT, wikiId), wikiVersion);
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
	 * @throws IOException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID, method = RequestMethod.GET)
	public @ResponseBody
	WikiPage getCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = false) Long wikiVersion)
			throws DatastoreException, NotFoundException, IOException {
		return serviceProvider.getWikiService().getWikiPage(userId,
				WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.EVALUATION, wikiId), wikiVersion);
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
	 * @throws IOException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_ID, method = RequestMethod.PUT)
	public @ResponseBody
	WikiPage updateAccessRequirementPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestBody WikiPage toUpdate) throws DatastoreException,
			NotFoundException, IOException {
		validateUpateArguments(wikiId, toUpdate);
		return serviceProvider.getWikiService().updateWikiPage(userId, ownerId,
				ObjectType.ACCESS_REQUIREMENT, toUpdate);
	}
	
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
	 * @throws IOException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID, method = RequestMethod.PUT)
	public @ResponseBody
	WikiPage updateEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestBody WikiPage toUpdate) throws DatastoreException,
			NotFoundException, IOException {
		validateUpateArguments(wikiId, toUpdate);
		return serviceProvider.getWikiService().updateWikiPage(userId, ownerId,
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
	 * @throws IOException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID, method = RequestMethod.PUT)
	public @ResponseBody
	WikiPage updateCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestBody WikiPage toUpdate) throws DatastoreException,
			NotFoundException, IOException {
		validateUpateArguments(wikiId, toUpdate);
		return serviceProvider.getWikiService().updateWikiPage(userId, ownerId,
				ObjectType.EVALUATION, toUpdate);
	}

	/**
	 * Helper to validate update arguments.
	 * 
	 * @param wikiId
	 * @param wikiPage
	 */
	private void validateUpateArguments(String wikiId, WikiPage wikiPage) {
		if (wikiPage == null)
			throw new IllegalArgumentException("WikiPage cannot be null");
		if (!wikiId.equals(wikiPage.getId())) {
			throw new IllegalArgumentException(
					"Path variable wikiId does not match the ID of the passed WikiPage");
		}
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
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId)
			throws DatastoreException, NotFoundException {
		serviceProvider.getWikiService().deleteWikiPage(userId,
				WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ENTITY, wikiId));
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
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId)
			throws DatastoreException, NotFoundException {
		serviceProvider.getWikiService().deleteWikiPage(userId,
				WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.EVALUATION, wikiId));
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
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_TREE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<WikiHeader> getEntityWikiHeaderTree(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Long limit,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getWikiService().getWikiHeaderTree(userId,
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
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_TREE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<WikiHeader> getCompetitionWikiHeaderTree(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Long limit,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getWikiService().getWikiHeaderTree(userId,
				ownerId, ObjectType.EVALUATION, limit, offset);
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
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_ATTCHMENT_HANDLE, method = RequestMethod.GET)
	public @ResponseBody
	FileHandleResults getEntityWikiAttachmenthHandles(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId)
			throws DatastoreException, NotFoundException {
		// Get the redirect url
		return serviceProvider.getWikiService().getAttachmentFileHandles(
				userId, WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ENTITY, wikiId));
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
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_ATTCHMENT_HANDLE, method = RequestMethod.GET)
	public @ResponseBody
	FileHandleResults getCompetitionWikiAttachmenthHandles(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId)
			throws DatastoreException, NotFoundException {
		// Get the redirect url
		return serviceProvider
				.getWikiService()
				.getAttachmentFileHandles(userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.EVALUATION, wikiId));
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
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_ATTCHMENT_FILE, method = RequestMethod.GET)
	public @ResponseBody
	void getEntityWikiAttachmentFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = true)  String fileName,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getWikiService()
				.getAttachmentRedirectURL(userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ENTITY, wikiId),
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
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_ATTCHMENT_FILE, method = RequestMethod.GET)
	public @ResponseBody
	void getCompetitionAttachmentFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = true) String fileName,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider
				.getWikiService()
				.getAttachmentRedirectURL(
						userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.EVALUATION, wikiId),
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
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_ATTCHMENT_FILE_PREVIEW, method = RequestMethod.GET)
	public @ResponseBody
	void getEntityWikiAttachmenPreviewFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = true) String fileName,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getWikiService()
				.getAttachmentPreviewRedirectURL(userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ENTITY, wikiId),
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
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_ATTCHMENT_FILE_PREVIEW, method = RequestMethod.GET)
	public @ResponseBody
	void getCompetitionAttachmenthPreviewFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = true) String fileName,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider
				.getWikiService()
				.getAttachmentPreviewRedirectURL(
						userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.EVALUATION, wikiId),
						fileName);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

}
