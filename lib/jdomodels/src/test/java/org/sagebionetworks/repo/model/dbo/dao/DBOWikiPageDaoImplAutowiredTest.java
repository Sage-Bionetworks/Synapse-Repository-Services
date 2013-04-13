package org.sagebionetworks.repo.model.dbo.dao;

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
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.backup.WikiPageBackup;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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
	FileHandleDao fileMetadataDao;
	
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
		fileOne = meta;
		// Two
		meta = new S3FileHandle();
		meta.setBucketName("bucketName2");
		meta.setKey("key2");
		meta.setContentType("content type2");
		meta.setContentSize(123l);
		meta.setContentMd5("md52");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName(longFileNamePrefix+".txt2");
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
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(fileOne.getFileName(), fileOne);
  		// Create it
		WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType);
		assertNotNull(clone);
		assertNotNull(clone.getId());
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);
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
		
		// Make sure the key matchs
		WikiPageKey lookupKey = wikiPageDao.lookupWikiKey(key.getWikiPageId());
		assertEquals(key, lookupKey);
	}
	
	@Test (expected=NotFoundException.class)
	public void testLookupWikiKeyNotFound() throws NotFoundException{
		wikiPageDao.lookupWikiKey("-123");
	}
	
	@Test
	public void testUpdate() throws NotFoundException, InterruptedException{
		WikiPage page = new WikiPage();
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdown("This is the markdown text");
		String ownerId = "456";
		ObjectType ownerType = ObjectType.EVALUATION;
		// Add an attachment
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(fileOne.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(fileOne.getFileName(),fileOne);
		// Create it
		WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType);
		assertNotNull(clone);
		toDelete.add(new WikiPageKey(ownerId, ownerType, clone.getId()));
		String startEtag = clone.getEtag();
		Long startModifiedOn = clone.getModifiedOn().getTime();
		// Sleep to ensure the next date is higher.
		Thread.sleep(1000);
		// Add another attachment to the list.
		clone.getAttachmentFileHandleIds().add(fileTwo.getId());
		clone.setTitle("Updated title");
		fileNameMap.put(fileTwo.getFileName(), fileTwo);
		// Update
		WikiPage clone2 = wikiPageDao.updateWikiPage(clone, fileNameMap, ownerId, ownerType, true);
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
		root = wikiPageDao.create(root, new HashMap<String, FileHandle>(), ownerId, ownerType);
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
			child = wikiPageDao.create(child, new HashMap<String, FileHandle>(), ownerId, ownerType);
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
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(fileOne.getFileName(), fileOne);
		String ownerId = "syn123";
		ObjectType ownerType = ObjectType.ENTITY;
		// Create it
		root = wikiPageDao.create(root, fileNameMap, ownerId, ownerType);
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
		Map<String, FileHandle> childFileNameMap = new HashMap<String, FileHandle>();
		childFileNameMap.put(fileTwo.getFileName(), fileTwo);
		childFileNameMap.put(fileOne.getFileName(), fileOne);
		child = wikiPageDao.create(child, childFileNameMap, ownerId, ownerType);
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
	
	@Test
	public void testgetWikiAttachmentFileHandleForFileName() throws Exception{
		String ownerId = "syn123";
		ObjectType ownerType = ObjectType.ENTITY;
		WikiPage root = new WikiPage();
		root.setTitle("Root");
		root.setCreatedBy(creatorUserGroupId);
		root.setModifiedBy(creatorUserGroupId);
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(fileOne.getFileName(), fileOne);
		fileNameMap.put(fileTwo.getFileName(), fileTwo);
		root = wikiPageDao.create(root, fileNameMap, ownerId, ownerType);
		assertNotNull(root);
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, root.getId());
		toDelete.add(key);
		// Now lookup each file using its name.
		String id = wikiPageDao.getWikiAttachmentFileHandleForFileName(key, fileOne.getFileName());
		assertEquals(fileOne.getId(), id);
		// The second
		id = wikiPageDao.getWikiAttachmentFileHandleForFileName(key, fileTwo.getFileName());
		assertEquals(fileTwo.getId(), id);
		// Test the not found case
		try{
			wikiPageDao.getWikiAttachmentFileHandleForFileName(key, fileTwo.getFileName()+"1");
			fail("The file name does not exist and should have failed");
		}catch(NotFoundException e){
			// expected
		}
	}
	
	@Test
	public void testGetMigrationObjectData() throws NotFoundException{
		long startCount = wikiPageDao.getCount();
		// Add some wiki hierarchy
		String ownerId = "syn123";
		ObjectType ownerType = ObjectType.ENTITY;
		WikiPage root = new WikiPage();
		root.setTitle("Root");
		root.setCreatedBy(creatorUserGroupId);
		root.setModifiedBy(creatorUserGroupId);
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(fileOne.getFileName(), fileOne);
		fileNameMap.put(fileTwo.getFileName(), fileTwo);
		root = wikiPageDao.create(root, fileNameMap, ownerId, ownerType);
		assertNotNull(root);
		WikiPageKey rootKey = new WikiPageKey(KeyFactory.stringToKey(ownerId).toString(), ownerType, root.getId());
		toDelete.add(rootKey);
		// Add a child
		WikiPage child = new WikiPage();
		child.setTitle("child");
		child.setCreatedBy(creatorUserGroupId);
		child.setModifiedBy(creatorUserGroupId);
		child.setParentWikiId(root.getId());
		child = wikiPageDao.create(child,  new HashMap<String, FileHandle>(), ownerId, ownerType);
		assertNotNull(root);
		WikiPageKey childKey = new WikiPageKey(KeyFactory.stringToKey(ownerId).toString(), ownerType, child.getId());
		toDelete.add(childKey);
		
		// the current count
		long currentCount = wikiPageDao.getCount();
		assertTrue(startCount +2l == currentCount);
		// Now get all pages
		QueryResults<MigratableObjectData> results = wikiPageDao.getMigrationObjectData(startCount, Long.MAX_VALUE, true);
		System.out.println(results);
		assertNotNull(results);
		assertNotNull(results.getResults());
		assertEquals(currentCount, results.getTotalNumberOfResults());
		assertEquals(2l, results.getResults().size());
		// the results must be sorted by ID.
		MigratableObjectData mod = results.getResults().get(0);
		assertEquals(rootKey.getKeyString(),  mod.getId().getId());
		assertEquals(root.getEtag(),  mod.getEtag());
		// The Root page should have no dependencies.
		assertNotNull(mod.getDependencies());
		assertEquals(0, mod.getDependencies().size());
		// next item
		mod = results.getResults().get(1);
		assertEquals(childKey.getKeyString(),  mod.getId().getId());
		assertEquals(child.getEtag(),  mod.getEtag());
		// This page should depend on its parent.
		assertNotNull(mod.getDependencies());
		assertEquals(1, mod.getDependencies().size());
		MigratableObjectDescriptor dependancy = mod.getDependencies().iterator().next();
		assertNotNull(dependancy);
		assertEquals(rootKey.getKeyString(), dependancy.getId());
		assertEquals(MigratableObjectType.WIKIPAGE, dependancy.getType());
		
		// Test paging
		// Only select the second to last.
		results = wikiPageDao.getMigrationObjectData(startCount+1, 1, true);
		System.out.println(results);
		assertNotNull(results);
		assertNotNull(results.getResults());
		assertEquals(currentCount, results.getTotalNumberOfResults());
		assertEquals(1l, results.getResults().size());
		assertEquals(childKey.getKeyString(),  results.getResults().get(0).getId().getId());
		assertEquals(child.getEtag(),  results.getResults().get(0).getEtag());
		
		// Check the type
		assertEquals(MigratableObjectType.WIKIPAGE, wikiPageDao.getMigratableObjectType());
		
	}
	
	@Test
	public void testGetWikiPageBackupRoundTrip() throws NotFoundException{
		// Add some wiki hierarchy
		String ownerId = "syn123";
		ObjectType ownerType = ObjectType.ENTITY;
		WikiPage root = new WikiPage();
		root.setTitle("Root");
		root.setCreatedBy(creatorUserGroupId);
		root.setModifiedBy(creatorUserGroupId);
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(fileOne.getFileName(), fileOne);
		fileNameMap.put(fileTwo.getFileName(), fileTwo);
		root = wikiPageDao.create(root, fileNameMap, ownerId, ownerType);
		assertNotNull(root);
		WikiPageKey rootKey = new WikiPageKey(KeyFactory.stringToKey(ownerId).toString(), ownerType, root.getId());
		toDelete.add(rootKey);
		// Add a child
		WikiPage child = new WikiPage();
		child.setTitle("child");
		child.setCreatedBy(creatorUserGroupId);
		child.setModifiedBy(creatorUserGroupId);
		child.setParentWikiId(root.getId());
		child = wikiPageDao.create(child,  new HashMap<String, FileHandle>(), ownerId, ownerType);
		assertNotNull(root);
		WikiPageKey childKey = new WikiPageKey(KeyFactory.stringToKey(ownerId).toString(), ownerType, child.getId());
		toDelete.add(childKey);
		
		// Capture the backup of each object.
		WikiPageBackup rootBackup = wikiPageDao.getWikiPageBackup(rootKey);
		assertNotNull(rootBackup);
		WikiPageBackup childBackup = wikiPageDao.getWikiPageBackup(childKey);
		assertNotNull(childBackup);
		// Now delete both
		wikiPageDao.delete(rootKey);
		wikiPageDao.delete(childKey);
		
		// restore both from a backup
		WikiPageKey rootCloneKey = wikiPageDao.createOrUpdateFromBackup(rootBackup);
		assertEquals(rootKey, rootCloneKey);
		WikiPageKey childCloneKey = wikiPageDao.createOrUpdateFromBackup(childBackup);
		assertEquals(childKey, childCloneKey);
		// Get clones and make sure they match
		WikiPage rootClone = wikiPageDao.get(rootCloneKey);
		assertEquals(root, rootClone);
		WikiPage childClone = wikiPageDao.get(childCloneKey);
		assertEquals(child, childClone);
		
		// We should be able to update using the backups
		rootCloneKey = wikiPageDao.createOrUpdateFromBackup(rootBackup);
		rootClone = wikiPageDao.get(rootCloneKey);
		assertEquals(root, rootClone);
		
		//update the child
		childCloneKey = wikiPageDao.createOrUpdateFromBackup(childBackup);
		childClone = wikiPageDao.get(childCloneKey);
		assertEquals(child, childClone);
		
	}
}
