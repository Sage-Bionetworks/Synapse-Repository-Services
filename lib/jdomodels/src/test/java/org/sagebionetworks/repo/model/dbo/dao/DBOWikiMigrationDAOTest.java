package org.sagebionetworks.repo.model.dbo.dao;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOWikiMigrationDAOTest {
	@Autowired
	private DBOWikiMigrationDAO wikiMigrationDao;
	@Autowired
	private WikiPageDao wikiPageDao;
	@Autowired
	private V2WikiPageDao v2WikiPageDao;
	@Autowired
	private FileHandleDao fileMetadataDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	private List<WikiPageKey> toDeleteFromV1;
	private List<WikiPageKey> toDeleteFromV2;
	String creatorUserGroupId;
	
	S3FileHandle attachOne;
	S3FileHandle markdown;
	
	@Before
	public void before(){
		toDeleteFromV1 = new LinkedList<WikiPageKey>();
		toDeleteFromV2 = new LinkedList<WikiPageKey>();
		UserGroup userGroup = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false);
		assertNotNull(userGroup);
		creatorUserGroupId = userGroup.getId();
		assertNotNull(creatorUserGroupId);
		// We use a long file name to test the uniqueness constraint
		String longFileNamePrefix = "loooooooooooooooooooooooooooooooooooooonnnnnnnnnnnnnnnnnnnnnnnnnnnggggggggggggggggggggggggg";
		
		// Create a few files
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName(longFileNamePrefix+".txt1");
		meta = fileMetadataDao.createFile(meta);
		attachOne = meta;

		meta = new S3FileHandle();
		meta.setBucketName("bucketName2");
		meta.setKey("key2");
		meta.setContentType("content type2");
		meta.setContentSize(123l);
		meta.setContentMd5("md52");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName(longFileNamePrefix+".txt2");
		meta = fileMetadataDao.createFile(meta);
		markdown = meta;
	}
	
	@After
	public void after(){
		if(wikiPageDao != null && toDeleteFromV1 != null) {
			for(WikiPageKey id: toDeleteFromV1) {
				wikiPageDao.delete(id);
			}
		}
		if(v2WikiPageDao != null && toDeleteFromV2 != null){
			for(WikiPageKey id: toDeleteFromV2){
				v2WikiPageDao.delete(id);
			}
		}
		// Delete the file handles
		if(attachOne != null){
			fileMetadataDao.delete(attachOne.getId());
		}
		if(markdown != null){
			fileMetadataDao.delete(markdown.getId());
		}
	}
	
	@Test
	public void testGetWikiPages() throws NotFoundException {
		// create some wiki pages for the v1 table
		createWikiPages();
		// get total count of v1 table
		Long total = wikiMigrationDao.getTotalCount();
		assertEquals(total, new Long(10));
		List<WikiPage> wikis = new ArrayList<WikiPage>();
		// Get wiki page at each offset
		for(int i = 0; i < total; i++) {
			wikis.addAll(wikiMigrationDao.getWikiPages(1, i));
		}
		assertEquals(total, new Long(wikis.size()));
	}
	
	@Test
	public void testGetWikiPage() throws NotFoundException {
		createWikiPages();
		WikiPage pageWithId2 = wikiMigrationDao.getWikiPage("2");
		assertNotNull(pageWithId2);
		assertEquals("2", pageWithId2.getId());
		assertEquals("markdown2", pageWithId2.getMarkdown());
		assertEquals("title2", pageWithId2.getTitle());
		assertEquals(1, pageWithId2.getAttachmentFileHandleIds().size());
		assertEquals(creatorUserGroupId, pageWithId2.getCreatedBy());
	}
	
	@Test
	public void testMigrateWikis() throws NotFoundException {
		createWikiPages();
		// "Translate" a wiki page and migrate
		V2WikiPage page = new V2WikiPage();
		page.setId("1");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setCreatedOn(new Date(1));
		page.setModifiedOn(new Date(1));
		page.setMarkdownFileHandleId(markdown.getId());
		page.setTitle("title1");
		page.setEtag("etag");
		page.setAttachmentFileHandleIds(new ArrayList<String>());
		page.getAttachmentFileHandleIds().add(attachOne.getId());
		V2WikiPage result = wikiMigrationDao.migrateWiki(page);
		assertNotNull(result);
		WikiPageKey key = v2WikiPageDao.lookupWikiKey(result.getId());
		toDeleteFromV2.add(key);
		assertEquals(1, v2WikiPageDao.getCount());
		
		V2WikiPage retrieved = v2WikiPageDao.get(key, null);
		assertNotNull(retrieved);
		String etag = wikiMigrationDao.getV2WikiPageEtag(retrieved.getId());
		assertEquals("etag", etag);
	}
	
	private void createWikiPages() throws NotFoundException {
		for(int i = 1; i < 11; i++) {
			String ownerId = "syn" + i;
			ObjectType ownerType = ObjectType.ENTITY;
			
			WikiPage page = new WikiPage();
			page.setId("" + i);
			page.setCreatedBy(creatorUserGroupId);
			page.setModifiedBy(creatorUserGroupId);
			page.setMarkdown("markdown" + i);
			page.setTitle("title" + i);
			page.setAttachmentFileHandleIds(new ArrayList<String>());
			page.getAttachmentFileHandleIds().add(attachOne.getId());
			Map<String, FileHandle> fileNameToFileHandleMap = new HashMap<String, FileHandle>();
			fileNameToFileHandleMap.put(attachOne.getFileName(), fileMetadataDao.get(attachOne.getId()));
			WikiPage result = wikiPageDao.create(page, fileNameToFileHandleMap, ownerId, ownerType);
			WikiPageKey key = wikiPageDao.lookupWikiKey(result.getId());
			toDeleteFromV1.add(key);
		}
	}
}
