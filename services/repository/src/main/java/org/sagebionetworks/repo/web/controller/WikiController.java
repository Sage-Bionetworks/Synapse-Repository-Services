package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.net.URL;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.message.ObjectType;
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
 * Controller for WikiPage services.
 * @author John
 *
 */
@ControllerInfo(displayName="Wiki Page Services", path="repo/v1")
@Controller
public class WikiController extends BaseController {
	
	@Autowired
	ServiceProvider serviceProvider;
	
	/**
	 * Create a wiki page with an entity owner.
	 * 
	 * @param userId - the user's id.
	 * @param ownerId - the ID of thw owner object.
	 * @param toCreate - the WikiPage to create.s
	 * @return - 
	 * @throws DatastoreException - Synapse error.
	 * @throws NotFoundException - returned if the user or owner does not exist.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI, method = RequestMethod.POST)
	public @ResponseBody
	WikiPage createEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@RequestBody WikiPage toCreate
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().createWikiPage(userId, ownerId, ObjectType.ENTITY, toCreate);
	}
	
	/**
	 * Create a wiki page with a evaluation owner.
	 * 
	 * @param userId
	 * @param ownerId
	 * @param toCreate
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI, method = RequestMethod.POST)
	public @ResponseBody
	WikiPage createCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@RequestBody WikiPage toCreate
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().createWikiPage(userId, ownerId, ObjectType.EVALUATION, toCreate);
	}
	
	/**
	 * Get the root wiki page for an owner.
	 * @param userId
	 * @param ownerId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI, method = RequestMethod.GET)
	public @ResponseBody
	WikiPage getEntityRootWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().getRootWikiPage(userId, ownerId, ObjectType.ENTITY);
	}
	
	/**
	 * Get the root wiki page owned by an evaluation.
	 * @param userId
	 * @param ownerId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI, method = RequestMethod.GET)
	public @ResponseBody
	WikiPage getCompetitionRootWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().getRootWikiPage(userId, ownerId, ObjectType.EVALUATION);
	}
	
	/**
	 * Get a wiki page owned by an entity.
	 * @param userId
	 * @param ownerId
	 * @param wikiId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID, method = RequestMethod.GET)
	public @ResponseBody
	WikiPage getEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().getWikiPage(userId, new WikiPageKey(ownerId, ObjectType.ENTITY, wikiId));
	}
	
	/**
	 * Get a WikiPage owed by 
	 * @param userId
	 * @param ownerId - the ID of the owner object
	 * @param wikiId - the ID of the wiki.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID, method = RequestMethod.GET)
	public @ResponseBody
	WikiPage getCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().getWikiPage(userId, new WikiPageKey(ownerId, ObjectType.EVALUATION, wikiId));
	}
	
	// Update methods.
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID, method = RequestMethod.PUT)
	public @ResponseBody
	WikiPage updateEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId,
			@RequestBody WikiPage toUpdate
			) throws DatastoreException, NotFoundException{
		validateUpateArguments(wikiId, toUpdate);
		return serviceProvider.getWikiService().updateWikiPage(userId, ownerId, ObjectType.ENTITY, toUpdate);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID, method = RequestMethod.PUT)
	public @ResponseBody
	WikiPage updateCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId,
			@RequestBody WikiPage toUpdate
			) throws DatastoreException, NotFoundException{
		validateUpateArguments(wikiId, toUpdate);
		return serviceProvider.getWikiService().updateWikiPage(userId, ownerId, ObjectType.EVALUATION, toUpdate);
	}

	/**
	 * Helper to validate update arguments.
	 * @param wikiId
	 * @param wikiPage
	 */
	private void validateUpateArguments(String wikiId, WikiPage wikiPage) {
		if(wikiPage == null) throw new IllegalArgumentException("WikiPage cannot be null");
		if(!wikiId.equals(wikiPage.getId())){
			throw new IllegalArgumentException("Path variable wikiId does not match the ID of the passed WikiPage");
		}
	}
	
	// Delete methods
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId
			) throws DatastoreException, NotFoundException{
		serviceProvider.getWikiService().deleteWikiPage(userId, new WikiPageKey(ownerId, ObjectType.ENTITY, wikiId));
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId
			) throws DatastoreException, NotFoundException{
		serviceProvider.getWikiService().deleteWikiPage(userId, new WikiPageKey(ownerId, ObjectType.EVALUATION, wikiId));
	}
	
	// Get Wiki Hierarchy
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_TREE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<WikiHeader> getEntityWikiHeaderTree(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Long limit,
			@PathVariable String ownerId
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().getWikiHeaderTree(userId, ownerId, ObjectType.ENTITY, limit, offset);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_TREE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<WikiHeader> getCompetitionWikiHeaderTree(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Long limit,
			@PathVariable String ownerId
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().getWikiHeaderTree(userId, ownerId, ObjectType.EVALUATION, limit, offset);
	}
	// Handles
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_ATTCHMENT_HANDLE, method = RequestMethod.GET)
	public @ResponseBody
	FileHandleResults getEntityWikiAttachmenthHandles(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId
			) throws DatastoreException, NotFoundException{
		// Get the redirect url
		return serviceProvider.getWikiService().getAttachmentFileHandles(userId, new WikiPageKey(ownerId, ObjectType.ENTITY, wikiId));
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_ATTCHMENT_HANDLE, method = RequestMethod.GET)
	public @ResponseBody
	FileHandleResults getCompetitionWikiAttachmenthHandles(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId
			) throws DatastoreException, NotFoundException{
		// Get the redirect url
		return serviceProvider.getWikiService().getAttachmentFileHandles(userId, new WikiPageKey(ownerId, ObjectType.EVALUATION, wikiId));
	}
	
	// Files
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_ATTCHMENT_FILE, method = RequestMethod.GET)
	public @ResponseBody
	void getEntityWikiAttachmentFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId,
			@RequestParam String fileName,
			@RequestParam (required = false) Boolean redirect,
			HttpServletResponse response
			) throws DatastoreException, NotFoundException, IOException{
		// Get the redirect url
		URL redirectUrl = serviceProvider.getWikiService().getAttachmentRedirectURL(userId,  new WikiPageKey(ownerId, ObjectType.ENTITY, wikiId), fileName);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_ATTCHMENT_FILE, method = RequestMethod.GET)
	public @ResponseBody
	void getCompetitionAttachmentFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId,
			@RequestParam String fileName,
			@RequestParam (required = false) Boolean redirect,
			HttpServletResponse response
			) throws DatastoreException, NotFoundException, IOException{
		// Get the redirect url
		URL redirectUrl = serviceProvider.getWikiService().getAttachmentRedirectURL(userId,  new WikiPageKey(ownerId, ObjectType.EVALUATION, wikiId), fileName);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	
	// Files
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_ATTCHMENT_FILE_PREVIEW, method = RequestMethod.GET)
	public @ResponseBody
	void getEntityWikiAttachmenPreviewFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId,
			@RequestParam String fileName,
			@RequestParam (required = false) Boolean redirect,
			HttpServletResponse response
			) throws DatastoreException, NotFoundException, IOException{
		// Get the redirect url
		URL redirectUrl = serviceProvider.getWikiService().getAttachmentPreviewRedirectURL(userId,  new WikiPageKey(ownerId, ObjectType.ENTITY, wikiId), fileName);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	
	@RequestMapping(value = UrlHelpers.EVALUATION_WIKI_ID_ATTCHMENT_FILE_PREVIEW, method = RequestMethod.GET)
	public @ResponseBody
	void getCompetitionAttachmenthPreviewFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId,
			@RequestParam String fileName,
			@RequestParam (required = false) Boolean redirect,
			HttpServletResponse response
			) throws DatastoreException, NotFoundException, IOException{
		// Get the redirect url
		URL redirectUrl = serviceProvider.getWikiService().getAttachmentPreviewRedirectURL(userId,  new WikiPageKey(ownerId, ObjectType.EVALUATION, wikiId), fileName);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	

}
