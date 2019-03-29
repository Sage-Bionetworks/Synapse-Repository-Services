package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
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
 * href="${org.sagebionetworks.repo.model.v2.wiki.V2WikiPage}">V2WikiPage</a> model
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
 * WikiPages are composed of two major parts; a file that contains the markdown text 
 * and a list of file attachments. For example, to embed an image from an end-user's
 * machine into a WikiPage, the image file must first be uploaded to Synapse as <a
 * href="${org.sagebionetworks.repo.model.file.FileHandle}">FileHandle</a> (see
 * <a href="${org.sagebionetworks.file.controller.UploadController}">File
 * Services</a>). The FileHandle ID can then be added to a
 * WikiPage.attachmentFileHandleIds list. See <a
 * href="https://www.synapse.org">www.synapse.org</a> for details on the
 * supported markdown syntax. The markdown text is similarly uploaded as a
 * FileHandle and its FileHandle ID is tracked by the WikiPage. 
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
 * href="${GET.entity.ownerId.wikiheadertree2}">GET
 * /entity/{ownerId}/wikiheadertree2</a> method. The returned list of <a
 * href="${org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader}">V2WikiHeaders</a> can
 * be used to construct a full wiki hierarchy tree for that owner.
 * </p>
 * <p>
 * To view a timeline of changes made to a WikiPage, use the 
 * <a href="${GET.entity.ownerId.wiki2.wikiId.wikihistory}">GET/entity/{ownerId}
 * /wiki2/{wikiId}/wikihistory</a> method. The returned list of <a 
 * href="${org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot}">V2WikiHistorySnapshots
 * </a> contain information about who modified the WikiPage and when the changes were made. 
 * </p>
 * <p>
 * Note: WikiPages can be nested to created a hierarchy of sub-pages. However,
 * there can only be one root WikiPage per owner object, and all sub-pages are
 * considered to be owned by the same object as the root page.
 * </p>
 */
@ControllerInfo(displayName = "WikiPage Services 2", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class V2WikiController {
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
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_V2, method = RequestMethod.POST)
	public @ResponseBody
	V2WikiPage createEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @RequestBody V2WikiPage toCreate)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getV2WikiService().createWikiPage(userId, ownerId,
				ObjectType.ENTITY, toCreate);
	}

	/**
	 * Create a WikiPage with an AccessRequirement as an owner.
	 * 
	 * <p>
	 * Note: The caller must be a member of the Synapse Access and Compliance Team.
	 * </p>
	 * <p>
	 * If the passed WikiPage is a root (parentWikiId = null) and the owner
	 * already has a root WikiPage, an error will be returned.
	 * </p>
	 * 
	 * @param userId
	 *            The user's id.
	 * @param ownerId
	 *            The ID of the owner AccessRequirement.
	 * @param toCreate
	 *            The WikiPage to create.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_V2, method = RequestMethod.POST)
	public @ResponseBody
	V2WikiPage createAccessRequirementWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @RequestBody V2WikiPage toCreate)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getV2WikiService().createWikiPage(userId, ownerId,
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
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_V2, method = RequestMethod.POST)
	public @ResponseBody
	V2WikiPage createCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getRootWikiPage(userId,
				ownerId, ObjectType.ENTITY);
	}

	/**
	 * Get the root WikiPage for an AccessRequirement.
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning AccessRequirement.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_V2, method = RequestMethod.GET)
	public @ResponseBody
	V2WikiPage getAccessRequirementRootWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getRootWikiPage(userId,
				ownerId, ObjectType.ACCESS_REQUIREMENT);
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getRootWikiPage(userId,
				ownerId, ObjectType.EVALUATION);
	}

	/**
	 * Get a specific WikiPage of an Entity.
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
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_V2, method = RequestMethod.GET)
	public @ResponseBody
	V2WikiPage getEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = false) Long wikiVersion)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getV2WikiService().getWikiPage(userId,
				WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ENTITY, wikiId), wikiVersion);
	}

	/**
	 * Get a specific WikiPage of an Access Requirement.
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Access Requirement.
	 * @param wikiId
	 *            The ID of the WikiPage to get.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_ID_V2, method = RequestMethod.GET)
	public @ResponseBody
	V2WikiPage getAccessRequirementWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = false) Long wikiVersion)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getV2WikiService().getWikiPage(userId,
				WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ACCESS_REQUIREMENT, wikiId), wikiVersion);
	}

	/**
	 * Get a specific WikiPage of an Evaluation.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = false) Long wikiVersion)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getV2WikiService().getWikiPage(userId,
				WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.EVALUATION, wikiId), wikiVersion);
	}
	
	// Update methods.
	/**
	 * Update a specific WikiPage of an Entity. This adds a new entry 
	 * to the history of changes made to this specific WikiPage. 
	 * <p>
	 * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle
	 * concurrent updates. Each time a WikiPage is updated a new etag will be
	 * issued to the WikiPage. When an update is requested, Synapse will compare
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestBody V2WikiPage toUpdate) throws DatastoreException,
			NotFoundException {
		validateUpateArguments(wikiId, toUpdate);
		return serviceProvider.getV2WikiService().updateWikiPage(userId, ownerId,
				ObjectType.ENTITY, toUpdate);
	}
	
	/**
	 * Update an order hint that corresponds to the given owner Entity.
	 *  <p>
	 * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle
	 * concurrent updates. Each time an WikiOrderHint is updated a new etag will be
	 * issued to the WikiOrderHint. When an update is requested, Synapse will compare
	 * the etag of the passed WikiOrderHint with the current etag of the WikiOrderHint. If
	 * the etags do not match, then the update will be rejected with a
	 * PRECONDITION_FAILED (412) response. When this occurs the caller should
	 * get the latest copy of the WikiOrderHint and re-apply any changes to the
	 * object, then re-attempt the update. This ensures the caller has all
	 * changes applied by other users before applying their own changes.
	 * </p>
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> permission on the owner.
	 * </p>
	 * 
	 * 
	 * @param userId
	 * @param ownerId
	 * @param wikiId
	 * @param toUpdate
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_V2_ORDER_HINT, method = RequestMethod.PUT)
	public @ResponseBody
	V2WikiOrderHint updateEntityWikiOrderHint(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @RequestBody V2WikiOrderHint toUpdate)
			throws DatastoreException, NotFoundException {
		if (toUpdate == null) throw new IllegalArgumentException("OrderHint cannot be null.");
		return serviceProvider.getV2WikiService().updateWikiOrderHint(userId, toUpdate);
	}
	
	

	/**
	 * Update a specific WikiPage of an Access Requirement. This adds a new entry 
	 * to the history of changes made to this specific WikiPage. 
	 * <p>
	 * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle
	 * concurrent updates. Each time a WikiPage is updated a new etag will be
	 * issued to the WikiPage. When an update is requested, Synapse will compare
	 * the etag of the passed WikiPage with the current etag of the WikiPage. If
	 * the etags do not match, then the update will be rejected with a
	 * PRECONDITION_FAILED (412) response. When this occurs the caller should
	 * get the latest copy of the WikiPage and re-apply any changes to the
	 * object, then re-attempt the update. This ensures the caller has all
	 * changes applied by other users before applying their own changes.
	 * </p>
	 * <p>
	 * Note: The caller must be a member of the Synapse Access and Compliance Team.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Access Requirement.
	 * @param wikiId
	 *            The ID of the WikiPage to update.
	 * @param toUpdate
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_ID_V2, method = RequestMethod.PUT)
	public @ResponseBody
	V2WikiPage updateAcessRequirementWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestBody V2WikiPage toUpdate) throws DatastoreException,
			NotFoundException {
		validateUpateArguments(wikiId, toUpdate);
		return serviceProvider.getV2WikiService().updateWikiPage(userId, ownerId,
				ObjectType.ACCESS_REQUIREMENT, toUpdate);
	}

	/**
	 * Update a specific WikiPage of an Evaluation. This adds a new entry 
	 * to the history of changes made to this specific WikiPage. 
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
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
	 * For a specific WikiPage of an Entity, restore a version of the WikiPage's content.
	 * <p>
	 * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle
	 * concurrent updates. Each time a WikiPage is updated a new etag will be
	 * issued to the WikiPage. When an update is requested, Synapse will compare
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
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_AND_VERSION_V2, method = RequestMethod.PUT)
	public @ResponseBody
	V2WikiPage restoreEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long wikiVersion,
			@PathVariable String ownerId, @PathVariable String wikiId) 
			throws DatastoreException, NotFoundException {
		return serviceProvider.getV2WikiService().restoreWikipage(userId, ownerId, ObjectType.ENTITY, wikiId, wikiVersion);
	}

	/**
	 * For a specific WikiPage of an Access Requirement, restore a version of the WikiPage's content.
	 * <p>
	 * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle
	 * concurrent updates. Each time a WikiPage is updated a new etag will be
	 * issued to the WikiPage. When an update is requested, Synapse will compare
	 * the etag of the passed WikiPage with the current etag of the WikiPage. If
	 * the etags do not match, then the update will be rejected with a
	 * PRECONDITION_FAILED (412) response. When this occurs the caller should
	 * get the latest copy of the WikiPage and re-apply any changes to the
	 * object, then re-attempt the update. This ensures the caller has all
	 * changes applied by other users before applying their own changes.
	 * </p>
	 * <p>
	 * Note: The caller must be a member of the Synapse Access and Compliance Team.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Access Requirement.
	 * @param wikiId
	 *            The ID of the WikiPage to update.
	 * @param toUpdate
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_ID_AND_VERSION_V2, method = RequestMethod.PUT)
	public @ResponseBody
	V2WikiPage restoreAccessRequirementWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long wikiVersion,
			@PathVariable String ownerId, @PathVariable String wikiId) 
			throws DatastoreException, NotFoundException {
		return serviceProvider.getV2WikiService().restoreWikipage(userId, ownerId, ObjectType.ACCESS_REQUIREMENT, wikiId, wikiVersion);
	}

	/**
	 * For a specific WikiPage of an Evaluation, restore a version of the WikiPage's content.
	 * <p>
	 * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle
	 * concurrent updates. Each time a WikiPage is updated a new etag will be
	 * issued to the WikiPage. When an update is requested, Synapse will compare
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
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_AND_VERSION_V2, method = RequestMethod.PUT)
	public @ResponseBody
	V2WikiPage restoreCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long wikiVersion,
			@PathVariable String ownerId, @PathVariable String wikiId) 
			throws DatastoreException, NotFoundException {
		return serviceProvider.getV2WikiService().restoreWikipage(userId, ownerId, ObjectType.EVALUATION, wikiId, wikiVersion);
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId)
			throws DatastoreException, NotFoundException {
		serviceProvider.getV2WikiService().deleteWikiPage(userId,
				WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ENTITY, wikiId));
	}

	/**
	 * Delete a specific WikiPage of an Access Requirement.
	 * <p>
	 * Note: When a WikiPage is deleted, the delete will cascade to all children
	 * WikiPages (recursively) of the deleted WikiPage.
	 * </p>
	 * <p>
	 * Note: The caller must be a member of the Synapse Access and Compliance Team.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Access Requirement.
	 * @param wikiId
	 *            The ID of the WikiPage to delete.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_ID_V2, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteAccessRequirementWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId)
			throws DatastoreException, NotFoundException {
		serviceProvider.getV2WikiService().deleteWikiPage(userId,
				WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ACCESS_REQUIREMENT, wikiId));
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId)
			throws DatastoreException, NotFoundException {
		serviceProvider.getV2WikiService().deleteWikiPage(userId,
				WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.EVALUATION, wikiId));
	}

	// Get Wiki Hierarchy
	/**
	 * Get a paginated list of all <a
	 * href="${org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader}">V2WikiHeaders</a>
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
	 *            of 10 require limit = 10. Limit must be 50 or less.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Long limit,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getWikiHeaderTree(userId,
				ownerId, ObjectType.ENTITY, limit, offset);
	}
	
	/**
	 * 
	 * Get an order hint
	 * that corresponds to the given owner Entity. The resulting hint can be used to
	 * establish a relative ordering for the subwikis in
	 * a tree of the WikiPages for this owner.
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
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_V2_ORDER_HINT, method = RequestMethod.GET)
	public @ResponseBody
	V2WikiOrderHint getEntityWikiOrderHint(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getWikiOrderHint(userId, ownerId, ObjectType.ENTITY);
	}

	/**
	 * Get a paginated list of all <a
	 * href="${org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader}">V2WikiHeaders</a>
	 * that belong to the given owner Access Requirement. The resulting list can be used to
	 * build a tree of the WikiPages for this owner. The first WikiHeader will
	 * be for the root WikiPage (parentWikiId = null).
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
	 *            The ID of the owning Access Requirement.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_TREE_V2, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<V2WikiHeader> getAccessRequirementWikiHeaderTree(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Long limit,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getWikiHeaderTree(userId,
				ownerId, ObjectType.ACCESS_REQUIREMENT, limit, offset);
	}

	/**
	 * Get a paginated list of all <a
	 * href="${org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader}">V2WikiHeaders</a>
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Long limit,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getWikiHeaderTree(userId,
				ownerId, ObjectType.EVALUATION, limit, offset);
	}
	
	// Wiki History
	/**
	 * Get a paginated list of all <a
	 * href="${org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot}">V2WikiHistorySnapshot</a>
	 * that belong to a specific WikiPage, which belong to the given owner Entity. The resulting list 
	 * can be used to display a timeline of changes to the specific WikiPage for this owner. 
	 * The first V2WikiHistorySnapshot describes the most recent change or update to the WikiPage.
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
	 *            of 10 require limit = 10. Limit must be 50 or less.
	 * @param ownerId
	 *            The ID of the owning Entity.
	 * @param wikiId
	 * 			  The ID of the WikiPage.
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_HISTORY_V2, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<V2WikiHistorySnapshot> getEntityWikiHistory(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(ServiceConstants.PAGINATION_OFFSET_PARAM) Long offset,
			@RequestParam(ServiceConstants.PAGINATION_LIMIT_PARAM) Long limit,
			@PathVariable String wikiId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getWikiHistory(userId, ownerId, ObjectType.ENTITY, 
				limit, offset, WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ENTITY, wikiId));

	}

	/**
	 * Get a paginated list of all <a
	 * href="${org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot}">V2WikiHistorySnapshot</a>
	 * that belong to a specific WikiPage, which belong to the given owner Access Requirement. The resulting list 
	 * can be used to display a timeline of changes to the specific WikiPage for this owner. 
	 * The first V2WikiHistorySnapshot describes the most recent change or update to the WikiPage.
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
	 *            The ID of the owning Access Requirement.
	 * @param wikiId
	 * 			  The ID of the WikiPage.
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_HISTORY_V2, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<V2WikiHistorySnapshot> getAccessRequirementWikiHistory(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(ServiceConstants.PAGINATION_OFFSET_PARAM) Long offset,
			@RequestParam(ServiceConstants.PAGINATION_LIMIT_PARAM) Long limit,
			@PathVariable String wikiId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getWikiHistory(userId, ownerId, ObjectType.ACCESS_REQUIREMENT, 
				limit, offset, WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ACCESS_REQUIREMENT, wikiId));

	}

	/**
	 * Get a paginated list of all <a
	 * href="${org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot}">V2WikiHistorySnapshot</a>
	 * that belong to a specific WikiPage, which belong to the given owner Evaluation. The resulting list 
	 * can be used to display a timeline of changes to the specific WikiPage for this owner. 
	 * The first V2WikiHistorySnapshot describes the most recent change or update to the WikiPage.
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
	 * @param wikiId
	 * 			  The ID of the WikiPage.
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_HISTORY_V2, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<V2WikiHistorySnapshot> getCompetitionWikiHistory(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(ServiceConstants.PAGINATION_OFFSET_PARAM) Long offset,
			@RequestParam(ServiceConstants.PAGINATION_LIMIT_PARAM) Long limit,
			@PathVariable String wikiId,
			@PathVariable String ownerId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getV2WikiService().getWikiHistory(userId, ownerId, ObjectType.EVALUATION, 
				limit, offset, WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.EVALUATION, wikiId));

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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = false) Long wikiVersion)
			throws DatastoreException, NotFoundException {
		// Get the redirect url
		return serviceProvider.getV2WikiService().getAttachmentFileHandles(
				userId, WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ENTITY, wikiId), wikiVersion);
	}

	/**
	 * Get the list of FileHandles for all file attachments of a specific
	 * WikiPage for a given owning Access Requirement. This list will include Previews if
	 * they exist and will provide information about file sizes, content types
	 * and names.
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Access Requirement.
	 * @param wikiId
	 *            The ID of the WikiPage.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_ID_ATTCHMENT_HANDLE_V2, method = RequestMethod.GET)
	public @ResponseBody
	FileHandleResults getAccessRequirementWikiAttachmenthHandles(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = false) Long wikiVersion)
			throws DatastoreException, NotFoundException {
		// Get the redirect url
		return serviceProvider.getV2WikiService().getAttachmentFileHandles(
				userId, WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ACCESS_REQUIREMENT, wikiId), wikiVersion);
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = false) Long wikiVersion)
			throws DatastoreException, NotFoundException {
		// Get the redirect url
		return serviceProvider
				.getV2WikiService()
				.getAttachmentFileHandles(userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.EVALUATION, wikiId), wikiVersion);
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = true)  String fileName,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response,
			@RequestParam(required = false) Long wikiVersion) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getV2WikiService()
				.getAttachmentRedirectURL(userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ENTITY, wikiId),
						fileName, wikiVersion);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get a URL that can be used to download a file for a given WikiPage file
	 * attachment.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Access Requirement
	 * @param wikiId
	 *            The ID of the WikiPage
	 * @param fileName
	 *            The name of the file to get. The file names can be found in
	 *            the FileHandles from the <a
	 *            href="${GET.entity.ownerId.wiki.wikiId.attachmenthandles}">GET
	 *            /accessRequirement/{ownerId}/wiki/{wikiId}/attachmenthandles</a> method.
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_ID_ATTCHMENT_FILE_V2, method = RequestMethod.GET)
	public @ResponseBody
	void getAccessRequirementWikiAttachmentFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = true)  String fileName,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response,
			@RequestParam(required = false) Long wikiVersion) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getV2WikiService()
				.getAttachmentRedirectURL(userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ACCESS_REQUIREMENT, wikiId),
						fileName, wikiVersion);
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = true) String fileName,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response,
			@RequestParam(required = false) Long wikiVersion) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider
				.getV2WikiService()
				.getAttachmentRedirectURL(
						userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.EVALUATION, wikiId),
						fileName, wikiVersion);
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = true) String fileName,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response,
			@RequestParam(required = false) Long wikiVersion) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getV2WikiService()
				.getAttachmentPreviewRedirectURL(userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ENTITY, wikiId),
						fileName, wikiVersion);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get a URL that can be used to download a preview file for a given
	 * WikiPage file attachment.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 *            The ID of the owning Access Requirement
	 * @param wikiId
	 *            The ID of the WikiPage
	 * @param fileName
	 *            The name of the file to get. The file names can be found in
	 *            the FileHandles from the <a
	 *            href="${GET.entity.ownerId.wiki.wikiId.attachmenthandles}">GET
	 *            /accessRequirement/{ownerId}/wiki/{wikiId}/attachmenthandles</a> method.
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_ID_ATTCHMENT_FILE_PREVIEW_V2, method = RequestMethod.GET)
	public @ResponseBody
	void getAccessRequirementWikiAttachmenPreviewFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = true) String fileName,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response,
			@RequestParam(required = false) Long wikiVersion) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getV2WikiService()
				.getAttachmentPreviewRedirectURL(userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ACCESS_REQUIREMENT, wikiId),
						fileName, wikiVersion);
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = true) String fileName,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response,
			@RequestParam(required = false) Long wikiVersion) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider
				.getV2WikiService()
				.getAttachmentPreviewRedirectURL(
						userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.EVALUATION, wikiId),
						fileName, wikiVersion);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	
	/**
	 * Get a URL that can be used to download the markdown file for a given
	 * WikiPage.
	 * 
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
	 * @param wikiId
	 * @param redirect
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_MARKDOWN_FILE_V2, method = RequestMethod.GET)
	public @ResponseBody
	void getEntityWikiMarkdownFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = false) Boolean redirect,
			@RequestParam(required = false) Long wikiVersion,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getV2WikiService()
				.getMarkdownRedirectURL(userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ENTITY, wikiId), wikiVersion);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	
	/**
	 * Get a URL that can be used to download the markdown file for a given
	 * WikiPage.
	 * 
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param ownerId
	 * @param wikiId
	 * @param redirect
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WIKI_ID_MARKDOWN_FILE_V2, method = RequestMethod.GET)
	public @ResponseBody
	void getAccessRequirementWikiMarkdownFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = false) Boolean redirect,
			@RequestParam(required = false) Long wikiVersion,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getV2WikiService()
				.getMarkdownRedirectURL(userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.ACCESS_REQUIREMENT, wikiId), wikiVersion);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	
	/**
	 * Get a URL that can be used to download the markdown file for a given
	 * WikiPage.
	 * 
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
	 * @param wikiId
	 * @param redirect
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_MARKDOWN_FILE_V2, method = RequestMethod.GET)
	public @ResponseBody
	void getEvaluationWikiMarkdownFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String ownerId, @PathVariable String wikiId,
			@RequestParam(required = false) Boolean redirect,
			@RequestParam(required = false) Long wikiVersion,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getV2WikiService()
				.getMarkdownRedirectURL(userId,
						WikiPageKeyHelper.createWikiPageKey(ownerId, ObjectType.EVALUATION, wikiId), wikiVersion);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

}
