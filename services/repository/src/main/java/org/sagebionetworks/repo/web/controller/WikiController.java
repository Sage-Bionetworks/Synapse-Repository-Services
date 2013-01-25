package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.web.UrlHelpers.*;

import java.util.List;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
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

@Controller
public class WikiController extends BaseController {
	
	@Autowired
	ServiceProvider serviceProvider;
	
	// Create methods.
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
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.COMPETITION_WIKI, method = RequestMethod.POST)
	public @ResponseBody
	WikiPage createCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@RequestBody WikiPage toCreate
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().createWikiPage(userId, ownerId, ObjectType.COMPETITION, toCreate);
	}
	
	// Get methods
	
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
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.COMPETITION_WIKI_ID, method = RequestMethod.GET)
	public @ResponseBody
	WikiPage getCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().getWikiPage(userId, new WikiPageKey(ownerId, ObjectType.COMPETITION, wikiId));
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
	@RequestMapping(value = UrlHelpers.COMPETITION_WIKI_ID, method = RequestMethod.PUT)
	public @ResponseBody
	WikiPage updateCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId,
			@RequestBody WikiPage toUpdate
			) throws DatastoreException, NotFoundException{
		validateUpateArguments(wikiId, toUpdate);
		return serviceProvider.getWikiService().updateWikiPage(userId, ownerId, ObjectType.COMPETITION, toUpdate);
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
	@RequestMapping(value = UrlHelpers.COMPETITION_WIKI_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId
			) throws DatastoreException, NotFoundException{
		serviceProvider.getWikiService().deleteWikiPage(userId, new WikiPageKey(ownerId, ObjectType.COMPETITION, wikiId));
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
	@RequestMapping(value = UrlHelpers.COMPETITION_WIKI_TREE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<WikiHeader> getCompetitionWikiHeaderTree(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Long limit,
			@PathVariable String ownerId
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().getWikiHeaderTree(userId, ownerId, ObjectType.COMPETITION, limit, offset);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID_ATTCHMENT, method = RequestMethod.GET)
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
	@RequestMapping(value = UrlHelpers.COMPETITION_WIKI_ID_ATTCHMENT, method = RequestMethod.GET)
	public @ResponseBody
	FileHandleResults getCompetitionWikiAttachmenthHandles(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@PathVariable String wikiId
			) throws DatastoreException, NotFoundException{
		// Get the redirect url
		return serviceProvider.getWikiService().getAttachmentFileHandles(userId, new WikiPageKey(ownerId, ObjectType.COMPETITION, wikiId));
	}
}
