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
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
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
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.WikiModelTranslationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
	private UserGroupDAO userGroupDAO;
	@Autowired
	private FileHandleDao fileMetadataDao;

	String creatorUserGroupId;
	List<WikiPageKey> toDelete;
	S3FileHandle markdown;
	
	@Before
	public void before() throws NotFoundException {
		toDelete = new ArrayList<WikiPageKey>(); 
		
		UserGroup userGroup = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false);
		assertNotNull(userGroup);
		creatorUserGroupId = userGroup.getId();
		assertNotNull(creatorUserGroupId);
		
		// Create a few files
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("file.txt1");
		meta = fileMetadataDao.createFile(meta);
		markdown = meta;
	}
	
	@After
	public void after() {
		// Clean up wikis from both v1 and v2 tables
		if(wikiPageDao != null && toDelete != null) {
			for(WikiPageKey id: toDelete) {
				wikiPageDao.delete(id);
			}
		}
		if(v2wikiPageDAO != null && toDelete != null) {
			for(WikiPageKey id: toDelete) {
				v2wikiPageDAO.delete(id);
			}
		}
		// Delete the file handles
		if(markdown != null){
			fileMetadataDao.delete(markdown.getId());
		}
	}
	
	@Test
	public void testMigrateSomeWikis() throws NotFoundException, IOException {
		// Create some wiki pages
		createWikiPages(1, 3);
	
		// Migrate some wiki pages
		PaginatedResults<WikiMigrationResult> results = wikiMigrationService.migrateSomeWikis(AuthorizationConstants.ADMIN_USER_NAME, 3, 0, "somePath");
		assertNotNull(results);
		// Limit of 3 should have been returned
		assertEquals(3, results.getResults().size());
		// Size of v2 DB should be equal to the number of wikis successfully migrated
		assertEquals(3, v2wikiPageDAO.getCount());		
	}
	
	@Test
	public void testMigrateSomeWikisWithError() throws NotFoundException, IOException {
		// Create an error by inserting a wiki with the same id into the V2 DB before migration
		String ownerId = "syn1";
		ObjectType ownerType = ObjectType.ENTITY;
		
		V2WikiPage page = new V2WikiPage();
		page.setId("1");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdown.getId());
		page.setTitle("title1");
		page.setAttachmentFileHandleIds(new ArrayList<String>());
		Map<String, FileHandle> fileNameToFileHandleMap = new HashMap<String, FileHandle>();
		V2WikiPage result = v2wikiPageDAO.create(page, fileNameToFileHandleMap, ownerId, ownerType, new ArrayList<String>());
		assertNotNull(result);
		WikiPageKey key = v2wikiPageDAO.lookupWikiKey(result.getId());
		toDelete.add(key);
		assertEquals(1, v2wikiPageDAO.getCount());
		
		createWikiPages(1, 1);
		PaginatedResults<WikiMigrationResult> results = wikiMigrationService.migrateSomeWikis(AuthorizationConstants.ADMIN_USER_NAME, 1, 0, "somePath");
		assertNotNull(results);
		// Limit of 1 should have been returned
		assertEquals(1, results.getResults().size());
		assertEquals(WikiMigrationResultType.FAILURE, results.getResults().get(0).getResultType());
		// Size of v2 DB should still be 1 because nothing was migrated
		assertEquals(1, v2wikiPageDAO.getCount());		
	}
	
	private void createWikiPages(int start, int end) throws NotFoundException {
		for(int i = start; i <= end; i++) {
			String ownerId = "syn" + i;
			ObjectType ownerType = ObjectType.ENTITY;
			
			WikiPage page = new WikiPage();
			page.setId("" + i);
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
