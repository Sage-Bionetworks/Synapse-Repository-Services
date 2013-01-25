package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOWikiPageDaoImplAutowiredTest {

	@Autowired
	FileMetadataDao fileMetadataDao;
	
	@Autowired
	WikiPageDao wikiPageDao;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	private List<WikiPageKey> toDelete;
	String creatorUserGroupId;
	
	S3FileHandle fileOne;
	S3FileHandle fileTwo;
	
	@Before
	public void before(){
		toDelete = new LinkedList<WikiPageKey>();
		creatorUserGroupId = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		assertNotNull(creatorUserGroupId);
		// Create a few files
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("foobar.txt");
		meta = fileMetadataDao.createFile(meta);
		fileOne = meta;
		// Two
		meta = new S3FileHandle();
		meta.setBucketName("bucketName2");
		meta.setKey("key2");
		meta.setContentType("content type2");
		meta.setContentSize(123l);
		meta.setContentMd5("md52");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("foobar2.txt");
		meta = fileMetadataDao.createFile(meta);
		fileTwo = meta;
	}
	
	@After
	public void after(){
		if(wikiPageDao != null && toDelete != null){
			for(WikiPageKey id: toDelete){
				wikiPageDao.delete(id);
			}
		}
		// Delete the file handles
		if(fileOne != null){
			fileMetadataDao.delete(fileOne.getId());
		}
		if(fileTwo != null){
			fileMetadataDao.delete(fileTwo.getId());
		}
	}
	
	/**
	 * Create a new wiki page.
	 * @throws NotFoundException 
	 */
	@Test
	public void testCreate() throws NotFoundException{
		// Create a new wiki page with a single attachment
		WikiPage page = new WikiPage();
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdown("This is the markdown text");
		String ownerId = "syn123";
		ObjectType ownerType = ObjectType.ENTITY;
		// Add an attachment
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(fileOne.getId());
		// Create it
		WikiPage clone = wikiPageDao.create(page, ownerId, ownerType);
		assertNotNull(clone);
		assertNotNull(clone.getId());
		toDelete.add(new WikiPageKey(ownerId, ownerType, clone.getId()));
		assertNotNull("createdOn date should have been filled in by the DB", clone.getCreatedOn());
		assertNotNull("modifiedOn date should have been filled in by the DB", clone.getModifiedOn());
		assertNotNull(clone.getEtag());
		assertEquals(creatorUserGroupId, clone.getCreatedBy());
		assertEquals(creatorUserGroupId, clone.getModifiedBy());
		assertEquals(page.getTitle(), clone.getTitle());
		assertEquals(page.getMarkdown(), clone.getMarkdown());
		assertEquals(null, clone.getParentWikiId());
		// The attachments should be equals
		assertEquals(page.getAttachmentFileHandleIds(), clone.getAttachmentFileHandleIds());
		
		// Make sure we can lock
		String etag = wikiPageDao.lockForUpdate(clone.getId());
		assertNotNull(etag);
		assertEquals(clone.getEtag(), etag);
	}
	
	@Test
	public void testUpdate() throws NotFoundException, InterruptedException{
		WikiPage page = new WikiPage();
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdown("This is the markdown text");
		String ownerId = "456";
		ObjectType ownerType = ObjectType.COMPETITION;
		// Add an attachment
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(fileOne.getId());
		// Create it
		WikiPage clone = wikiPageDao.create(page, ownerId, ownerType);
		assertNotNull(clone);
		toDelete.add(new WikiPageKey(ownerId, ownerType, clone.getId()));
		String startEtag = clone.getEtag();
		Long startModifiedOn = clone.getModifiedOn().getTime();
		// Sleep to ensure the next date is higher.
		Thread.sleep(1000);
		// Add another attachment to the list.
		clone.getAttachmentFileHandleIds().add(fileTwo.getId());
		clone.setTitle("Updated title");
		// Update
		WikiPage clone2 = wikiPageDao.updateWikiPage(clone,ownerId, ownerType, true);
		assertNotNull(clone2);
		assertNotNull(clone2.getEtag());
		// The etag should be new
		assertFalse("The etag should have changed",!startEtag.equals(clone2.getEtag()));
		Long endModifiedOn = clone2.getModifiedOn().getTime();
		assertTrue("Modified On should have change and be greater than the previous timestamp", endModifiedOn > startModifiedOn);
		// Make sure the attachments are correct
		assertEquals(clone.getAttachmentFileHandleIds(), clone2.getAttachmentFileHandleIds());
		// The title should be updated.
		assertEquals(clone.getTitle(), clone2.getTitle());
	}
	
	@Test
	public void testCreateChildPage() throws NotFoundException{
		WikiPage root = new WikiPage();
		root.setTitle("Root");
		root.setCreatedBy(creatorUserGroupId);
		root.setModifiedBy(creatorUserGroupId);
		String ownerId = "syn123";
		ObjectType ownerType = ObjectType.ENTITY;
		// Create it
		root = wikiPageDao.create(root, ownerId, ownerType);
		assertNotNull(root);
		WikiPageKey rootKey = new WikiPageKey(ownerId, ownerType, root.getId());
		toDelete.add(rootKey);
		// Add add children
		int childCount = 3;
		List<WikiPage> children = new LinkedList<WikiPage>();
		// Add the children in reverse alphabetical order.
		for(int i=childCount-1; i>-1; i--){
			WikiPage child = new WikiPage();
			child.setTitle("A"+i);
			child.setCreatedBy(creatorUserGroupId);
			child.setModifiedBy(creatorUserGroupId);
			child.setParentWikiId(root.getId());
			child = wikiPageDao.create(child, ownerId, ownerType);
			children.add(child);
		}
		// Now get the children of this parent
		List<WikiHeader> list = wikiPageDao.getHeaderTree(ownerId, ownerType);
		System.out.println(list);
		assertNotNull(list);
		assertEquals(childCount+1, list.size());
		// The parent should be first
		assertEquals("Root", list.get(0).getTitle());
		// Check the order
		assertEquals("A0", list.get(1).getTitle());
		assertEquals("A"+(childCount-1), list.get(childCount).getTitle());
		// Check cascade delete
		wikiPageDao.delete(rootKey);
		for(WikiPage childWiki: children){
			try{
				wikiPageDao.get(new WikiPageKey(ownerId, ownerType, childWiki.getId()));
				fail("This child should have been deleted when the parent was deleted.");
			}catch(NotFoundException e){
				// expected
			}
		}
	}
	
	@Test
	public void testGetWikiFileHandleIds() throws NotFoundException{
		WikiPage root = new WikiPage();
		root.setTitle("Root");
		root.setCreatedBy(creatorUserGroupId);
		root.setModifiedBy(creatorUserGroupId);
		root.setAttachmentFileHandleIds(new LinkedList<String>());
		// add  file handle to the root.
		root.getAttachmentFileHandleIds().add(fileOne.getId());
		String ownerId = "syn123";
		ObjectType ownerType = ObjectType.ENTITY;
		// Create it
		root = wikiPageDao.create(root, ownerId, ownerType);
		assertNotNull(root);
		WikiPageKey rootKey = new WikiPageKey(ownerId, ownerType, root.getId());
		toDelete.add(rootKey);
		// Add add children
		// Add a child.
		WikiPage child = new WikiPage();
		child.setTitle("Child");
		child.setCreatedBy(creatorUserGroupId);
		child.setModifiedBy(creatorUserGroupId);
		child.setParentWikiId(root.getId());
		// add  file handle to the child.
		child.setAttachmentFileHandleIds(new LinkedList<String>());
		child.getAttachmentFileHandleIds().add(fileTwo.getId());
		child.getAttachmentFileHandleIds().add(fileOne.getId());
		child = wikiPageDao.create(child, ownerId, ownerType);
		WikiPageKey childKey = new WikiPageKey(ownerId, ownerType, child.getId());
		// Now get the FileHandleIds of each
		List<String> handleList = wikiPageDao.getWikiFileHandleIds(rootKey);
		assertNotNull(handleList);
		assertEquals(1, handleList.size());
		assertEquals(fileOne.getId(), handleList.get(0));
		// Test the child
		handleList = wikiPageDao.getWikiFileHandleIds(childKey);
		assertNotNull(handleList);
		assertEquals(2, handleList.size());
		assertEquals(fileOne.getId(), handleList.get(0));
		assertEquals(fileTwo.getId(), handleList.get(1));
	}
}
