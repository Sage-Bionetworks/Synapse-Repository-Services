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
		if(translated != null) {
			System.out.println("Translated!");
		} else {
			System.out.println("Translated is NULL!");
		}
		V2WikiPage result = v2WikiManager.createWikiPage(user, objectId, objectType, translated);
		if(result != null) {
			System.out.println("Return from v2 manager. Created Wiki");
		} else {
			System.out.println("V2Wiki is NULL");
		}
		WikiPage converted = wikiModelTranslationHelper.convertToWikiPage(result);
		if(converted != null) {
			System.out.println("Converted successfully!");
		} else {
			System.out.println("Converted is NULL.");
		}
		return converted;
	}

	@Override
	public WikiPage getWikiPage(String userId, WikiPageKey key) throws DatastoreException, NotFoundException, IOException {
		UserInfo user = userManager.getUserInfo(userId);
		V2WikiPage result = v2WikiManager.getWikiPage(user, key);
		return wikiModelTranslationHelper.convertToWikiPage(result);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage updateWikiPage(String userId, String objectId,	ObjectType objectType, WikiPage toUpdate) throws DatastoreException, NotFoundException, IOException {
		UserInfo user = userManager.getUserInfo(userId);
		V2WikiPage translated = wikiModelTranslationHelper.convertToV2WikiPage(toUpdate, user);
		V2WikiPage result = v2WikiManager.updateWikiPage(user, objectId, objectType, translated);
		return wikiModelTranslationHelper.convertToWikiPage(result);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteWikiPage(String userId, WikiPageKey wikiPageKey) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		v2WikiManager.deleteWiki(user, wikiPageKey);
	}

	@Override
	public PaginatedResults<WikiHeader> getWikiHeaderTree(String userId, String ownerId, ObjectType type, Long limit, Long offest) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
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
		return new PaginatedResults<WikiHeader>(convertedList, convertedList.size());
	}

	@Override
	public FileHandleResults getAttachmentFileHandles(String userId, WikiPageKey wikiPageKey) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return v2WikiManager.getAttachmentFileHandles(user, wikiPageKey);
	}

	@Override
	public URL getAttachmentRedirectURL(String userId, WikiPageKey wikiPageKey,	String fileName) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		// First lookup the FileHandle
		String fileHandleId = v2WikiManager.getFileHandleIdForFileName(user, wikiPageKey, fileName);
		// Use the FileHandle ID to get the URL
		return fileHandleManager.getRedirectURLForFileHandle(fileHandleId);
	}

	@Override
	public URL getAttachmentPreviewRedirectURL(String userId, WikiPageKey wikiPageKey, String fileName)	throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		// First lookup the FileHandle
		String fileHandleId = v2WikiManager.getFileHandleIdForFileName(user, wikiPageKey, fileName);
		// Get FileHandle
		String previewId = fileHandleManager.getPreviewFileHandleId(fileHandleId);
		// Get the URL of the preview.
		return fileHandleManager.getRedirectURLForFileHandle(previewId);
	}

	@Override
	public WikiPage getRootWikiPage(String userId, String ownerId, ObjectType type) throws UnauthorizedException, NotFoundException, IOException {
		UserInfo user = userManager.getUserInfo(userId);
		V2WikiPage result = v2WikiManager.getRootWikiPage(user, ownerId, type);
		return wikiModelTranslationHelper.convertToWikiPage(result);
	}

}
