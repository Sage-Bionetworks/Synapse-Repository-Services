package org.sagebionetworks.repo.web.service;

import java.net.URL;

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
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
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
	public V2WikiPage createWikiPage(String userId, String objectId,
			ObjectType entity, V2WikiPage toCreate) throws DatastoreException,
			NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.createWikiPage(user, objectId, entity, toCreate);
	}

	@Override
	public V2WikiPage getWikiPage(String userId, WikiPageKey key, Long version)
			throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getWikiPage(user, key, version);
	}
	
	@Override
	public V2WikiPage updateWikiPage(String userId, String objectId,
			ObjectType objectType, V2WikiPage toUpdate)
			throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.updateWikiPage(user, objectId, objectType, toUpdate);
	}

	@Override
	public V2WikiPage restoreWikipage(String userId, String objectId,
			ObjectType objectType, String wikiId, Long version)
			throws NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.restoreWikiPage(user, objectId, objectType, version, wikiId);
	}

	@Override
	public void deleteWikiPage(String userId, WikiPageKey wikiPageKey)
			throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		wikiManager.deleteWiki(user, wikiPageKey);
	}

	@Override
	public PaginatedResults<V2WikiHeader> getWikiHeaderTree(String userId,
			String ownerId, ObjectType type, Long limit, Long offest)
			throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getWikiHeaderTree(user, ownerId, type, limit, offest);
	}

	@Override
	public PaginatedResults<V2WikiHistorySnapshot> getWikiHistory(
			String userId, String ownerId, ObjectType type, Long limit,
			Long offset, WikiPageKey wikiPageKey) throws DatastoreException,
			NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getWikiHistory(user, ownerId, type, wikiPageKey, limit, offset);
	}

	@Override
	public FileHandleResults getAttachmentFileHandles(String userId,
			WikiPageKey wikiPageKey, Long version) throws DatastoreException,
			NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getAttachmentFileHandles(user, wikiPageKey, version);
	}

	@Override
	public URL getAttachmentRedirectURL(String userId, WikiPageKey wikiPageKey,
			String fileName, Long version) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		String id = wikiManager.getFileHandleIdForFileName(user, wikiPageKey, fileName, version);
		return fileHandleManager.getRedirectURLForFileHandle(id);
	}

	@Override
	public URL getAttachmentPreviewRedirectURL(String userId,
			WikiPageKey wikiPageKey, String fileName, Long version)
			throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		String id = wikiManager.getFileHandleIdForFileName(user, wikiPageKey, fileName, version);
		String previewId = fileHandleManager.getPreviewFileHandleId(id);
		// Get the URL of the preview.
		return fileHandleManager.getRedirectURLForFileHandle(previewId);
	}

	@Override
	public V2WikiPage getRootWikiPage(String userId, String ownerId,
			ObjectType type) throws UnauthorizedException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return wikiManager.getRootWikiPage(user, ownerId, type);
	}

	@Override
	public URL getMarkdownRedirectURL(String userId, WikiPageKey wikiPageKey, Long version)
			throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		String id = wikiManager.getMarkdownFileHandleId(user, wikiPageKey, version);
		return fileHandleManager.getRedirectURLForFileHandle(id);
	}

}
