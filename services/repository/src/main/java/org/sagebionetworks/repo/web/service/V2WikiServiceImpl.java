package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.manager.wiki.V2WikiManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * V2 WikiService implementation.
 * (Derived from org.sagebionetworks.repo.web.service.WikiServiceImpl)
 * @author hso
 *
 */
public class V2WikiServiceImpl implements V2WikiService {

	@Autowired
	UserManager userManager;
	@Autowired
	V2WikiManager wikiManager;
	@Autowired
	FileHandleManager fileHandleManager;
	
	@Override
	public V2WikiPage createWikiPage(Long userId, String objectId,
			ObjectType entity, V2WikiPage toCreate) throws DatastoreException,
			NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.createWikiPage(user, objectId, entity, toCreate);
	}

	@Override
	public V2WikiPage getWikiPage(Long userId, WikiPageKey key, Long version)
			throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getWikiPage(user, key, version);
	}
	
	@Override
	public V2WikiPage updateWikiPage(Long userId, String objectId,
			ObjectType objectType, V2WikiPage toUpdate)
			throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.updateWikiPage(user, objectId, objectType, toUpdate);
	}

	@Override
	public V2WikiPage restoreWikipage(Long userId, String objectId,
			ObjectType objectType, String wikiId, Long version)
			throws NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.restoreWikiPage(user, objectId, objectType, version, wikiId);
	}

	@Override
	public void deleteWikiPage(Long userId, WikiPageKey wikiPageKey)
			throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		wikiManager.deleteWiki(user, wikiPageKey);
	}

	@Override
	public PaginatedResults<V2WikiHeader> getWikiHeaderTree(Long userId,
			String ownerId, ObjectType type, Long limit, Long offest)
			throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getWikiHeaderTree(user, ownerId, type, limit, offest);
	}
	
	@Override
	public V2WikiOrderHint getWikiOrderHint(Long userId, String ownerId, ObjectType type)
			throws NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getOrderHint(user, ownerId, type);
	}
	
	@Override
	public V2WikiOrderHint updateWikiOrderHint(Long userId,
			V2WikiOrderHint orderHint)
			throws NotFoundException{
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.updateOrderHint(user, orderHint);
	}

	@Override
	public PaginatedResults<V2WikiHistorySnapshot> getWikiHistory(
			Long userId, String ownerId, ObjectType type, Long limit,
			Long offset, WikiPageKey wikiPageKey) throws DatastoreException,
			NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getWikiHistory(user, ownerId, type, wikiPageKey, limit, offset);
	}

	@Override
	public FileHandleResults getAttachmentFileHandles(Long userId,
			WikiPageKey wikiPageKey, Long version) throws DatastoreException,
			NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getAttachmentFileHandles(user, wikiPageKey, version);
	}

	@Override
	public String getAttachmentRedirectURL(Long userId, WikiPageKey wikiPageKey,
			String fileName, Long version) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		String fileHandleId = wikiManager.getFileHandleIdForFileName(userInfo, wikiPageKey, fileName, version);
		
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandleId)
				.withAssociation(FileHandleAssociateType.WikiAttachment, wikiPageKey.getWikiPageId());
		
		return fileHandleManager.getRedirectURLForFileHandle(urlRequest);
	}

	@Override
	public String getAttachmentPreviewRedirectURL(Long userId,
			WikiPageKey wikiPageKey, String fileName, Long version)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		String fileHandleId = wikiManager.getFileHandleIdForFileName(userInfo, wikiPageKey, fileName, version);
		String fileHandlePreviewId = fileHandleManager.getPreviewFileHandleId(fileHandleId);
		

		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandlePreviewId)
				.withAssociation(FileHandleAssociateType.WikiAttachment, wikiPageKey.getWikiPageId());
		
		return fileHandleManager.getRedirectURLForFileHandle(urlRequest);
	}

	@Override
	public V2WikiPage getRootWikiPage(Long userId, String ownerId,
			ObjectType type) throws UnauthorizedException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getRootWikiPage(user, ownerId, type);
	}

	@Override
	public String getMarkdownRedirectURL(Long userId, WikiPageKey wikiPageKey, Long version)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		String fileHandleId = wikiManager.getMarkdownFileHandleId(userInfo, wikiPageKey, version);
		
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandleId)
				.withAssociation(FileHandleAssociateType.WikiMarkdown, wikiPageKey.getWikiPageId());
		
		return fileHandleManager.getRedirectURLForFileHandle(urlRequest);
	}

}
