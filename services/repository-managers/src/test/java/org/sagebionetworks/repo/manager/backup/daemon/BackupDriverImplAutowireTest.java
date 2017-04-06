package org.sagebionetworks.repo.manager.backup.daemon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.Progress;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BackupDriverImplAutowireTest {
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	private V2WikiPageDao wikiPageDao;
	
	@Autowired
	private UserManager userManager;	
	
	@Autowired
	private EntityBootstrapper entityBootstrapper;
	
	@Autowired
	private BackupDriver backupDriver;

	@Autowired
	private IdGenerator idGenerator;
	
	private List<String> toDelete;
	private UserInfo adminUser;
	private String creatorUserGroupId;
	private S3FileHandle withPreview;
	private PreviewFileHandle preview;
	private FileHandle markdownFileHandle;
	private WikiPageKey wikiKey;
	private V2WikiPage wiki;
	private Map<String, FileHandle> fileNameToFileHandleMap;
	private File backupOne;
	private File backupTwo;
	
	@Before
	public void before() throws Exception {
		toDelete = new LinkedList<String>();
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		creatorUserGroupId = adminUser.getId().toString();
		assertNotNull(creatorUserGroupId);
		// The one will have a preview
		withPreview = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		withPreview.setFileName("withPreview.txt");
		// The Preview
		preview = TestUtils.createPreviewFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		preview.setFileName("preview.txt");

		markdownFileHandle = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		markdownFileHandle.setFileName("markdown.txt");

		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		fileHandleToCreate.add(withPreview);
		fileHandleToCreate.add(preview);
		fileHandleToCreate.add(markdownFileHandle);
		fileHandleDao.createBatch(fileHandleToCreate);

		withPreview = (S3FileHandle) fileHandleDao.get(withPreview.getId());
		assertNotNull(withPreview);
		toDelete.add(withPreview.getId());
		preview = (PreviewFileHandle) fileHandleDao.get(preview.getId());
		assertNotNull(preview);
		toDelete.add(preview.getId());
		markdownFileHandle = fileHandleDao.get(markdownFileHandle.getId());
		toDelete.add(markdownFileHandle.getId());

		// Assign it as a preview
		fileHandleDao.setPreviewId(withPreview.getId(), preview.getId());
		// The etag should have changed
		withPreview = (S3FileHandle) fileHandleDao.get(withPreview.getId());
		String ownerId = "123";
		ObjectType ownerType = ObjectType.EVALUATION;
		wiki = new V2WikiPage();
		wiki.setTitle("testPLFM_1937");
		wiki.setMarkdownFileHandleId(markdownFileHandle.getId());
		wiki.setCreatedBy(adminUser.getId().toString());
		wiki.setModifiedBy(wiki.getCreatedBy());
		wiki.setCreatedOn(new Date());
		wiki.setModifiedOn(wiki.getCreatedOn());
		wiki.setEtag("etag");
		fileNameToFileHandleMap = new HashMap<String, FileHandle>();
		fileNameToFileHandleMap.put(withPreview.getFileName(), withPreview);
		fileNameToFileHandleMap.put(preview.getFileName(), preview);
		List<String> newFileHandleIds = new ArrayList<String>();
		newFileHandleIds.add(withPreview.getId());
		newFileHandleIds.add(preview.getId());
		// Now create the wikipage
		wiki = wikiPageDao.create(wiki, fileNameToFileHandleMap, ownerId, ownerType, newFileHandleIds);
		assertEquals(2, wiki.getAttachmentFileHandleIds().size());
		wikiKey = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wiki.getId());
	}
	
	@After
	public void after() throws Exception{
		// Since this test can delete all data make sure bootstrap data gets put back.
		entityBootstrapper.bootstrapAll();
		// If we have deleted all data make sure the bootstrap process puts it back
		if(fileHandleDao != null && toDelete != null){
			for(String id: toDelete){
				fileHandleDao.delete(id);
			}
		}
		if(wikiKey != null){
			wikiPageDao.delete(wikiKey);
		}
		if(backupOne != null){
			backupOne.delete();
		}
		if(backupTwo != null){
			backupTwo.delete();
		}
	}
	
	@Test
	public void testPLFM_1937() throws Exception{
		// PLFM-1937 is a bug where we were not clearing rows of secondary tables on restore. So rows deleted 
		// in secondary tables on the source stack would not get deleted on the destination stack.
		
		// To reproduce this bug we will use a wiki page with file attachments.  For this case, a wiki is a primary table
		// and the attachments are secondary tables.
		// First we will create a wiki page with two file attachments and create backup one.
		// Then we will delete one of the attachments and create backup two.
		// The wiki will then be deleted, then backup one applied, followed by backup two.  When the second backup is applied
		// the deleted attachment should also get deleted.

		// Now create a backup
		List<Long> ids1 = new LinkedList<Long>();
		ids1.add(Long.parseLong(wiki.getId()));
		backupOne = File.createTempFile("backupOne", ".zip");
		backupDriver.writeBackup(adminUser, backupOne, new Progress(), MigrationType.V2_WIKI_PAGE, ids1);
		// Now delete one of the attachments and take another backup copy.
		fileNameToFileHandleMap.remove(preview.getFileName());
		wiki = wikiPageDao.updateWikiPage(wiki, fileNameToFileHandleMap, wikiKey.getOwnerObjectId(), wikiKey.getOwnerObjectType(), new ArrayList<String>());
		assertEquals(1, wiki.getAttachmentFileHandleIds().size());
		// Now create another backup
		backupTwo = File.createTempFile("backupTwo", ".zip");
		backupDriver.writeBackup(adminUser, backupTwo, new Progress(), MigrationType.V2_WIKI_PAGE, ids1);
		
		// Now apply the first backup, should restore the deleted attachment
		backupDriver.restoreFromBackup(adminUser, backupOne, new Progress());
		// We should have two attachments
		wiki = wikiPageDao.get(wikiKey, null);
		assertEquals(2, wiki.getAttachmentFileHandleIds().size());
		// Now apply the second backup
		backupDriver.restoreFromBackup(adminUser, backupTwo, new Progress());
		wiki = wikiPageDao.get(wikiKey, null);
		assertEquals("update did not clear secondary table rows",1, wiki.getAttachmentFileHandleIds().size());
	}
	

}
