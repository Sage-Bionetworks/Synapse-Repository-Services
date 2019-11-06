package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.manager.wiki.V2WikiManager;
import org.sagebionetworks.repo.manager.wiki.V2WikiManagerImpl;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.WikiModelTranslator;
import org.springframework.beans.factory.annotation.Autowired;

public class WikiServiceImpl implements WikiService {
	
	@Autowired
	UserManager userManager;
	@Autowired
	V2WikiManager v2WikiManager;
	@Autowired
	WikiModelTranslator wikiModelTranslationHelper;
	@Autowired
	FileHandleManager fileHandleManager;
	
	@WriteTransaction
	@Override
	public WikiPage createWikiPage(Long userId, String objectId, ObjectType objectType, WikiPage toCreate) throws DatastoreException, NotFoundException, IOException {
		// Resolve the userID
		UserInfo user = userManager.getUserInfo(userId);
		// Translate the created V1 wiki into a V2 and create it
		V2WikiPage translated = wikiModelTranslationHelper.convertToV2WikiPage(toCreate, user);
		V2WikiPage created = v2WikiManager.createWikiPage(user, objectId, objectType, translated);
		// Return it as a V1 wiki
		return wikiModelTranslationHelper.convertToWikiPage(created);
	}

	@Override
	public WikiPage getWikiPage(Long userId, WikiPageKey key, Long version) throws DatastoreException, NotFoundException, IOException {
		UserInfo user = userManager.getUserInfo(userId);
		// Return most recent version of the wiki because V1 service doesn't have history
		V2WikiPage wiki = v2WikiManager.getWikiPage(user, key, version);
		return wikiModelTranslationHelper.convertToWikiPage(wiki);
	}

	@WriteTransaction
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

	@WriteTransaction
	@Override
	public void deleteWikiPage(Long userId, WikiPageKey wikiPageKey) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		// Delete
		v2WikiManager.deleteWiki(user, wikiPageKey);
	}

	@Override
	public PaginatedResults<WikiHeader> getWikiHeaderTree(Long userId, String ownerId, ObjectType type, Long limit, Long offset) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		if(limit == null){
			limit = V2WikiManagerImpl.MAX_LIMIT;
		}
		if(offset == null){
			offset = 0L;
		}
		// Get the v2 wiki headers and translate them into v1 headers
		PaginatedResults<V2WikiHeader> headerResults = v2WikiManager.getWikiHeaderTree(user, ownerId, type, limit, offset);
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
		return PaginatedResults.createWithLimitAndOffset(convertedList, limit, offset);
	}

	@Override
	public FileHandleResults getAttachmentFileHandles(Long userId, WikiPageKey wikiPageKey) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		// Get most recent file handles because V1 service doesn't have history of wiki file handles
		return v2WikiManager.getAttachmentFileHandles(user, wikiPageKey, null);
	}

	@Override
	public String getAttachmentRedirectURL(Long userId, WikiPageKey wikiPageKey, String fileName) throws DatastoreException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		// First lookup the FileHandle from V2
		String fileHandleId = v2WikiManager.getFileHandleIdForFileName(userInfo, wikiPageKey, fileName, null);
		
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandleId)
				.withAssociation(FileHandleAssociateType.WikiAttachment, wikiPageKey.getWikiPageId());
		
		return fileHandleManager.getRedirectURLForFileHandle(urlRequest);
	}

	@Override
	public String getAttachmentPreviewRedirectURL(Long userId, WikiPageKey wikiPageKey, String fileName) throws DatastoreException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		// First lookup the FileHandle
		String fileHandleId = v2WikiManager.getFileHandleIdForFileName(userInfo, wikiPageKey, fileName, null);
		// Get FileHandle from V2
		String fileHandlePreviewId = fileHandleManager.getPreviewFileHandleId(fileHandleId);

		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandlePreviewId)
				.withAssociation(FileHandleAssociateType.WikiAttachment, wikiPageKey.getWikiPageId());
		
		return fileHandleManager.getRedirectURLForFileHandle(urlRequest);
	}

	@Override
	public WikiPage getRootWikiPage(Long userId, String ownerId, ObjectType type) throws UnauthorizedException, NotFoundException, IOException {
		UserInfo user = userManager.getUserInfo(userId);
		// Get the root V2 wiki
		V2WikiPage root = v2WikiManager.getRootWikiPage(user, ownerId, type);
		// Return as a V1 wiki
		return wikiModelTranslationHelper.convertToWikiPage(root);
	}

	@Override
	public WikiPageKey getRootWikiKey(Long userId, String ownerId,
			ObjectType type) throws NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return v2WikiManager.getRootWikiKey(user, ownerId, type);
	}

}
