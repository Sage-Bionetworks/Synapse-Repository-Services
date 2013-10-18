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
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_AND_VERSION_V2, method = RequestMethod.PUT)
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

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_AND_VERSION_V2, method = RequestMethod.PUT)
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
