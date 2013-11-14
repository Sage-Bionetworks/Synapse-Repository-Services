package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.wiki.V2WikiManager;
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
	WikiModelTranslator wikiModelTranslationHelper;
	@Autowired
	FileHandleManager fileHandleManager;
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage createWikiPage(String userId, String objectId,	ObjectType objectType, WikiPage toCreate) throws DatastoreException, NotFoundException, IOException {
		// Resolve the userID
		UserInfo user = userManager.getUserInfo(userId);
		V2WikiPage translated = wikiModelTranslationHelper.convertToV2WikiPage(toCreate, user);
		V2WikiPage result = v2WikiManager.createWikiPage(user, objectId, objectType, translated);
		WikiPage translatedResult = wikiModelTranslationHelper.convertToWikiPage(result);
		return wikiManager.createWikiPage(user, objectId, objectType, toCreate);
	}

	@Override
	public WikiPage getWikiPage(String userId, WikiPageKey key) throws DatastoreException, NotFoundException, IOException {
		UserInfo user = userManager.getUserInfo(userId);
		//V2WikiPage result = v2WikiManager.getWikiPage(user, key);
		return wikiManager.getWikiPage(user, key);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage updateWikiPage(String userId, String objectId,	ObjectType objectType, WikiPage toUpdate) throws DatastoreException, NotFoundException, IOException {
		UserInfo user = userManager.getUserInfo(userId);
		V2WikiPage translated = wikiModelTranslationHelper.convertToV2WikiPage(toUpdate, user);
		V2WikiPage result = v2WikiManager.updateWikiPage(user, objectId, objectType, translated);
		WikiPage translatedResult = wikiModelTranslationHelper.convertToWikiPage(result);
		return wikiManager.updateWikiPage(user, objectId, objectType, toUpdate);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteWikiPage(String userId, WikiPageKey wikiPageKey) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		v2WikiManager.deleteWiki(user, wikiPageKey);
		wikiManager.deleteWiki(user, wikiPageKey);
	}

	@Override
	public PaginatedResults<WikiHeader> getWikiHeaderTree(String userId, String ownerId, ObjectType type, Long limit, Long offest) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		/*
		PaginatedResults<V2WikiHeader> headerResults = v2WikiManager.getWikiHeaderTree(user, ownerId, type, limit, offest);
		List<V2WikiHeader> resultAsList = headerResults.getResults();
		List<WikiHeader> convertedList = new ArrayList<WikiHeader>(); 
		for(int i = 0; i < resultAsList.size(); i++) {
			V2WikiHeader header = resultAsList.get(i);
			WikiHeader newHeader = new WikiHeader();
			newHeader.setId(header.getId());
			newHeader.setParentId(header.getParentId());
			newHeader.setTitle(header.getTitle());
			convertedList.add(newHeader);
		}
		PaginatedResults<WikiHeader> convertedPaginatedResults = new PaginatedResults<WikiHeader>(convertedList, convertedList.size());
		*/
		return wikiManager.getWikiHeaderTree(user, ownerId, type, limit, offest);
	}

	@Override
	public FileHandleResults getAttachmentFileHandles(String userId, WikiPageKey wikiPageKey) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		//FileHandleResults result = v2WikiManager.getAttachmentFileHandles(user, wikiPageKey);
		return wikiManager.getAttachmentFileHandles(user, wikiPageKey);
	}

	@Override
	public URL getAttachmentRedirectURL(String userId, WikiPageKey wikiPageKey,	String fileName) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		// First lookup the FileHandle
		//String fileHandleId = v2WikiManager.getFileHandleIdForFileName(user, wikiPageKey, fileName);
		String fileHandleId = wikiManager.getFileHandleIdForFileName(user, wikiPageKey, fileName);
		// Use the FileHandle ID to get the URL
		return fileHandleManager.getRedirectURLForFileHandle(fileHandleId);
	}

	@Override
	public URL getAttachmentPreviewRedirectURL(String userId, WikiPageKey wikiPageKey, String fileName)	throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		// First lookup the FileHandle
		//String fileHandleId = v2WikiManager.getFileHandleIdForFileName(user, wikiPageKey, fileName);
		String fileHandleId = wikiManager.getFileHandleIdForFileName(user, wikiPageKey, fileName);
		// Get FileHandle
		String previewId = fileHandleManager.getPreviewFileHandleId(fileHandleId);
		// Get the URL of the preview.
		return fileHandleManager.getRedirectURLForFileHandle(previewId);
	}

	@Override
	public WikiPage getRootWikiPage(String userId, String ownerId, ObjectType type) throws UnauthorizedException, NotFoundException, IOException {
		UserInfo user = userManager.getUserInfo(userId);
		/*
		V2WikiPage result = v2WikiManager.getRootWikiPage(user, ownerId, type);
		WikiPage translatedResult = wikiModelTranslationHelper.convertToWikiPage(result);
		*/
		return wikiManager.getRootWikiPage(user, ownerId, type);
	}

}
