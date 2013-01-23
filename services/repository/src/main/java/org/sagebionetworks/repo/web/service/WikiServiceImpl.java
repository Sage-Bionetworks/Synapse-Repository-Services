package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.wiki.WikiManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class WikiServiceImpl implements WikiService {
	
	@Autowired
	UserManager userManager;
	@Autowired
	WikiManager wikiManager;

	@Override
	public WikiPage createWikiPage(String userId, String objectId,	ObjectType objectType, WikiPage toCreate) throws DatastoreException, NotFoundException {
		// Resolve the userID
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.createWikiPage(user, objectId, objectType, toCreate);
	}

	@Override
	public WikiPage getWikiPage(String userId, WikiPageKey key) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getWikiPage(user, key);
	}

	@Override
	public WikiPage updateWikiPage(String userId, String objectId,	ObjectType objectType, WikiPage toUpdate) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.updateWikiPage(user, objectId, objectType, toUpdate);
	}

	@Override
	public void deleteWikiPage(String userId, WikiPageKey wikiPageKey) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		wikiManager.deleteWiki(user, wikiPageKey);
	}

}
