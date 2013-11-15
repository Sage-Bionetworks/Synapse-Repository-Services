package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.wiki.V2WikiManager;
import org.sagebionetworks.repo.manager.wiki.V2WikiMirrorManager;
import org.sagebionetworks.repo.manager.wiki.WikiManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.WikiModelTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class WikiServiceImpl implements WikiService {
	
	@Autowired
	UserManager userManager;
	@Autowired
	WikiManager wikiManager;
	@Autowired
	V2WikiManager v2WikiManager;
	@Autowired
	V2WikiMirrorManager v2WikiMirrorManager;
	@Autowired
	WikiModelTranslator wikiModelTranslationHelper;
	@Autowired
	FileHandleManager fileHandleManager;
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage createWikiPage(String userId, String objectId,	ObjectType objectType, WikiPage toCreate) throws DatastoreException, NotFoundException, IOException {
		// Resolve the userID
		UserInfo user = userManager.getUserInfo(userId);
		// Create the V1 wiki
		WikiPage createdResult = wikiManager.createWikiPage(user, objectId, objectType, toCreate);
		// Translate the created V1 wiki into a V2 and create it
		V2WikiPage translated = wikiModelTranslationHelper.convertToV2WikiPage(createdResult, user);
		V2WikiPage result = v2WikiMirrorManager.createWikiPage(user, objectId, objectType, translated);
		return createdResult;
	}

	@Override
	public WikiPage getWikiPage(String userId, WikiPageKey key) throws DatastoreException, NotFoundException, IOException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getWikiPage(user, key);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage updateWikiPage(String userId, String objectId,	ObjectType objectType, WikiPage toUpdate) throws DatastoreException, NotFoundException, IOException {
		UserInfo user = userManager.getUserInfo(userId);
		// Update the V1 wiki
		WikiPage updateResult = wikiManager.updateWikiPage(user, objectId, objectType, toUpdate);
		// Translate the updated V1 wiki
		V2WikiPage translated = wikiModelTranslationHelper.convertToV2WikiPage(updateResult, user);
		// Reset the updated etag to the previous etag so we can lock / updated etag will be set after the lock
		translated.setEtag(toUpdate.getEtag());
		// Update the V2 wiki and pass in the updated etag we will set
		V2WikiPage result = v2WikiMirrorManager.updateWikiPage(user, objectId, objectType, translated, updateResult.getEtag());
		return updateResult;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteWikiPage(String userId, WikiPageKey wikiPageKey) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		// Delete the V1 wiki and its mirror V2 wiki
		wikiManager.deleteWiki(user, wikiPageKey);
		v2WikiMirrorManager.deleteWiki(user, wikiPageKey);
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
	public WikiPage getRootWikiPage(String userId, String ownerId, ObjectType type) throws UnauthorizedException, NotFoundException, IOException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getRootWikiPage(user, ownerId, type);
	}

}
