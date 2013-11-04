package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.V2WikiPageMigrationDao;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.migration.WikiMigrationResult;
import org.sagebionetworks.repo.model.migration.WikiMigrationResultType;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class WikiMigrationServiceTest {
	@Autowired
	private WikiMigrationService wikiMigrationService;
	@Autowired
	private WikiPageDao wikiPageDao;
	@Autowired
	private V2WikiPageDao v2wikiPageDAO;
	@Autowired
	private V2WikiPageMigrationDao v2WikiPageMigrationDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private FileHandleDao fileMetadataDao;
	@Autowired
	private AmazonS3Client s3Client;
	@Autowired
	public UserManager userManager;

	String creatorUserGroupId;
	List<WikiPageKey> toDelete;
	List<WikiPageKey> toDeleteForParentCase;
	UserInfo adminUserInfo;
	
	@Before
	public void before() throws NotFoundException {
		toDelete = new ArrayList<WikiPageKey>(); 
		toDeleteForParentCase = new ArrayList<WikiPageKey>();
		
		UserGroup userGroup = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false);
		assertNotNull(userGroup);
		creatorUserGroupId = userGroup.getId();
		assertNotNull(creatorUserGroupId);
		
		adminUserInfo = userManager.getUserInfo(AuthorizationConstants.ADMIN_USER_NAME);
	}
	
	@After
	public void after() throws NotFoundException {
		// Clean up wikis from both v1 and v2 tables
		if(wikiPageDao != null && toDelete != null) {
			for(WikiPageKey id: toDelete) {
				wikiPageDao.delete(id);
			}
		}
		// We only want to delete from V2 with the keys of toDelete when its not the special parent case
		if(v2wikiPageDAO != null && toDelete != null && toDeleteForParentCase.size() == 0) {
			for(WikiPageKey id: toDelete) {
				V2WikiPage wiki = v2wikiPageDAO.get(id);
				String markdownHandleId = wiki.getMarkdownFileHandleId();
				S3FileHandle markdownHandle = (S3FileHandle) fileMetadataDao.get(markdownHandleId);
				s3Client.deleteObject(markdownHandle.getBucketName(), markdownHandle.getKey());
				fileMetadataDao.delete(markdownHandleId);
				v2wikiPageDAO.delete(id);
			}
		}
		if(v2wikiPageDAO != null && toDeleteForParentCase != null) {
			for(int i = 0; i < toDeleteForParentCase.size(); i++) {
				WikiPageKey key = toDeleteForParentCase.get(i);
				V2WikiPage wiki = v2wikiPageDAO.get(key);
				String markdownHandleId = wiki.getMarkdownFileHandleId();
				S3FileHandle markdownHandle = (S3FileHandle) fileMetadataDao.get(markdownHandleId);
				s3Client.deleteObject(markdownHandle.getBucketName(), markdownHandle.getKey());
				fileMetadataDao.delete(markdownHandleId);
				v2wikiPageDAO.delete(key);
			}
		}
	}
	
	@Test
	public void testMigrateAndRemigrateSomeWikis() throws NotFoundException, IOException {
		// Create some wiki pages
		createWikiPages(1, 3);
		String userName = adminUserInfo.getIndividualGroup().getName();
		// Migrate some wiki pages
		PaginatedResults<WikiMigrationResult> results = wikiMigrationService.migrateSomeWikis(userName, 3, 0, "somePath");
		assertNotNull(results);
		// Limit of 3 should have been returned
		assertEquals(3, results.getResults().size());
		// Size of the V2 DB should be equal to the number of wikis successfully migrated
		assertEquals(3, v2wikiPageDAO.getCount());	
		
		// Re-migrate the same wiki page.
		PaginatedResults<WikiMigrationResult> duplicateMigrationResults = wikiMigrationService.migrateSomeWikis(userName, 1, 0, "somePath");
		assertNotNull(duplicateMigrationResults);
		assertEquals(1, duplicateMigrationResults.getResults().size());
		// Size of the V2 DB should still remain 3.
		assertEquals(3, v2wikiPageDAO.getCount());	
	}

	@Test
	public void testMigrateSomeWikisWithParents() throws NotFoundException, IOException {
		String ownerId = "syn1";
		ObjectType ownerType = ObjectType.ENTITY;
		
		WikiPage greatAncestor = new WikiPage();
		greatAncestor.setId("1");
		greatAncestor.setParentWikiId(null);
		greatAncestor.setCreatedBy(creatorUserGroupId);
		greatAncestor.setModifiedBy(creatorUserGroupId);
		greatAncestor.setMarkdown("markdown1");
		greatAncestor.setTitle("title1");			
		greatAncestor.setAttachmentFileHandleIds(new ArrayList<String>());
		Map<String, FileHandle> fileNameToFileHandleMap = new HashMap<String, FileHandle>();
		greatAncestor = wikiPageDao.create(greatAncestor, fileNameToFileHandleMap, ownerId, ownerType);
		assertNotNull(greatAncestor);
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, greatAncestor.getId());
		
		WikiPage ancestor = new WikiPage();
		ancestor.setId("2");
		ancestor.setParentWikiId("1");
		ancestor.setCreatedBy(creatorUserGroupId);
		ancestor.setModifiedBy(creatorUserGroupId);
		ancestor.setMarkdown("markdown1");
		ancestor.setTitle("title1");			
		ancestor.setAttachmentFileHandleIds(new ArrayList<String>());
		Map<String, FileHandle> fileNameToFileHandleMap2 = new HashMap<String, FileHandle>();
		ancestor = wikiPageDao.create(ancestor, fileNameToFileHandleMap2, ownerId, ownerType);
		assertNotNull(ancestor);
		WikiPageKey key2 = new WikiPageKey(ownerId, ownerType, ancestor.getId());

		WikiPage grandparent = new WikiPage();
		grandparent.setId("3");
		grandparent.setParentWikiId("2");
		grandparent.setCreatedBy(creatorUserGroupId);
		grandparent.setModifiedBy(creatorUserGroupId);
		grandparent.setMarkdown("markdown1");
		grandparent.setTitle("title1");			
		grandparent.setAttachmentFileHandleIds(new ArrayList<String>());
		Map<String, FileHandle> fileNameToFileHandleMap3 = new HashMap<String, FileHandle>();
		grandparent = wikiPageDao.create(grandparent, fileNameToFileHandleMap3, ownerId, ownerType);
		assertNotNull(grandparent);
		WikiPageKey key3 = new WikiPageKey(ownerId, ownerType, grandparent.getId());
		
		WikiPage parent = new WikiPage();
		parent.setId("4");
		parent.setParentWikiId("3");
		parent.setCreatedBy(creatorUserGroupId);
		parent.setModifiedBy(creatorUserGroupId);
		parent.setMarkdown("markdown1");
		parent.setTitle("title1");			
		parent.setAttachmentFileHandleIds(new ArrayList<String>());
		Map<String, FileHandle> fileNameToFileHandleMap4 = new HashMap<String, FileHandle>();
		parent = wikiPageDao.create(parent, fileNameToFileHandleMap4, ownerId, ownerType);
		assertNotNull(parent);
		WikiPageKey key4 = new WikiPageKey(ownerId, ownerType, parent.getId());
		
		WikiPage child = new WikiPage();
		child.setId("5");
		child.setParentWikiId("4");
		child.setCreatedBy(creatorUserGroupId);
		child.setModifiedBy(creatorUserGroupId);
		child.setMarkdown("markdown1");
		child.setTitle("title1");			
		child.setAttachmentFileHandleIds(new ArrayList<String>());
		Map<String, FileHandle> fileNameToFileHandleMap5 = new HashMap<String, FileHandle>();
		child = wikiPageDao.create(child, fileNameToFileHandleMap5, ownerId, ownerType);
		assertNotNull(child);
		WikiPageKey key5 = new WikiPageKey(ownerId, ownerType, child.getId());
		
		// Migrate the PARENT first
		// greatAncestor, ancestor, and grandparent must be migrated first
		V2WikiPage parentResult = wikiMigrationService.migrate(parent, adminUserInfo);
		// By migrating parent, the V2 DB should hold four wiki pages
		assertEquals(4, v2wikiPageDAO.getCount());
		/*
		assertEquals(true, v2WikiPageMigrationDao.doesParentExist(parent.getParentWikiId()));
		assertEquals(true, v2WikiPageMigrationDao.doesParentExist(grandparent.getParentWikiId()));
		assertEquals(true, v2WikiPageMigrationDao.doesParentExist(ancestor.getParentWikiId()));
		assertEquals(true, v2WikiPageMigrationDao.doesParentExist(child.getParentWikiId()));
		*/
		V2WikiPage childResult = wikiMigrationService.migrate(child, adminUserInfo);
		assertEquals(5, v2wikiPageDAO.getCount());
		
		// Add keys to list of wikis to delete / start with child to avoid cascade delete
		toDeleteForParentCase.add(key5);
		toDeleteForParentCase.add(key4);
		toDeleteForParentCase.add(key3);
		toDeleteForParentCase.add(key2);
		toDeleteForParentCase.add(key);

		// Nothing to clean up in V1 wiki pages, so casacde deleting is okay
		toDelete.add(key5);
	}
	
	private void createWikiPages(int start, int end) throws NotFoundException {
		for(int i = start; i <= end; i++) {
			String ownerId = "syn" + i;
			ObjectType ownerType = ObjectType.ENTITY;
			
			WikiPage page = new WikiPage();
			page.setId("" + i);
			page.setParentWikiId(null);
			page.setCreatedBy(creatorUserGroupId);
			page.setModifiedBy(creatorUserGroupId);
			page.setMarkdown("markdown" + i);
			page.setTitle("title" + i);			
			page.setAttachmentFileHandleIds(new ArrayList<String>());
			Map<String, FileHandle> fileNameToFileHandleMap = new HashMap<String, FileHandle>();
			WikiPage result = wikiPageDao.create(page, fileNameToFileHandleMap, ownerId, ownerType);
			assertNotNull(result);
			WikiPageKey key = wikiPageDao.lookupWikiKey(result.getId());
			toDelete.add(key);
		}
	}

}
