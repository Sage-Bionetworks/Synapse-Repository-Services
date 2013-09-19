package org.sagebionetworks.repo.web.service;

import java.net.URL;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.wiki.WikiManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class WikiServiceImpl implements WikiService {
	
	@Autowired
	UserManager userManager;
	@Autowired
	WikiManager wikiManager;
	@Autowired
	FileHandleManager fileHandleManager;
	
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

	@Override
	public PaginatedResults<WikiHeader> getWikiHeaderTree(String userId, String ownerId, ObjectType type, Long limit, Long offest) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getWikiHeaderTree(user, ownerId, type, limit, offest);
	}

	@Override
	public FileHandleResults getAttachmentFileHandles(String userId, WikiPageKey wikiPageKey) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getAttachmentFileHandles(user, wikiPageKey);
	}

	@Override
	public URL getAttachmentRedirectURL(String userId, WikiPageKey wikiPageKey,	String fileName) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		// First lookup the FileHandle
		String fileHandleId = wikiManager.getFileHandleIdForFileName(user, wikiPageKey, fileName);
		// Use the FileHandle ID to get the URL
		return fileHandleManager.getRedirectURLForFileHandle(fileHandleId);
	}

	@Override
	public URL getAttachmentPreviewRedirectURL(String userId, WikiPageKey wikiPageKey, String fileName)	throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		// First lookup the FileHandle
		String fileHandleId = wikiManager.getFileHandleIdForFileName(user, wikiPageKey, fileName);
		// Get FileHandle
		String previewId = fileHandleManager.getPreviewFileHandleId(fileHandleId);
		// Get the URL of the preview.
		return fileHandleManager.getRedirectURLForFileHandle(previewId);
	}

	@Override
	public WikiPage getRootWikiPage(String userId, String ownerId, ObjectType type) throws UnauthorizedException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getRootWikiPage(user, ownerId, type);
	}

}
