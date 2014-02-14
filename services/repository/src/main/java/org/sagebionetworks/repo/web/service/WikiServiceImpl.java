package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.wiki.V2WikiManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
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
	V2WikiManager v2WikiManager;
	@Autowired
	WikiModelTranslator wikiModelTranslationHelper;
	@Autowired
	FileHandleManager fileHandleManager;
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage createWikiPage(Long userId, String objectId,	ObjectType objectType, WikiPage toCreate) throws DatastoreException, NotFoundException, IOException {
		// Resolve the userID
		UserInfo user = userManager.getUserInfo(userId);
		// Translate the created V1 wiki into a V2 and create it
		V2WikiPage translated = wikiModelTranslationHelper.convertToV2WikiPage(toCreate, user);
		V2WikiPage created = v2WikiManager.createWikiPage(user, objectId, objectType, translated);
		// Return it as a V1 wiki
		return wikiModelTranslationHelper.convertToWikiPage(created);
	}

	@Override
	public WikiPage getWikiPage(Long userId, WikiPageKey key) throws DatastoreException, NotFoundException, IOException {
		UserInfo user = userManager.getUserInfo(userId);
		// Return most recent version of the wiki because V1 service doesn't have history
		V2WikiPage wiki = v2WikiManager.getWikiPage(user, key, null);
		return wikiModelTranslationHelper.convertToWikiPage(wiki);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage updateWikiPage(Long userId, String objectId,	ObjectType objectType, WikiPage toUpdate) throws DatastoreException, NotFoundException, IOException {
		UserInfo user = userManager.getUserInfo(userId);
		// Translate the updated V1 wiki
		V2WikiPage translated = wikiModelTranslationHelper.convertToV2WikiPage(toUpdate, user);
		// Update
		V2WikiPage updated = v2WikiManager.updateWikiPage(user, objectId, objectType, translated);
		// Return as a V1 wiki
		return wikiModelTranslationHelper.convertToWikiPage(updated);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteWikiPage(Long userId, WikiPageKey wikiPageKey) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		// Delete
		v2WikiManager.deleteWiki(user, wikiPageKey);
	}

	@Override
	public PaginatedResults<WikiHeader> getWikiHeaderTree(Long userId, String ownerId, ObjectType type, Long limit, Long offest) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		// Get the v2 wiki headers and translate them into v1 headers
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
		// create the paginated results with the translated headers and return
		PaginatedResults<WikiHeader> convertedPaginatedResults = new PaginatedResults<WikiHeader>(convertedList, convertedList.size());
		return convertedPaginatedResults;
	}

	@Override
	public FileHandleResults getAttachmentFileHandles(Long userId, WikiPageKey wikiPageKey) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		// Get most recent file handles because V1 service doesn't have history of wiki file handles
		return v2WikiManager.getAttachmentFileHandles(user, wikiPageKey, null);
	}

	@Override
	public URL getAttachmentRedirectURL(Long userId, WikiPageKey wikiPageKey,	String fileName) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		// First lookup the FileHandle from V2
		String fileHandleId = v2WikiManager.getFileHandleIdForFileName(user, wikiPageKey, fileName, null);
		// Use the FileHandle ID to get the URL
		return fileHandleManager.getRedirectURLForFileHandle(fileHandleId);
	}

	@Override
	public URL getAttachmentPreviewRedirectURL(Long userId, WikiPageKey wikiPageKey, String fileName)	throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		// First lookup the FileHandle
		String fileHandleId = v2WikiManager.getFileHandleIdForFileName(user, wikiPageKey, fileName, null);
		// Get FileHandle from V2
		String previewId = fileHandleManager.getPreviewFileHandleId(fileHandleId);
		// Get the URL of the preview.
		return fileHandleManager.getRedirectURLForFileHandle(previewId);
	}

	@Override
	public WikiPage getRootWikiPage(Long userId, String ownerId, ObjectType type) throws UnauthorizedException, NotFoundException, IOException {
		UserInfo user = userManager.getUserInfo(userId);
		// Get the root V2 wiki
		V2WikiPage root = v2WikiManager.getRootWikiPage(user, ownerId, type);
		// Return as a V1 wiki
		return wikiModelTranslationHelper.convertToWikiPage(root);
	}

}
