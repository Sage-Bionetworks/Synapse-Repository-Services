package org.sagebionetworks.repo.manager.backup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.backup.WikiPageBackup;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dbo.WikiTranslationUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

public class StubWikiPageDao implements WikiPageDao {

	private Map<WikiPageKey, WikiPageBackup> backupMap = new HashMap<WikiPageKey, WikiPageBackup>();

	@Override
	public long getCount() throws DatastoreException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public WikiPage create(WikiPage toCreate,
			Map<String, FileHandle> fileNameToFileHandleMap, String ownerId,
			ObjectType ownerType) throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WikiPage updateWikiPage(WikiPage toUpdate,
			Map<String, FileHandle> fileNameToFileHandleMap, String ownerId,
			ObjectType ownerType, boolean keepEtag) throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WikiPage get(WikiPageKey key) throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getRootWiki(String ownerId, ObjectType ownerType)
			throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(WikiPageKey key) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<WikiHeader> getHeaderTree(String ownerId, ObjectType ownerType)
			throws DatastoreException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String lockForUpdate(String wikiId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getWikiFileHandleIds(WikiPageKey key)
			throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getWikiAttachmentFileHandleForFileName(WikiPageKey key,
			String fileName) throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WikiPageKey lookupWikiKey(String wikiId) throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

}
