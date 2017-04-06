package org.sagebionetworks.repo.model.dbo.wikiV2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ROOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_PAGE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiMarkdownVersion;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class V2DBOWikiPageDaoImplAutowiredTest {

	private static final String SQL_GET_ROOT_ID = "SELECT "+V2_COL_WIKI_ROOT_ID+" FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ?";

	@Autowired
	private FileHandleDao fileMetadataDao;
	
	@Autowired
	private V2WikiPageDao wikiPageDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	private List<WikiPageKey> toDelete;
	private String creatorUserGroupId;
	
	private S3FileHandle attachOne;
	private S3FileHandle attachTwo;
	private S3FileHandle markdownOne;
	private S3FileHandle markdownTwo;
	
	@After
	public void after(){
		if(wikiPageDao != null && toDelete != null){
			for(WikiPageKey id: toDelete){
				wikiPageDao.delete(id);
			}
		}
		// Delete the file handles
		if(attachOne != null){
			fileMetadataDao.delete(attachOne.getId());
		}
		if(attachTwo != null){
			fileMetadataDao.delete(attachTwo.getId());
		}
		
		if(markdownOne != null) {
			fileMetadataDao.delete(markdownOne.getId());
		}
		
		if(markdownTwo != null) {
			fileMetadataDao.delete(markdownTwo.getId());
		}
	}
	
	@Before
	public void before(){
		toDelete = new LinkedList<WikiPageKey>();
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
		assertNotNull(creatorUserGroupId);
		// We use a long file name to test the uniqueness constraint
		String longFileNamePrefix = "loooooooooooooooooooooooooooooooooooooonnnnnnnnnnnnnnnnnnnnnnnnnnnggggggggggggggggggggggggg";
		
		// Create a few files
		attachOne = new S3FileHandle();
		attachOne.setBucketName("bucketName");
		attachOne.setKey("key");
		attachOne.setContentType("content type");
		attachOne.setContentSize(123l);
		attachOne.setContentMd5("md5");
		attachOne.setCreatedBy(creatorUserGroupId);
		attachOne.setFileName(longFileNamePrefix+".txt1");
		attachOne.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		attachOne.setEtag(UUID.randomUUID().toString());

		attachTwo = new S3FileHandle();
		attachTwo.setBucketName("bucketName2");
		attachTwo.setKey("key2");
		attachTwo.setContentType("content type2");
		attachTwo.setContentSize(123l);
		attachTwo.setContentMd5("md52");
		attachTwo.setCreatedBy(creatorUserGroupId);
		attachTwo.setFileName(longFileNamePrefix+".txt2");
		attachTwo.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		attachTwo.setEtag(UUID.randomUUID().toString());
		
		//Create different markdown content
		markdownOne = new S3FileHandle();
		markdownOne.setBucketName("markdownBucketName");
		markdownOne.setKey("key3");
		markdownOne.setContentType("content type3");
		markdownOne.setContentSize((long) 1231);
		markdownOne.setContentMd5("md53");
		markdownOne.setCreatedBy(creatorUserGroupId);
		markdownOne.setFileName("markdown1");
		markdownOne.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		markdownOne.setEtag(UUID.randomUUID().toString());
		
		markdownTwo = new S3FileHandle();
		markdownTwo.setBucketName("markdownBucketName2");
		markdownTwo.setKey("key4");
		markdownTwo.setContentType("content type4");
		markdownTwo.setContentSize((long) 1231);
		markdownTwo.setContentMd5("md54");
		markdownTwo.setCreatedBy(creatorUserGroupId);
		markdownTwo.setFileName("markdown2");
		markdownTwo.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		markdownTwo.setEtag(UUID.randomUUID().toString());

		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		fileHandleToCreate.add(attachOne);
		fileHandleToCreate.add(attachTwo);
		fileHandleToCreate.add(markdownOne);
		fileHandleToCreate.add(markdownTwo);
		fileMetadataDao.createBatch(fileHandleToCreate);

		attachOne = (S3FileHandle) fileMetadataDao.get(attachOne.getId());
		attachTwo = (S3FileHandle) fileMetadataDao.get(attachTwo.getId());
		markdownOne = (S3FileHandle) fileMetadataDao.get(markdownOne.getId());
		markdownTwo = (S3FileHandle) fileMetadataDao.get(markdownTwo.getId());
	}
	
	/**
	 * Create a new wiki page.
	 * @throws NotFoundException 
	 */
	@Test
	public void testCreate() throws NotFoundException{
		// Create a new wiki page with a single attachment
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn182";
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
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		
		// Create it
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(clone);
		assertNotNull(clone.getId());
		
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, clone.getId());
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
	public void testUpdate() throws NotFoundException, InterruptedException{
		V2WikiPage page = new V2WikiPage();
        String ownerId = "syn1082";
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
        List<String> newIds = new ArrayList<String>();
        newIds.add(attachOne.getId());
        
        // Create it
        V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, newIds);
        assertNotNull(clone);

        WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, clone.getId());
        toDelete.add(key);
        Long startModifiedOn = clone.getModifiedOn().getTime();
        
        List<Long> reservationIdsBeforeUpdate = wikiPageDao.getFileHandleReservationForWiki(key);
        // The archive should have one entry
        assertTrue(reservationIdsBeforeUpdate.size() == 1);
        
        List<Long> markdownIds = wikiPageDao.getMarkdownFileHandleIdsForWiki(key);
        assertTrue(markdownIds.size() == 1);
        assertEquals(markdownOne.getId(), markdownIds.get(0).toString());
        
        // Sleep to ensure the next date is higher.
        Thread.sleep(1000);
        
        // Add another attachment to the list and update markdown filehandle id to new markdown
        clone.getAttachmentFileHandleIds().add(attachTwo.getId());
        fileNameMap.put(attachTwo.getFileName(), attachTwo);
        
        List<String> newIds2 = new ArrayList<String>();
        newIds2.add(attachTwo.getId());
        
        // Update
        V2WikiPage clone2 = wikiPageDao.updateWikiPage(clone, fileNameMap, ownerId, ownerType, newIds2);                
        assertNotNull(clone2);
        assertNotNull(clone2.getEtag());
        
        // The etag should be new and the modified time should be greater than the previous modified time
        Long endModifiedOn = clone2.getModifiedOn().getTime();
        assertTrue("Modified On should have change and be greater than the previous timestamp", endModifiedOn > startModifiedOn);
        
        // Make sure the attachments for this wiki are updated to have 2 files
        assertEquals(clone.getAttachmentFileHandleIds(), clone2.getAttachmentFileHandleIds());
        
        List<Long> reservationIds = wikiPageDao.getFileHandleReservationForWiki(key);
        // The archive should have only added one more entry
        assertTrue(reservationIds.size() == 2);

        clone2.setMarkdownFileHandleId(markdownTwo.getId());
        // Update with same fileNameMap
        wikiPageDao.updateWikiPage(clone2, fileNameMap, ownerId, ownerType, new ArrayList<String>());                
        List<Long> reservationIds2 = wikiPageDao.getFileHandleReservationForWiki(key);                
        // the toInsert list of attachments should be 0 and the archive should still be size 2
        assertTrue(reservationIds2.size() == 2);
        List<Long> markdownIdsAfterUpdate = wikiPageDao.getMarkdownFileHandleIdsForWiki(key);
        assertTrue(markdownIdsAfterUpdate.size() == 2);
	}
	
	@Test
	public void testParentCycle() throws NotFoundException {
		// When updating the parent wiki id, the check for valid parent ids
		// should ensure that the update will not create a cycle. 
		String ownerId = "syn182";
		ObjectType ownerType = ObjectType.ENTITY;
		
		V2WikiPage grandparent = new V2WikiPage();
		grandparent.setTitle("Title");
		grandparent.setCreatedBy(creatorUserGroupId);
		grandparent.setModifiedBy(creatorUserGroupId);
		grandparent.setMarkdownFileHandleId(markdownOne.getId());
		grandparent.setAttachmentFileHandleIds(new LinkedList<String>());
		grandparent = wikiPageDao.create(grandparent, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, grandparent.getId());
		toDelete.add(key);
		
		V2WikiPage parent = new V2WikiPage();
		parent.setParentWikiId(grandparent.getId());
		parent.setTitle("Title");
		parent.setCreatedBy(creatorUserGroupId);
		parent.setModifiedBy(creatorUserGroupId);
		parent.setMarkdownFileHandleId(markdownOne.getId());
		parent.setAttachmentFileHandleIds(new LinkedList<String>());
		parent = wikiPageDao.create(parent, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
		WikiPageKey key2 = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, parent.getId());
		toDelete.add(key2);
		
		V2WikiPage child = new V2WikiPage();
		child.setParentWikiId(parent.getId());
		child.setTitle("Title");
		child.setCreatedBy(creatorUserGroupId);
		child.setModifiedBy(creatorUserGroupId);
		child.setMarkdownFileHandleId(markdownOne.getId());
		child.setAttachmentFileHandleIds(new LinkedList<String>());
		child = wikiPageDao.create(child, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
		WikiPageKey key3 = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, child.getId());
		toDelete.add(key3);
		
		grandparent.setParentWikiId(child.getId());
		// This creates a cycle
		try {
			grandparent = wikiPageDao.updateWikiPage(grandparent, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
			fail("Should have thrown an exception because this update makes a cycle.");
		} catch(IllegalArgumentException e) {
			// expected
		}

		// Create a wiki with its parent id equal to its own id
		// Should detect this short cycle.
		V2WikiPage thirdChild = new V2WikiPage();
		thirdChild.setId("100");
		thirdChild.setParentWikiId("100");
		thirdChild.setTitle("Title");
		thirdChild.setCreatedBy(creatorUserGroupId);
		thirdChild.setModifiedBy(creatorUserGroupId);
		thirdChild.setMarkdownFileHandleId(markdownOne.getId());
		thirdChild.setAttachmentFileHandleIds(new LinkedList<String>());
		try {
			thirdChild = wikiPageDao.create(thirdChild, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
			fail("Should have failed because creating this wiki creates a cycle.");
		} catch(Exception e) {
			// expected
		}
	}
	
	@Test
	public void testGetWikiHistoryAndRestore() throws NotFoundException, InterruptedException {
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn1082";
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
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		
		// Create it
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(clone);

		// Sleep to ensure the next date is higher.
		Thread.sleep(1000);
		 
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);
		
		// Try to get current wiki's markdown file handle id.
		String currentMarkdownHandleId = wikiPageDao.getMarkdownHandleId(key, null);
		assertEquals(markdownOne.getId(), currentMarkdownHandleId);
		
		// Add another attachment to the list, change the title, and update markdown filehandle id to new markdown
		clone.getAttachmentFileHandleIds().add(attachTwo.getId());
		fileNameMap.put(attachTwo.getFileName(), attachTwo);
		clone.setMarkdownFileHandleId(markdownTwo.getId());
		
		clone.setTitle("Updated title");
		
		List<String> newIds2 = new ArrayList<String>();
		newIds2.add(attachTwo.getId());
		
		// Update
		V2WikiPage clone2 = wikiPageDao.updateWikiPage(clone, fileNameMap, ownerId, ownerType, newIds2);		
		assertNotNull(clone2);
		assertTrue(clone2.getMarkdownFileHandleId().equals(markdownTwo.getId()));

		// At this point, the markdown database has two versions for this wiki page
		// Make sure history is accurate
		List<V2WikiHistorySnapshot> history = wikiPageDao.getWikiHistory(key, new Long(10), new Long(0));
		assertTrue(history.size() == 2);
		
		// history.get(0) is the most recent snapshot (the recently updated page)
		// history.get(1) older snapshot
		V2WikiHistorySnapshot oldWikiVersion = history.get(1);
		V2WikiHistorySnapshot currentWikiVersion = history.get(0);
		assertTrue(currentWikiVersion.getModifiedOn().getTime() > oldWikiVersion.getModifiedOn().getTime());
		assertTrue(Long.parseLong(currentWikiVersion.getVersion()) > Long.parseLong(oldWikiVersion.getVersion()));
		
		// Get the first version with one attatchment and the old markdown file handle
		V2WikiPage getFirstVersion = wikiPageDao.get(key, new Long(oldWikiVersion.getVersion()));
		List<String> firstVersionIds = getFirstVersion.getAttachmentFileHandleIds();
		assertEquals(1, firstVersionIds.size());
		assertEquals(attachOne.getId(), firstVersionIds.get(0));
		assertEquals(markdownOne.getId(), getFirstVersion.getMarkdownFileHandleId());
		// Title of wikipage should be the old title
		assertEquals("Title", getFirstVersion.getTitle());
		// Make sure ModifiedOn information is not equal to the most recent version's.
		assertTrue(!clone2.getModifiedOn().equals(getFirstVersion.getModifiedOn()));
		
		// Get the most recent version
		V2WikiPage getRecentVersion = wikiPageDao.get(key, new Long(currentWikiVersion.getVersion()));
		List<String> recentVersionIds = getRecentVersion.getAttachmentFileHandleIds();
		// Should be two attachments
		assertEquals(2, recentVersionIds.size());
		// Should have the new markdown handle
		assertEquals(markdownTwo.getId(), getRecentVersion.getMarkdownFileHandleId());
		// Wiki title should be the updated title
		assertEquals("Updated title", getRecentVersion.getTitle());
		// Wiki modified on should be updated
		assertTrue(clone2.getModifiedOn().equals(getRecentVersion.getModifiedOn()));
		
		// Test that correct versions are being accessed
		String currentMarkdownFileHandleId = wikiPageDao.getMarkdownHandleId(key, Long.parseLong(currentWikiVersion.getVersion()));
		List<String> currentAttachmentIds = wikiPageDao.getWikiFileHandleIds(key, Long.parseLong(currentWikiVersion.getVersion()));
		assertTrue(currentMarkdownFileHandleId.equals(markdownTwo.getId()));
		assertTrue(currentAttachmentIds.size() == 2);
		assertTrue(currentAttachmentIds.contains(attachOne.getId()) && currentAttachmentIds.contains(attachTwo.getId()));

		// To restore wiki to a the oldWikiVersion, mimick what Manager does.
		// Download old version of attachments and markdown, set, and update again
		V2WikiMarkdownVersion oldWikiContents = wikiPageDao.getVersionOfWikiContent(key, Long.parseLong(oldWikiVersion.getVersion()));
		assertNotNull(oldWikiContents);
		assertTrue(oldWikiContents.getMarkdownFileHandleId().equals(markdownOne.getId()));
		assertTrue(oldWikiContents.getAttachmentFileHandleIds().size() == 1);
		assertTrue(oldWikiContents.getAttachmentFileHandleIds().get(0).equals(attachOne.getId()));
		// Set up a new V2 WikiPage
		V2WikiPage newWikiVersion = new V2WikiPage();
		newWikiVersion.setId(clone.getId());
		// Sets etag to most recent etag so it will lock
		newWikiVersion.setEtag(clone2.getEtag());
		//Preserve creation metadata
		newWikiVersion.setCreatedBy(clone.getCreatedBy());
		newWikiVersion.setCreatedOn(clone.getCreatedOn());
		newWikiVersion.setModifiedBy(clone.getCreatedBy());
		newWikiVersion.setModifiedOn(new Date(1));
		// Assign restored content to the wiki page
		newWikiVersion.setMarkdownFileHandleId(oldWikiContents.getMarkdownFileHandleId());
		newWikiVersion.setAttachmentFileHandleIds(oldWikiContents.getAttachmentFileHandleIds());
		newWikiVersion.setTitle(oldWikiContents.getTitle());

		fileNameMap.remove(attachTwo.getFileName());
		
		// Update
		V2WikiPage restored = wikiPageDao.updateWikiPage(newWikiVersion, fileNameMap, ownerId, ownerType, new ArrayList<String>());		
		assertNotNull(restored);
		
		assertTrue(restored.getMarkdownFileHandleId().equals(markdownOne.getId()));
		assertTrue(restored.getAttachmentFileHandleIds().size() == 1);
		assertTrue(restored.getTitle().equals("Title"));
		// At this point, the restored wiki is the third version of the markdown/attachments
		List<V2WikiHistorySnapshot> historyAfterRestoration = wikiPageDao.getWikiHistory(key, new Long(10), new Long(0));
		assertTrue(historyAfterRestoration.size() == 3);
		
	}
	
	/**
	 * See PLFM
	 * @throws NotFoundException
	 * @throws InterruptedException
	 */
	@Test
	public void testGetWikiHistoryLastPage() throws NotFoundException, InterruptedException {
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn1082";
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
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		
		// Create it
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(clone);
		 
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);

		long limit = 10;
		long offset = 1000;
		List<V2WikiHistorySnapshot> history = wikiPageDao.getWikiHistory(key, limit, offset);
		assertNotNull(history);
		assertTrue(history.isEmpty());
	}
	
	@Test
	public void testGet() throws NotFoundException {
		// Create a new wiki page with a single attachment
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn182";
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
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		
		// Create it
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(clone);
		assertNotNull(clone.getId());
		
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);

		// Get wiki with key
		V2WikiPage retrievedWiki = wikiPageDao.get(key, null);
		// Compare the wiki pages
		assertEquals(retrievedWiki.getAttachmentFileHandleIds(), clone.getAttachmentFileHandleIds());
		assertEquals(retrievedWiki.getMarkdownFileHandleId(), clone.getMarkdownFileHandleId());
		assertEquals(retrievedWiki.getParentWikiId(), clone.getParentWikiId());
		assertEquals(retrievedWiki.getEtag(), clone.getEtag());
		assertTrue(retrievedWiki.equals(clone));
	}
	
	/**
	 * Create hierarchy of wiki pages
	 * @throws NotFoundException 
	 */
	@Test
	public void testWikiHierarchyAndSettingRoots() throws NotFoundException{
		V2WikiPage root = new V2WikiPage();
		String ownerId = "syn2224";
		ObjectType ownerType = ObjectType.ENTITY;
		root.setTitle("Root");
		root.setCreatedBy(creatorUserGroupId);
		root.setModifiedBy(creatorUserGroupId);
		root.setMarkdownFileHandleId(markdownOne.getId());
		
		// Create it
		root = wikiPageDao.create(root, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
		assertNotNull(root);
		String rootId = root.getId();
		assertNotNull(rootId);
		
		WikiPageKey rootKey = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, rootId);
		toDelete.add(rootKey);
		
		// In setRoot, passed through first branch because root == parent
		// Root id should be set it itself
		long rootIdForParent = jdbcTemplate.queryForObject(SQL_GET_ROOT_ID, Long.class, root.getId());
		assertEquals(String.valueOf(rootIdForParent), rootId);
		
		// Add add children in reverse alphabetical order.
		int childCount = 3;
		List<V2WikiPage> children = new LinkedList<V2WikiPage>();
		for(int i=childCount-1; i>-1; i--){
			V2WikiPage child = new V2WikiPage();
			child.setTitle("A"+i);
			child.setCreatedBy(creatorUserGroupId);
			child.setModifiedBy(creatorUserGroupId);
			// Set parent id before creating children
			// In setRoot, passes through else branch because parent id is not null
			child.setParentWikiId(root.getId());
			child.setMarkdownFileHandleId(markdownOne.getId());
			child = wikiPageDao.create(child, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
			children.add(child);
		}
		
		// Test one child; root should be set to parent root's id
		long rootIdForChild = jdbcTemplate.queryForObject(SQL_GET_ROOT_ID, Long.class, children.get(0).getId());
		assertEquals(String.valueOf(rootIdForChild), rootId);
		
		// Test getRootWiki for this hierarchy
		Long rootWikiId = wikiPageDao.getRootWiki(ownerId, ownerType);
		assertTrue(rootWikiId.equals(new Long(rootId)));
		
		// Test getHeaderTree
		// Should return tree (including the parent)
		long limit = 10L;
		long offset = 0L;
		List<V2WikiHeader> list = wikiPageDao.getHeaderTree(ownerId, ownerType, limit, offset);
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
				wikiPageDao.get(WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, childWiki.getId()), null);
				fail("This child should have been deleted when the parent was deleted.");
			}catch(NotFoundException e){
				// expected
			}
		}
		
	}
	
	@Test
	public void testDelete() throws NotFoundException, InterruptedException{
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn2082";
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
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		
		// Create it
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(clone);
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);
		
		List<V2WikiHistorySnapshot> historyBeforeUpdate = wikiPageDao.getWikiHistory(key, new Long(10), new Long(0));
		assertTrue(historyBeforeUpdate.size() == 1);

		Thread.sleep(1000);
		
		// Add another attachment to the list and update markdown filehandle id to another markdown
		clone.getAttachmentFileHandleIds().add(attachTwo.getId());
		clone.setMarkdownFileHandleId(markdownTwo.getId());
		fileNameMap.put(attachTwo.getFileName(), attachTwo);
		
		List<String> newIds2 = new ArrayList<String>();
		newIds2.add(attachTwo.getId());
		
		// Update
		V2WikiPage clone2 = wikiPageDao.updateWikiPage(clone, fileNameMap, ownerId, ownerType, newIds2);		assertNotNull(clone2);
		assertNotNull(clone2.getEtag());
		
		List<V2WikiHistorySnapshot> historyAfterUpdate = wikiPageDao.getWikiHistory(key, new Long(10), new Long(0));
		assertTrue(historyAfterUpdate.size() == 2);
		
		// Check cascade delete
		wikiPageDao.delete(key);
		try {
			wikiPageDao.getWikiHistory(key, new Long(10), new Long(0));
			fail("Should not be able to access history of a deleted wiki.");
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
		String ownerId = "syn3082";
		ObjectType ownerType = ObjectType.ENTITY;
		root.setTitle("Root");
		root.setCreatedBy(creatorUserGroupId);
		root.setModifiedBy(creatorUserGroupId);
		root.setMarkdownFileHandleId(markdownOne.getId());
		root.setAttachmentFileHandleIds(new LinkedList<String>());
		// Add file handles to the root.
		root.getAttachmentFileHandleIds().add(attachOne.getId());
		root.getAttachmentFileHandleIds().add(attachTwo.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(attachOne.getFileName(), attachOne);
		fileNameMap.put(attachTwo.getFileName(), attachTwo);
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		newIds.add(attachTwo.getId());
		
		// Create it
		root = wikiPageDao.create(root, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(root);
		WikiPageKey rootKey = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, root.getId());
		toDelete.add(rootKey);
		
		// Test the parent for the FileHandleIds
		List<String> handleList = wikiPageDao.getWikiFileHandleIds(rootKey, null);
		assertNotNull(handleList);
		assertEquals(2, handleList.size());
		assertEquals(attachOne.getId(), handleList.get(0));
		assertEquals(attachTwo.getId(), handleList.get(1));
		
		// Add a child.
		V2WikiPage child = new V2WikiPage();
		child.setTitle("Child");
		child.setCreatedBy(creatorUserGroupId);
		child.setModifiedBy(creatorUserGroupId);
		child.setMarkdownFileHandleId(markdownOne.getId());
		child.setParentWikiId(root.getId());
		// Add one file handle to the child.
		child.setAttachmentFileHandleIds(new LinkedList<String>());
		child.getAttachmentFileHandleIds().add(attachOne.getId());
		Map<String, FileHandle> childFileNameMap = new HashMap<String, FileHandle>();
		childFileNameMap.put(attachOne.getFileName(), attachOne);
		List<String> childNewIds = new ArrayList<String>();
		childNewIds.add(attachOne.getId());
		
		// Create it
		child = wikiPageDao.create(child, childFileNameMap, ownerId, ownerType, childNewIds);
		WikiPageKey childKey = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, child.getId());
		
		// Test the child for the FileHandleId
		List<String> childHandleList = wikiPageDao.getWikiFileHandleIds(childKey, null);
		assertNotNull(childHandleList);
		assertEquals(1, childHandleList.size());
		assertTrue(childHandleList.contains(attachOne.getId()));
	}
	
	/**
	 * Looks up file handles for wiki pages by name
	 * @throws Exception
	 */
	@Test
	public void testgetWikiAttachmentFileHandleForFileName() throws Exception{
		V2WikiPage root = new V2WikiPage();
		String ownerId = "syn4082";
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
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		newIds.add(attachTwo.getId());
		root = wikiPageDao.create(root, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(root);
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, root.getId());
		toDelete.add(key);
		
		// Now lookup each file using its name.
		String id = wikiPageDao.getWikiAttachmentFileHandleForFileName(key, attachOne.getFileName(), null);
		assertEquals(attachOne.getId(), id);
		// The second
		id = wikiPageDao.getWikiAttachmentFileHandleForFileName(key, attachTwo.getFileName(), null);
		assertEquals(attachTwo.getId(), id);
		
		// Update the page so it has no attachments
		root.getAttachmentFileHandleIds().clear();
		fileNameMap = new HashMap<String, FileHandle>();
		newIds = new ArrayList<String>();
		root = wikiPageDao.updateWikiPage(root, fileNameMap, ownerId, ownerType, newIds);
		
		try{
			wikiPageDao.getWikiAttachmentFileHandleForFileName(key, attachOne.getFileName(), null);
			fail("The file name does not exist and should have failed");
		}catch(NotFoundException e){
			// expected
		}

		// Try to get a version of the wiki's attachments, attachOne
		String versionOfAttachmentId = wikiPageDao.getWikiAttachmentFileHandleForFileName(key, attachOne.getFileName(), new Long(0));
		assertEquals(attachOne.getId(), versionOfAttachmentId);
		
		// Test the not found case
		try{
			wikiPageDao.getWikiAttachmentFileHandleForFileName(key, attachTwo.getFileName()+"1", null);
			fail("The file name does not exist and should have failed");
		}catch(NotFoundException e){
			// expected
		}
	}
	
	@Test
	public void testGetAndUpdateOrderHint() throws Exception {
		// Create a new wiki page.
		V2WikiPage page = new V2WikiPage();
		String ownerId = "2086";
		ObjectType ownerType = ObjectType.EVALUATION;
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdownOne.getId());
		
		// Un-null added attachments.
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		List<String> newIds = new ArrayList<String>();
		
		// Create wiki page key
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(clone);
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);
		
		// Make order hint.
		V2WikiOrderHint orderHint = new V2WikiOrderHint();
		orderHint.setEtag("etag");
		orderHint.setIdList(Arrays.asList(new String[] {"A", "B", "C", "D"}));
		orderHint.setOwnerId(ownerId);
		orderHint.setOwnerObjectType(ownerType);
		
		// Update order hint.
		V2WikiOrderHint recordedOrderHint = wikiPageDao.updateOrderHint(orderHint, key);
		
		// Check if update happened.
		assertTrue(Arrays.equals(orderHint.getIdList().toArray(), recordedOrderHint.getIdList().toArray()));
		assertTrue(recordedOrderHint.getOwnerId().equals(ownerId));
		assertTrue(recordedOrderHint.getOwnerObjectType().equals(ObjectType.EVALUATION));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetFileHandleIdsAssociateWithWikiNullFileHandleIds(){
		wikiPageDao.getFileHandleIdsAssociatedWithWikiAttachments(null, "1");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetFileHandleIdsAssociateWithWikiNullWikiPageId() {
		wikiPageDao.getFileHandleIdsAssociatedWithWikiAttachments(new ArrayList<String>(0), null);
	}

	@Test
	public void testGetFileHandleIdsAssociateWithWikiEmptyFileHandleIds() {
		Set<String> fileHandleIds = wikiPageDao.getFileHandleIdsAssociatedWithWikiAttachments(new ArrayList<String>(0), "1");
		assertNotNull(fileHandleIds);
		assertTrue(fileHandleIds.isEmpty());
	}

	@Test
	public void testGetFileHandleIdsAssociateWithWikiNoAttachments() {
		List<String> givenFileHandleIds = Arrays.asList("1", "2");
		Set<String> fileHandleIds = wikiPageDao.getFileHandleIdsAssociatedWithWikiAttachments(givenFileHandleIds, "1");
		assertNotNull(fileHandleIds);
		assertTrue(fileHandleIds.isEmpty());
	}

	@Test
	public void testGetFileHandleIdsAssociateWithWiki() {
		// Create a new wiki page with a single attachment
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn192";
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
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		
		// Create it
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(clone);
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);
		
		List<String> givenFileHandleIds = Arrays.asList("1", "2", attachOne.getId());
		Set<String> fileHandleIds = wikiPageDao.getFileHandleIdsAssociatedWithWikiAttachments(givenFileHandleIds, clone.getId());
		assertNotNull(fileHandleIds);
		assertEquals(1L, fileHandleIds.size());
		assertTrue(fileHandleIds.contains(attachOne.getId()));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testDeleteWikiVersionsNullWikiId() throws Exception {
		Long versionToDelete = 123L;
		wikiPageDao.deleteWikiVersions(null, versionToDelete);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testDeleteWikiVersionsNullVersions() throws Exception {
		WikiPageKey key = new WikiPageKey();
		wikiPageDao.deleteWikiVersions(key, null);
	}
	
	@Test
	public void testDeleteWikiVersions() throws Exception {
		// Create a new wiki page
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn192";
		ObjectType ownerType = ObjectType.ENTITY;
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdownOne.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		List<String> fileIds = new ArrayList<String>();
		
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, fileIds);
		assertNotNull(clone);
		
		V2WikiPage clone2 = createVersions(clone, ownerId, ownerType, 4);
		
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);
		assertEquals(5, wikiPageDao.getWikiHistory(key, 10L, 0L).size());
		
		Long minVerToKeep = 2L;

		wikiPageDao.deleteWikiVersions(key, minVerToKeep);

		List<V2WikiHistorySnapshot> history = wikiPageDao.getWikiHistory(key, 10L, 0L);
		assertNotNull(history);
		assertEquals(3, history.size());
		assertEquals("4", history.get(0).getVersion());
		assertEquals("3", history.get(1).getVersion());
		assertEquals("2", history.get(2).getVersion());

	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testUpdateWikiEtagNullKey() throws Exception {
		wikiPageDao.updateWikiEtag(null, "etag");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testUpdateWikiEtagNullEtag() throws Exception {
		wikiPageDao.updateWikiEtag(new WikiPageKey(), null);
	}
	
	@Test
	public void testUpdateWikiEtag() throws Exception {
		// Create a new wiki page
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn192";
		ObjectType ownerType = ObjectType.ENTITY;
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdownOne.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		List<String> fileIds = new ArrayList<String>();
		
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, fileIds);
		assertNotNull(clone);
		String etag1 = clone.getEtag();
		assertNotNull(etag1);
		
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);
		
		// Call under test
		wikiPageDao.updateWikiEtag(key, "etag2");
		
		V2WikiPage clone2 = wikiPageDao.get(key, null);
		assertNotNull(clone2);
		String etag2 = clone2.getEtag();
		assertNotNull(etag2);
		assertFalse(etag1.equals(etag2));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetNumberOfVersionsNullWikiKey() {
		wikiPageDao.getNumberOfVersions(null);
	}
	
	@Test
	public void testGetNumberOfVersions() {
		// Create a new wiki page
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn192";
		ObjectType ownerType = ObjectType.ENTITY;
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdownOne.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		List<String> fileIds = new ArrayList<String>();
		
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, fileIds);
		assertNotNull(clone);
		V2WikiPage clone2 = createVersions(clone, ownerId, ownerType, 4);
		WikiPageKey key1 = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, clone2.getId());
		toDelete.add(key1);
		
		// Create another page to check that query does filter on id
		page.setTitle("title2");
		page.setMarkdownFileHandleId(markdownTwo.getId());
		V2WikiPage clone3 = wikiPageDao.create(page, fileNameMap, "syn193", ObjectType.ENTITY, fileIds);
		assertNotNull(clone3);
		WikiPageKey key2 = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, clone3.getId());
		toDelete.add(key2);
		
		Long numVersions = wikiPageDao.getNumberOfVersions(key1);
		
		assertTrue(numVersions == 5);
		
	}

	@Test
	public void testGetWikiVersionByRank() throws Exception {
		// Create a new wiki page
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn192";
		ObjectType ownerType = ObjectType.ENTITY;
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdownOne.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		List<String> fileIds = new ArrayList<String>();

		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, fileIds);
		assertNotNull(clone);
		V2WikiPage clone2 = createVersions(clone, ownerId, ownerType, 4);
		WikiPageKey key1 = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, clone2.getId());
		toDelete.add(key1);

		Long vAtRank0 = wikiPageDao.getWikiVersionByRank(key1, 0L);
		assertNotNull(vAtRank0);
		assertEquals(4L, vAtRank0.longValue());
		Long vAtRank2 = wikiPageDao.getWikiVersionByRank(key1, 2L);
		assertEquals(2L, vAtRank2.longValue());
		Long vAtRank5 = wikiPageDao.getWikiVersionByRank(key1, 4L);
		assertEquals(0L, vAtRank5.longValue());
		Long vAtRank7 = wikiPageDao.getWikiVersionByRank(key1, 7L);
		assertEquals(0L, vAtRank5.longValue());

	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithWikiMarkdown(){
		String ownerId = "syn192";
		ObjectType ownerType = ObjectType.ENTITY;
		V2WikiPage one = new V2WikiPage();
		one.setTitle("one");
		one.setCreatedBy(creatorUserGroupId);
		one.setModifiedBy(creatorUserGroupId);
		one.setMarkdownFileHandleId(markdownOne.getId());
		one.setAttachmentFileHandleIds(new LinkedList<String>());
		one = wikiPageDao.create(one, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
		WikiPageKey oneKey = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, one.getId());
		toDelete.add(oneKey);
		// create a few versions of this wiki
		createVersions(one, ownerId, ownerType, 2);
		
		// create another wiki
		ownerId = "syn1923";
		V2WikiPage two = new V2WikiPage();
		two.setTitle("two");
		two.setCreatedBy(creatorUserGroupId);
		two.setModifiedBy(creatorUserGroupId);
		two.setMarkdownFileHandleId(attachOne.getId());
		two.setAttachmentFileHandleIds(new LinkedList<String>());
		two = wikiPageDao.create(two, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
		WikiPageKey twoKey = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, two.getId());
		toDelete.add(twoKey);
		// call under test
		List<String> fileHandleIds = Lists.newArrayList(markdownTwo.getId(), markdownOne.getId(), attachOne.getId());
		Set<String> results = wikiPageDao.getFileHandleIdsAssociatedWithWikiMarkdown(fileHandleIds, one.getId());
		assertNotNull(results);
		assertEquals(2, results.size());
		assertTrue(results.contains(markdownOne.getId()));
		assertTrue(results.contains(markdownTwo.getId()));
		assertFalse(results.contains(attachOne.getId()));
		// check the other wiki
		fileHandleIds = Lists.newArrayList(attachOne.getId(), "-123");
		results = wikiPageDao.getFileHandleIdsAssociatedWithWikiMarkdown(fileHandleIds, two.getId());
		assertNotNull(results);
		assertEquals(1, results.size());
		assertTrue(results.contains(attachOne.getId()));
	}
	
	
	@Test
	public void testGetFileHandleIdsAssociatedWithWikiMarkdownEmpty(){
		// call under test
		List<String> fileHandleIds = new LinkedList<>();
		Set<String> results = wikiPageDao.getFileHandleIdsAssociatedWithWikiMarkdown(fileHandleIds, "123");
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithWikiMarkdownEmptyResults(){
		// There should be no matches for this case.
		List<String> fileHandleIds = Lists.newArrayList("456");
		Set<String> results = wikiPageDao.getFileHandleIdsAssociatedWithWikiMarkdown(fileHandleIds, "123");
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	// Just create versions with modified page title
	private V2WikiPage createVersions(V2WikiPage page, String ownerId, ObjectType ownerType, int numVersions) {
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		List<String> fileIds = new ArrayList<String>();
		for (int v = 0; v < numVersions; v++) {
			page.setTitle(page.getTitle() + " - " + v);
			page.setMarkdownFileHandleId(v % 2 == 0 ? markdownOne.getId() : markdownTwo.getId());
			page = wikiPageDao.updateWikiPage(page, fileNameMap, ownerId, ownerType, fileIds);
		}
		return page;
	}
}
