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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
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
	private FileHandleDao fileMetadataDao;
	
	@Autowired
	private AmazonS3Client s3Client;
	
	@Autowired
	public UserManager userManager;

	private UserInfo adminUserInfo;
	private String creatorUserGroupId;
	
	private List<WikiPageKey> toDelete;
	private List<WikiPageKey> toDeleteForParentCase;
	private List<String> abandonedFileHandleIds;
	
	@Before
	public void before() throws NotFoundException {
		toDelete = new ArrayList<WikiPageKey>(); 
		toDeleteForParentCase = new ArrayList<WikiPageKey>();
		abandonedFileHandleIds = new ArrayList<String>();
		
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		creatorUserGroupId = adminUserInfo.getIndividualGroup().getId();
	}
	
	@After
	public void after() throws NotFoundException {
		// No file handles to clean first. Clean up wikis from the V1 table
		if(wikiPageDao != null && toDelete != null) {
			for(WikiPageKey id: toDelete) {
				wikiPageDao.delete(id);
			}
		}
		// Clean up file handles abandoned by V2 wikis during remigration
		if(abandonedFileHandleIds != null) {
			for(String id: abandonedFileHandleIds) {
				S3FileHandle markdownHandle = (S3FileHandle) fileMetadataDao.get(id);
				s3Client.deleteObject(markdownHandle.getBucketName(), markdownHandle.getKey());
				fileMetadataDao.delete(id);
			}
		}
		// If not the special parent case, we delete from "toDelete" which are wikis without hierarchy
		if(v2wikiPageDAO != null && toDelete != null && toDeleteForParentCase.size() == 0) {
			for(WikiPageKey id: toDelete) {
				V2WikiPage wiki = v2wikiPageDAO.get(id, null);
				String markdownHandleId = wiki.getMarkdownFileHandleId();
				S3FileHandle markdownHandle = (S3FileHandle) fileMetadataDao.get(markdownHandleId);
				s3Client.deleteObject(markdownHandle.getBucketName(), markdownHandle.getKey());
				fileMetadataDao.delete(markdownHandleId);
				v2wikiPageDAO.delete(id);
			}
		}
		// Delete in order to avoid deleting a parent before children have been cleaned up
		if(v2wikiPageDAO != null && toDeleteForParentCase != null) {
			for(int i = 0; i < toDeleteForParentCase.size(); i++) {
				WikiPageKey key = toDeleteForParentCase.get(i);
				V2WikiPage wiki = v2wikiPageDAO.get(key, null);
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

		// Re-migrate the same wiki page. No error should be thrown; should just skip.
		PaginatedResults<WikiMigrationResult> duplicateMigrationResults = wikiMigrationService.migrateSomeWikis(userName, 1, 0, "somePath");
		assertNotNull(duplicateMigrationResults);
		assertEquals(1, duplicateMigrationResults.getResults().size());
		assertEquals(WikiMigrationResultType.SKIPPED, duplicateMigrationResults.getResults().get(0).getResultType());
		// Size of the V2 DB should still remain 3.
		assertEquals(3, v2wikiPageDAO.getCount());	
	}
	
	@Test
	public void testRemigration() throws NotFoundException, IOException {
		// If there is a change in the wiki page that needs to be updated in the V2 table, we'll remigrate.
		// Otherwise, we skip the remigration of a wiki page.
		
		String userName = adminUserInfo.getIndividualGroup().getName();
		String ownerId = "syn1";
		ObjectType ownerType = ObjectType.ENTITY;
		// Create a V2 Wiki
		WikiPage wiki = new WikiPage();
		wiki.setId("5");
		wiki.setEtag("etag");
		wiki.setCreatedBy(creatorUserGroupId);
		wiki.setModifiedBy(creatorUserGroupId);
		wiki.setMarkdown("markdown1");
		wiki.setTitle("title1");			
		wiki.setAttachmentFileHandleIds(new ArrayList<String>());
		Map<String, FileHandle> fileNameToFileHandleMap = new HashMap<String, FileHandle>();
		wiki = wikiPageDao.create(wiki, fileNameToFileHandleMap, ownerId, ownerType);
		assertNotNull(wiki);
		
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, wiki.getId());
		toDelete.add(key);
		
		// Migrate successfully
		PaginatedResults<WikiMigrationResult> results = wikiMigrationService.migrateSomeWikis(userName, 1, 0, "somePath");
		assertNotNull(results);
		assertEquals(WikiMigrationResultType.SUCCESS, results.getResults().get(0).getResultType());
		
		// Store the markdown file handle created during migration to clean up
		V2WikiPage wikiCreatedFromMigration = v2wikiPageDAO.get(key, null);
		String firstMarkdownFileHandleId = wikiCreatedFromMigration.getMarkdownFileHandleId();
		abandonedFileHandleIds.add(firstMarkdownFileHandleId);
		
		// Update the wiki page in the V1 DB and make sure etag is changed
		wiki.setEtag("etag2");
		wiki.setTitle("title2");
		wiki = wikiPageDao.updateWikiPage(wiki, fileNameToFileHandleMap, ownerId, ownerType, false);
		// Migrate the wiki again; should return success after updating the V2 DB
		// The second markdown file handle created during remigration will be cleaned up when deleting the V2 wiki
		PaginatedResults<WikiMigrationResult> resultsAfterUpdate = wikiMigrationService.migrateSomeWikis(userName, 1, 0, "somePath");
		assertNotNull(resultsAfterUpdate);
		assertEquals(WikiMigrationResultType.SUCCESS, resultsAfterUpdate.getResults().get(0).getResultType());
		V2WikiPage retrievedFromV2 = v2wikiPageDAO.get(key, null);
		// V2 should show changes
		assertEquals("title2", retrievedFromV2.getTitle());
		
		// Migrate again without any changes
		PaginatedResults<WikiMigrationResult> resultsAfterNoUpdate = wikiMigrationService.migrateSomeWikis(userName, 1, 0, "somePath");
		assertNotNull(resultsAfterNoUpdate);
		// Since nothing changed, we should have just skipped this wiki during migration
		assertEquals(WikiMigrationResultType.SKIPPED, resultsAfterNoUpdate.getResults().get(0).getResultType());
		
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
		// NOTE: Took out the recursive migration of parents, and this test failed to migrate the PARENT successfully
		// greatAncestor, ancestor, and grandparent must be migrated first
		V2WikiPage parentResult = wikiMigrationService.migrate(parent, adminUserInfo);
		// By migrating parent, the V2 DB should hold four wiki pages
		assertEquals(4, v2wikiPageDAO.getCount());
		assertEquals(true, v2WikiPageMigrationDao.doesWikiExist(parent.getParentWikiId()));
		assertEquals(true, v2WikiPageMigrationDao.doesWikiExist(grandparent.getParentWikiId()));
		assertEquals(true, v2WikiPageMigrationDao.doesWikiExist(ancestor.getParentWikiId()));
		assertEquals(true, v2WikiPageMigrationDao.doesWikiExist(child.getParentWikiId()));
		
		V2WikiPage childResult = wikiMigrationService.migrate(child, adminUserInfo);
		assertEquals(5, v2wikiPageDAO.getCount());
		
		// Add keys to list of wikis to delete / start with child to avoid cascade delete
		toDeleteForParentCase.add(key5);
		toDeleteForParentCase.add(key4);
		toDeleteForParentCase.add(key3);
		toDeleteForParentCase.add(key2);
		toDeleteForParentCase.add(key);

		// Nothing to clean up in V1 wiki pages, so casacde deleting is okay
		toDelete.add(key);
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
