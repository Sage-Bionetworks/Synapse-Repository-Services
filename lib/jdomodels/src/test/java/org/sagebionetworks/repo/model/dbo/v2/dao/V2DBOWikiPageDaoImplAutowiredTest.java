package org.sagebionetworks.repo.model.dbo.v2.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class V2DBOWikiPageDaoImplAutowiredTest {

	@Autowired
	FileHandleDao fileMetadataDao;
	
	@Autowired
	V2WikiPageDao wikiPageDao;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	private List<WikiPageKey> toDelete;
	String creatorUserGroupId;
	
	S3FileHandle attachOne;
	S3FileHandle attachTwo;
	S3FileHandle markdownOne;
	S3FileHandle markdownTwo;
	
	@Before
	public void before(){
		toDelete = new LinkedList<WikiPageKey>();
		creatorUserGroupId = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
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
		attachTwo = meta;
		
		//Create different markdown content
		meta = new S3FileHandle();
		meta.setBucketName("markdownBucketName");
		meta.setKey("key3");
		meta.setContentType("content type3");
		meta.setContentSize((long) 1231);
		meta.setContentMd5("md53");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("markdown1");
		markdownOne = fileMetadataDao.createFile(meta);
		
		meta = new S3FileHandle();
		meta.setBucketName("markdownBucketName2");
		meta.setKey("key4");
		meta.setContentType("content type4");
		meta.setContentSize((long) 1231);
		meta.setContentMd5("md54");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("markdown2");
		markdownTwo = fileMetadataDao.createFile(meta);
	}
	
	/**
	 * Create a new wiki page.
	 * @throws NotFoundException 
	 */
	@Test
	public void testCreate() throws NotFoundException{
		// Create a new wiki page with a single attachment
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn181";
		ObjectType ownerType = ObjectType.ENTITY;
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdownOne.getId());
		
		// Add an attachment
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(attachOne.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(attachOne.getFileName(), attachOne);
		
		// Create it
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType);
		assertNotNull(clone);
		assertNotNull(clone.getId());
		
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);
		assertNotNull("createdOn date should have been filled in by the DB", clone.getCreatedOn());
		assertNotNull("modifiedOn date should have been filled in by the DB", clone.getModifiedOn());
		assertNotNull(clone.getEtag());
		assertNotNull(clone.getId());
		
		assertEquals(creatorUserGroupId, clone.getCreatedBy());
		assertEquals(creatorUserGroupId, clone.getModifiedBy());
		assertEquals(page.getTitle(), clone.getTitle());
		assertEquals(null, clone.getParentWikiId());
		// Markdown file handle id and attachment ids should not change
		// They are retrieved from the markdown table and attachment reservation table
		assertEquals(page.getMarkdownFileHandleId(), clone.getMarkdownFileHandleId());
		assertEquals(page.getAttachmentFileHandleIds(), clone.getAttachmentFileHandleIds());
		// There should only be one entry in the history of this wiki
		List<V2WikiPage> history = wikiPageDao.getWikiHistory(key);
		assertEquals(history.size(), 1);
		
		// Make sure we can lock
		String etag = wikiPageDao.lockForUpdate(clone.getId());
		assertNotNull(etag);
		assertEquals(clone.getEtag(), etag);
		
		// Make sure the key matches
		WikiPageKey lookupKey = wikiPageDao.lookupWikiKey(key.getWikiPageId());
		assertEquals(key, lookupKey);
	}
	
	/**
	 * Create and update a wiki page. Restore an older version and 
	 * confirm wiki history is accurate
	 * @throws NotFoundException, InterruptedException
	 */
	@Test
	public void testUpdateAndRestore() throws NotFoundException, InterruptedException{
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn1081";
		ObjectType ownerType = ObjectType.EVALUATION;
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdownOne.getId());
		
		// Add an attachment
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(attachOne.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(attachOne.getFileName(), attachOne);
		
		// Create it
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType);
		assertNotNull(clone);

		WikiPageKey key = new WikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);
		String startEtag = clone.getEtag();
		Long startModifiedOn = clone.getModifiedOn().getTime();
		// Sleep to ensure the next date is higher.
		Thread.sleep(1000);
		
		// Add another attachment to the list and update markdown filehandle id to another markdown
		clone.getAttachmentFileHandleIds().add(attachTwo.getId());
		clone.setTitle("Updated title");
		clone.setMarkdownFileHandleId(markdownTwo.getId());
		fileNameMap.put(attachTwo.getFileName(), attachTwo);
		
		// Update
		V2WikiPage clone2 = wikiPageDao.updateWikiPage(clone, fileNameMap, ownerId, ownerType, true);
		assertNotNull(clone2);
		assertNotNull(clone2.getEtag());
		// The etag should be new
		assertFalse("The etag should have changed",!startEtag.equals(clone2.getEtag()));
		
		Long endModifiedOn = clone2.getModifiedOn().getTime();
		assertTrue("Modified On should have change and be greater than the previous timestamp", endModifiedOn > startModifiedOn);
		
		// Make sure the edited markdown, title, and attachments are correct
		assertEquals(clone.getMarkdownFileHandleId(), clone2.getMarkdownFileHandleId());
		assertEquals(clone.getTitle(), clone2.getTitle());
		assertEquals(clone.getAttachmentFileHandleIds(), clone2.getAttachmentFileHandleIds());

		// At this point, the markdown database has two versions for this wiki page
		// Make sure history support is accurate
		List<V2WikiPage> history = wikiPageDao.getWikiHistory(key);
		assertTrue(history.size() == 2);
		
		// history.get(0) is the most recent snapshot (the recently updated page)
		// history.get(1) older snapshot
		V2WikiPage oldWikiVersion = history.get(1);
		V2WikiPage newWikiVersion = history.get(0);
		assertTrue(newWikiVersion.getMarkdownFileHandleId() != oldWikiVersion.getMarkdownFileHandleId());
		assertTrue(newWikiVersion.getAttachmentFileHandleIds() != oldWikiVersion.getAttachmentFileHandleIds());
		assertTrue(newWikiVersion.getModifiedOn().getTime() > oldWikiVersion.getModifiedOn().getTime());
		
		// To restore wiki to a previous version, update again
		fileNameMap.remove(attachTwo.getFileName());
		V2WikiPage restored = wikiPageDao.updateWikiPage(oldWikiVersion, fileNameMap, ownerId, ownerType, false);
		assertEquals(oldWikiVersion.getMarkdownFileHandleId(), restored.getMarkdownFileHandleId());
		assertEquals(oldWikiVersion.getAttachmentFileHandleIds(), restored.getAttachmentFileHandleIds());
		
		// At this point, the restored wiki is the third version of the markdown/attachments
		List<V2WikiPage> historyAfterRestoration = wikiPageDao.getWikiHistory(key);
		assertTrue(historyAfterRestoration.size() == 3);
	}
	
	/**
	 * Create children wiki pages
	 * @throws NotFoundException 
	 */
	@Test
	public void testCreateChildPage() throws NotFoundException{
		V2WikiPage root = new V2WikiPage();
		String ownerId = "syn2081";
		ObjectType ownerType = ObjectType.ENTITY;
		root.setTitle("Root");
		root.setCreatedBy(creatorUserGroupId);
		root.setModifiedBy(creatorUserGroupId);
		root.setMarkdownFileHandleId(markdownOne.getId());
		
		// Create it
		root = wikiPageDao.create(root, new HashMap<String, FileHandle>(), ownerId, ownerType);
		assertNotNull(root);
		WikiPageKey rootKey = new WikiPageKey(ownerId, ownerType, root.getId());
		toDelete.add(rootKey);
		
		// Add add children in reverse alphabetical order.
		int childCount = 3;
		List<V2WikiPage> children = new LinkedList<V2WikiPage>();
		for(int i=childCount-1; i>-1; i--){
			V2WikiPage child = new V2WikiPage();
			child.setTitle("A"+i);
			child.setCreatedBy(creatorUserGroupId);
			child.setModifiedBy(creatorUserGroupId);
			child.setParentWikiId(root.getId());
			child.setMarkdownFileHandleId(markdownOne.getId());
			child = wikiPageDao.create(child, new HashMap<String, FileHandle>(), ownerId, ownerType);
			children.add(child);
		}
		
		// Now get the children of this parent
		List<V2WikiHeader> list = wikiPageDao.getHeaderTree(ownerId, ownerType);
		System.out.println(list);
		assertNotNull(list);
		assertEquals(childCount+1, list.size());
		
		// Check order; the parent should be first
		assertEquals("Root", list.get(0).getTitle());
		assertEquals("A0", list.get(1).getTitle());
		assertEquals("A"+(childCount-1), list.get(childCount).getTitle());
		
		// Check cascade delete
		wikiPageDao.delete(rootKey);
		for(V2WikiPage childWiki: children){
			try{
				wikiPageDao.get(new WikiPageKey(ownerId, ownerType, childWiki.getId()));
				fail("This child should have been deleted when the parent was deleted.");
			}catch(NotFoundException e){
				// expected
			}
		}
	}
	
	@Test
	public void testDelete() throws NotFoundException, InterruptedException{
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn2081";
		ObjectType ownerType = ObjectType.EVALUATION;
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdownOne.getId());
		
		// Add an attachment
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(attachOne.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(attachOne.getFileName(), attachOne);
		
		// Create it
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType);
		assertNotNull(clone);
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);
		
		Thread.sleep(1000);
		
		// Add another attachment to the list and update markdown filehandle id to another markdown
		clone.getAttachmentFileHandleIds().add(attachTwo.getId());
		clone.setTitle("Updated title");
		clone.setMarkdownFileHandleId(markdownTwo.getId());
		fileNameMap.put(attachTwo.getFileName(), attachTwo);
		
		// Update
		V2WikiPage clone2 = wikiPageDao.updateWikiPage(clone, fileNameMap, ownerId, ownerType, true);
		assertNotNull(clone2);
		assertNotNull(clone2.getEtag());
	
		// Check cascade delete
		wikiPageDao.delete(key);
		try {
			wikiPageDao.getWikiHistory(key);
			fail("Versions of this wiki's content should have been deleted when the wiki was deleted.");
		} catch(NotFoundException e) {
			// expected
		}
	}
	
	/**
	 * Gets a list of a wiki page's file handle ids
	 * @throws NotFoundException 
	 */
	@Test
	public void testGetWikiFileHandleIds() throws NotFoundException{
		V2WikiPage root = new V2WikiPage();
		String ownerId = "syn3081";
		ObjectType ownerType = ObjectType.ENTITY;
		root.setTitle("Root");
		root.setCreatedBy(creatorUserGroupId);
		root.setModifiedBy(creatorUserGroupId);
		root.setMarkdownFileHandleId(markdownOne.getId());
		root.setAttachmentFileHandleIds(new LinkedList<String>());
		// Add a file handle to the root.
		root.getAttachmentFileHandleIds().add(attachOne.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(attachOne.getFileName(), attachOne);
		// Create it
		root = wikiPageDao.create(root, fileNameMap, ownerId, ownerType);
		assertNotNull(root);
		WikiPageKey rootKey = new WikiPageKey(ownerId, ownerType, root.getId());
		toDelete.add(rootKey);
		
		// Add a child.
		V2WikiPage child = new V2WikiPage();
		child.setTitle("Child");
		child.setCreatedBy(creatorUserGroupId);
		child.setModifiedBy(creatorUserGroupId);
		child.setMarkdownFileHandleId(markdownOne.getId());
		child.setParentWikiId(root.getId());
		// Add  file handle to the child.
		child.setAttachmentFileHandleIds(new LinkedList<String>());
		child.getAttachmentFileHandleIds().add(attachTwo.getId());
		child.getAttachmentFileHandleIds().add(attachOne.getId());
		Map<String, FileHandle> childFileNameMap = new HashMap<String, FileHandle>();
		childFileNameMap.put(attachTwo.getFileName(), attachTwo);
		childFileNameMap.put(attachOne.getFileName(), attachOne);
		child = wikiPageDao.create(child, childFileNameMap, ownerId, ownerType);
		WikiPageKey childKey = new WikiPageKey(ownerId, ownerType, child.getId());
		
		// Now get the FileHandleIds of each
		List<String> handleList = wikiPageDao.getWikiFileHandleIds(rootKey);
		assertNotNull(handleList);
		assertEquals(1, handleList.size());
		assertEquals(attachOne.getId(), handleList.get(0));
		
		// Test the child
		handleList = wikiPageDao.getWikiFileHandleIds(childKey);
		assertNotNull(handleList);
		assertEquals(2, handleList.size());
		assertTrue(handleList.contains(attachOne.getId()));
		assertTrue(handleList.contains(attachTwo.getId()));
	}
	
	/**
	 * Looks up file handles for wiki pages by name
	 * @throws Exception
	 */
	@Test
	public void testgetWikiAttachmentFileHandleForFileName() throws Exception{
		V2WikiPage root = new V2WikiPage();
		String ownerId = "syn4081";
		ObjectType ownerType = ObjectType.ENTITY;
		root.setTitle("Root");
		root.setCreatedBy(creatorUserGroupId);
		root.setModifiedBy(creatorUserGroupId);
		root.setMarkdownFileHandleId(markdownOne.getId());
		root.setAttachmentFileHandleIds(new LinkedList<String>());
		root.getAttachmentFileHandleIds().add(attachOne.getId());
		root.getAttachmentFileHandleIds().add(attachTwo.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(attachOne.getFileName(), attachOne);
		fileNameMap.put(attachTwo.getFileName(), attachTwo);
		root = wikiPageDao.create(root, fileNameMap, ownerId, ownerType);
		assertNotNull(root);
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, root.getId());
		toDelete.add(key);
		
		// Now lookup each file using its name.
		String id = wikiPageDao.getWikiAttachmentFileHandleForFileName(key, attachOne.getFileName());
		assertEquals(attachOne.getId(), id);
		// The second
		id = wikiPageDao.getWikiAttachmentFileHandleForFileName(key, attachTwo.getFileName());
		assertEquals(attachTwo.getId(), id);
		
		// Test the not found case
		try{
			wikiPageDao.getWikiAttachmentFileHandleForFileName(key, attachTwo.getFileName()+"1");
			fail("The file name does not exist and should have failed");
		}catch(NotFoundException e){
			// expected
		}
	}
}
