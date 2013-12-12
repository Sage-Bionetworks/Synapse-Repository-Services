package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class V2WikiControllerTest {
	
	@Autowired
	private EntityServletTestHelper entityServletHelper;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private FileHandleDao fileMetadataDao;
	
	private String userName;
	private String ownerId;
	
	private Project entity;
	private Evaluation evaluation;
	private List<WikiPageKey> toDelete;
	private S3FileHandle handleOne;
	private S3FileHandle markdown;
	private S3FileHandle markdownTwo;
	private PreviewFileHandle handleTwo;
	
	@Before
	public void before() throws Exception{
		// get user IDs
		userName = userManager.getGroupName(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		ownerId = userManager.getUserInfo(userName).getIndividualGroup().getId();
		
		toDelete = new LinkedList<WikiPageKey>();
		// Create a file handle
		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(ownerId);
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("mainFileKey");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne = fileMetadataDao.createFile(handleOne);
		// Create a preview
		handleTwo = new PreviewFileHandle();
		handleTwo.setCreatedBy(ownerId);
		handleTwo.setCreatedOn(new Date());
		handleTwo.setBucketName("bucket");
		handleTwo.setKey("previewFileKey");
		handleTwo.setEtag("etag");
		handleTwo.setFileName("bar.txt");
		handleTwo = fileMetadataDao.createFile(handleTwo);
		// Set two as the preview of one
		fileMetadataDao.setPreviewId(handleOne.getId(), handleTwo.getId());
		markdown = new S3FileHandle();
		markdown.setCreatedBy(ownerId);
		markdown.setCreatedOn(new Date());
		markdown.setBucketName("bucket");
		markdown.setKey("markdownKey");
		markdown.setEtag("etag");
		markdown.setFileName("markdown");
		markdown = fileMetadataDao.createFile(markdown);
		markdownTwo = new S3FileHandle();
		markdownTwo.setCreatedBy(ownerId);
		markdownTwo.setCreatedOn(new Date());
		markdownTwo.setBucketName("bucket");
		markdownTwo.setKey("markdownKey2");
		markdownTwo.setEtag("etag2");
		markdownTwo.setFileName("markdown2");
		markdownTwo = fileMetadataDao.createFile(markdownTwo);
	}
	
	@After
	public void after() throws Exception{
		for(WikiPageKey key: toDelete){
			entityServletHelper.deleteV2WikiPage(key, userName);
		}
		if(evaluation != null){
			try {
				entityServletHelper.deleteEvaluation(evaluation.getId(), userName);
			} catch (Exception e) {}
		}
		if(entity != null){
			UserInfo userInfo = userManager.getUserInfo(userName);
			nodeManager.delete(userInfo, entity.getId());
		}
		if(handleOne != null && handleOne.getId() != null){
			fileMetadataDao.delete(handleOne.getId());
		}
		if(handleTwo != null && handleTwo.getId() != null){
			fileMetadataDao.delete(handleTwo.getId());
		}
		if(markdown != null && markdown.getId() != null) {
			fileMetadataDao.delete(markdown.getId());
		}
		if(markdownTwo != null && markdownTwo.getId() != null) {
			fileMetadataDao.delete(markdownTwo.getId());
		}
	}
	
	@Test
	public void testEntityWikiCRUD() throws Exception {
		// create an entity
		entity = new Project();
		entity.setEntityType(Project.class.getName());
		entity = (Project) entityServletHelper.createEntity(entity, userName, null);
		// Test all wiki CRUD for an entity
		doWikiCRUDForOwnerObject(entity.getId(), ObjectType.ENTITY);
	}
	
	@Test
	public void testCompetitionWikiCRUD() throws Exception {
		// create an entity
		evaluation = new Evaluation();
		evaluation.setName("testCompetitionWikiCRUD");
		evaluation.setContentSource(KeyFactory.SYN_ROOT_ID);
		evaluation.setDescription("a test descrption");
		evaluation.setStatus(EvaluationStatus.OPEN);
		evaluation = entityServletHelper.createEvaluation(evaluation, userName);
		// Test all wiki CRUD for an entity
		doWikiCRUDForOwnerObject(evaluation.getId(), ObjectType.EVALUATION);
	}
	
	/**
	 * Perform all Wiki CRUD for a given owner.  This allows the same test to be run for each owner type.
	 * @param ownerId
	 * @param ownerType
	 * @throws Exception
	 */
	private void doWikiCRUDForOwnerObject(String ownerId, ObjectType ownerType) throws Exception{
		
		// Create a wiki page
		V2WikiPage wiki = new V2WikiPage();
		wiki.setTitle("testCreateEntityWikiRoundTrip-"+ownerId+"-"+ownerType);
		wiki.setMarkdownFileHandleId(markdown.getId());
		wiki.setAttachmentFileHandleIds(new LinkedList<String>());
		wiki = entityServletHelper.createV2WikiPage(userName, ownerId, ownerType, wiki);
		assertNotNull(wiki);
		
		assertNotNull(wiki.getId());
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, wiki.getId());
		toDelete.add(key);
		assertNotNull(wiki.getEtag());
		assertNotNull(ownerId, wiki.getModifiedBy());
		assertNotNull(ownerId, wiki.getCreatedBy());
		
		URL markdownPresigned = entityServletHelper.getV2WikiMarkdownFileURL(userName, key, null, null);
		assertNotNull(markdownPresigned);
		assertTrue(markdownPresigned.toString().indexOf("markdownKey") > 0);
		
		PaginatedResults<V2WikiHistorySnapshot> startHistory = entityServletHelper.getV2WikiHistory(key, userName, new Long(0), new Long(10));
		assertNotNull(startHistory);
		List<V2WikiHistorySnapshot> firstSnapshot = startHistory.getResults();
		assertNotNull(firstSnapshot);
		assertEquals(1, firstSnapshot.size());
		// Results are ordered, descending
		// First snapshot is the most recent modification/highest version
		assertEquals("0", firstSnapshot.get(0).getVersion());
		
		// Get the wiki page.
		V2WikiPage clone = entityServletHelper.getV2WikiPage(key, userName, null);
		assertNotNull(clone);
		System.out.println(clone);
		assertEquals(wiki, clone);
		V2WikiPage getFirstVersion = entityServletHelper.getV2WikiPage(key, userName, new Long(0));
		assertEquals(wiki, getFirstVersion);
		
		// Get the root wiki
		V2WikiPage root = entityServletHelper.getRootV2WikiPage(ownerId, ownerType, userName);
		// The root should match the clone
		assertEquals(clone, root);
		
		// Update the wiki
		clone.setMarkdownFileHandleId(markdownTwo.getId());
		clone.getAttachmentFileHandleIds().add(handleOne.getId());
		clone.setTitle("Version 1 title");
		String currentEtag = clone.getEtag();
		V2WikiPage cloneUpdated = entityServletHelper.updateWikiPage(userName, ownerId, ownerType, clone);
		assertNotNull(cloneUpdated);
		assertEquals("Version 1 title", cloneUpdated.getTitle());
		assertEquals(cloneUpdated.getMarkdownFileHandleId(), markdownTwo.getId());
		assertEquals(cloneUpdated.getAttachmentFileHandleIds().size(), 1);
		assertEquals(cloneUpdated.getAttachmentFileHandleIds().get(0), handleOne.getId());
		assertFalse("The etag should have changed from the update", currentEtag.equals(cloneUpdated.getEtag()));
		
		// Update one more time
		cloneUpdated.getAttachmentFileHandleIds().add(handleTwo.getId());
		cloneUpdated.setTitle("Version 2 title");
		String currentEtag2 = cloneUpdated.getEtag();
		V2WikiPage cloneUpdated2 = entityServletHelper.updateWikiPage(userName, ownerId, ownerType, cloneUpdated);
		assertNotNull(cloneUpdated2);
		assertEquals(cloneUpdated2.getMarkdownFileHandleId(), markdownTwo.getId());
		assertEquals(cloneUpdated2.getAttachmentFileHandleIds().size(), 2);
		assertEquals(cloneUpdated2.getTitle(), "Version 2 title");
		assertFalse("The etag should have changed from the update", currentEtag2.equals(cloneUpdated2.getEtag()));
		
		URL markdownPresignedUpdated = entityServletHelper.getV2WikiMarkdownFileURL(userName, key, new Long(0), null);
		assertNotNull(markdownPresignedUpdated);
		assertTrue(markdownPresignedUpdated.toString().indexOf("markdownKey") > 0);
		Boolean redirectMarkdown = Boolean.FALSE;
		markdownPresignedUpdated  = entityServletHelper.getV2WikiMarkdownFileURL(userName, key, new Long(0), redirectMarkdown);
		assertNotNull(markdownPresignedUpdated);
		assertTrue(markdownPresignedUpdated.toString().indexOf("markdownKey") > 0);
		
		// Get history (there should be three snapshots returned)
		PaginatedResults<V2WikiHistorySnapshot> historyResults = entityServletHelper.getV2WikiHistory(key, userName, new Long(0), new Long(10));
		assertNotNull(historyResults);
		List<V2WikiHistorySnapshot> snapshots = historyResults.getResults();
		assertNotNull(snapshots);
		assertEquals(3, snapshots.size());
		// Results are ordered, descending
		// First snapshot is the most recent modification/highest version
		assertEquals("2", snapshots.get(0).getVersion());
		assertEquals("1", snapshots.get(1).getVersion());
		assertEquals("0", snapshots.get(2).getVersion());
		
		// First version should have no file handles
		FileHandleResults oldHandles = entityServletHelper.getV2WikiFileHandles(userName, key, new Long(0));
		assertNotNull(oldHandles);
		assertNotNull(oldHandles.getList());
		assertEquals(0, oldHandles.getList().size());
		
		Long versionToRestore = new Long(1);
		// Get an older version
		V2WikiPage versionOne = entityServletHelper.getV2WikiPage(key, userName, versionToRestore);
		assertEquals(markdownTwo.getId(), versionOne.getMarkdownFileHandleId());
		assertEquals(1, versionOne.getAttachmentFileHandleIds().size());
		assertEquals("Version 1 title", versionOne.getTitle());
		assertEquals(cloneUpdated.getModifiedOn(), versionOne.getModifiedOn());
		
		// Restore wiki to version 1 which had markdownTwo and one file attachment
		String currentEtag3 = cloneUpdated2.getEtag();
		V2WikiPage restored = entityServletHelper.restoreWikiPage(userName, ownerId, ownerType, cloneUpdated2, versionToRestore);
		assertNotNull(restored);
		assertFalse("The etag should have changed from the restore", currentEtag3.equals(restored.getEtag()));
		
		assertEquals(restored.getMarkdownFileHandleId(), markdownTwo.getId());
		assertEquals(restored.getAttachmentFileHandleIds().size(), 1);

		// Add a child wiki
		V2WikiPage child = new V2WikiPage();
		child.setTitle("Child");
		child.setMarkdownFileHandleId(markdown.getId());
		child.setParentWikiId(wiki.getId());
		child.setAttachmentFileHandleIds(new LinkedList<String>());
		// Note, we are adding a file handle with a preview.
		// Both the S3FileHandle and its Preview should be returned from getWikiFileHandles()
		child.getAttachmentFileHandleIds().add(handleOne.getId());
		
		// Create child!
		child = entityServletHelper.createV2WikiPage(userName, ownerId, ownerType, child);
		assertNotNull(child);
		assertNotNull(child.getId());
		WikiPageKey childKey = new WikiPageKey(ownerId, ownerType, child.getId());
		toDelete.add(childKey);
		// List the hierarchy
		PaginatedResults<V2WikiHeader> paginated = entityServletHelper.getV2WikiHeaderTree(userName, ownerId, ownerType);
		assertNotNull(paginated);
		assertNotNull(paginated.getResults());
		assertEquals(2, paginated.getResults().size());
		
		// check the root header.
		V2WikiHeader rootHeader = paginated.getResults().get(0);
		assertEquals(cloneUpdated.getId(), rootHeader.getId());
		assertEquals(cloneUpdated.getTitle(), rootHeader.getTitle());
		assertEquals(null, rootHeader.getParentId());
		
		// Check the child header
		V2WikiHeader childHeader =  paginated.getResults().get(1);
		assertEquals(child.getId(), childHeader.getId());
		assertEquals(child.getTitle(), childHeader.getTitle());
		assertEquals(wiki.getId(), childHeader.getParentId());
		
		// Check that we can get the FileHandles	
		FileHandleResults handles = entityServletHelper.getV2WikiFileHandles(userName, childKey, null);
		assertNotNull(handles);
		assertNotNull(handles.getList());
		assertEquals(2, handles.getList().size());
		// The first should be the S3FileHandle, the second should be the Preview.
		assertEquals(handleOne.getId(), handles.getList().get(0).getId());
		assertEquals(handleTwo.getId(), handles.getList().get(1).getId());
		
		// Get the presigned URL for the first file
		URL presigned  = entityServletHelper.getV2WikiAttachmentFileURL(userName, childKey, handleOne.getFileName(), null);
		assertNotNull(presigned);
		assertTrue(presigned.toString().indexOf("mainFileKey") > 0);
		System.out.println(presigned);
		// Get the preview presigned URL.
		presigned  = entityServletHelper.getV2WikiAttachmentPreviewFileURL(userName, childKey, handleOne.getFileName(), null);
		assertNotNull(presigned);
		assertTrue(presigned.toString().indexOf("previewFileKey") > 0);
		System.out.println(presigned);
		
		// Make sure we can get the URLs without a redirect
		Boolean redirect = Boolean.FALSE;
		presigned  = entityServletHelper.getV2WikiAttachmentFileURL(userName, childKey, handleOne.getFileName(), redirect);
		assertNotNull(presigned);
		assertTrue(presigned.toString().indexOf("mainFileKey") > 0);
		System.out.println(presigned);
		// again without the redirct
		presigned  = entityServletHelper.getV2WikiAttachmentPreviewFileURL(userName, childKey, handleOne.getFileName(), redirect);
		assertNotNull(presigned);
		assertTrue(presigned.toString().indexOf("previewFileKey") > 0);
		System.out.println(presigned);
		
		// Now delete the wiki
		entityServletHelper.deleteV2WikiPage(key, userName);
		try {
			entityServletHelper.getV2WikiPage(key, userName, null);
			fail("The wiki should have been deleted");
		} catch (NotFoundException e) {
			// this is expected
		}
		// the child should be delete as well
		try {
			entityServletHelper.getV2WikiPage(childKey, userName, null);
			fail("The wiki should have been deleted");
		} catch (NotFoundException e) {
			// this is expected
		}
	}

}
