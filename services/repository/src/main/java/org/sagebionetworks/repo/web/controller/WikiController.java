package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.web.UrlHelpers.*;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.message.ObjectType;
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
			@PathVariable(value= ID_PATH_VARIABLE) String entityId,
			@RequestBody WikiPage toCreate
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().createWikiPage(userId, entityId, ObjectType.ENTITY, toCreate);
	}
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.COMPETITION_WIKI, method = RequestMethod.POST)
	public @ResponseBody
	WikiPage createCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable(value= COMPETITION_ID_PATH_VAR) String competitionId,
			@RequestBody WikiPage toCreate
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().createWikiPage(userId, competitionId, ObjectType.COMPETITION, toCreate);
	}
	
	// Get methods
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID, method = RequestMethod.GET)
	public @ResponseBody
	WikiPage getEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable(value= ID_PATH_VARIABLE) String entityId,
			@PathVariable(value= WIKI_ID_PATH_VAR) String wikiId
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().getWikiPage(userId, new WikiPageKey(entityId, ObjectType.ENTITY, wikiId));
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.COMPETITION_WIKI_ID, method = RequestMethod.GET)
	public @ResponseBody
	WikiPage getCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable(value= COMPETITION_ID_PATH_VAR) String compId,
			@PathVariable(value= WIKI_ID_PATH_VAR) String wikiId
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().getWikiPage(userId, new WikiPageKey(compId, ObjectType.COMPETITION, wikiId));
	}
	
	// Update methods.
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID, method = RequestMethod.PUT)
	public @ResponseBody
	WikiPage updateEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable(value= ID_PATH_VARIABLE) String entityId,
			@PathVariable(value= WIKI_ID_PATH_VAR) String wikiId,
			@RequestBody WikiPage toUpdate
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().updateWikiPage(userId, entityId, ObjectType.ENTITY, toUpdate);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.COMPETITION_WIKI_ID, method = RequestMethod.PUT)
	public @ResponseBody
	WikiPage updateCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable(value= COMPETITION_ID_PATH_VAR) String compId,
			@PathVariable(value= WIKI_ID_PATH_VAR) String wikiId,
			@RequestBody WikiPage toUpdate
			) throws DatastoreException, NotFoundException{
		return serviceProvider.getWikiService().updateWikiPage(userId, compId, ObjectType.ENTITY, toUpdate);
	}
	
	// Delete methods
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_WIKI_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable(value= ID_PATH_VARIABLE) String entityId,
			@PathVariable(value= WIKI_ID_PATH_VAR) String wikiId
			) throws DatastoreException, NotFoundException{
		serviceProvider.getWikiService().deleteWikiPage(userId, new WikiPageKey(entityId, ObjectType.ENTITY, wikiId));
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.COMPETITION_WIKI_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable(value= COMPETITION_ID_PATH_VAR) String compId,
			@PathVariable(value= WIKI_ID_PATH_VAR) String wikiId
			) throws DatastoreException, NotFoundException{
		serviceProvider.getWikiService().deleteWikiPage(userId, new WikiPageKey(compId, ObjectType.ENTITY, wikiId));
	}
}
