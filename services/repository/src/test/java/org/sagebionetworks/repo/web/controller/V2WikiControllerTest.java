package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.springframework.beans.factory.annotation.Autowired;

public class V2WikiControllerTest extends AbstractAutowiredControllerTestBase {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private IdGenerator idGenerator;
	
	private Long adminUserId;
	private String adminUserIdString;
	
	private Project entity;
	private Evaluation evaluation;
	private List<WikiPageKey> toDelete;
	private S3FileHandle fileOneHandle;
	private S3FileHandle markdownOneHandle;
	private S3FileHandle markdownTwoHandle;
	private PreviewFileHandle fileOnePreviewHandle;
	
	@Before
	public void before() throws Exception{
		// get user IDs
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserIdString = adminUserId.toString();

		toDelete = new LinkedList<WikiPageKey>();
		
		// Create a file handle
		fileOneHandle = new S3FileHandle();
		fileOneHandle.setCreatedBy(adminUserIdString);
		fileOneHandle.setCreatedOn(new Date());
		fileOneHandle.setBucketName("bucket");
		fileOneHandle.setKey("mainFileKey");
		fileOneHandle.setEtag("etag");
		fileOneHandle.setFileName("foo.bar");
		fileOneHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fileOneHandle.setEtag(UUID.randomUUID().toString());
	
		// Create a preview
		fileOnePreviewHandle = new PreviewFileHandle();
		fileOnePreviewHandle.setCreatedBy(adminUserIdString);
		fileOnePreviewHandle.setCreatedOn(new Date());
		fileOnePreviewHandle.setBucketName("bucket");
		fileOnePreviewHandle.setKey("previewFileKey");
		fileOnePreviewHandle.setEtag("etag");
		fileOnePreviewHandle.setFileName("bar.txt");
		fileOnePreviewHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fileOnePreviewHandle.setEtag(UUID.randomUUID().toString());
		
		markdownOneHandle = new S3FileHandle();
		markdownOneHandle.setCreatedBy(adminUserIdString);
		markdownOneHandle.setCreatedOn(new Date());
		markdownOneHandle.setBucketName("bucket");
		markdownOneHandle.setKey("markdownKey");
		markdownOneHandle.setEtag("etag");
		markdownOneHandle.setFileName("markdown");
		markdownOneHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		markdownOneHandle.setEtag(UUID.randomUUID().toString());
		
		markdownTwoHandle = new S3FileHandle();
		markdownTwoHandle.setCreatedBy(adminUserIdString);
		markdownTwoHandle.setCreatedOn(new Date());
		markdownTwoHandle.setBucketName("bucket");
		markdownTwoHandle.setKey("markdownKey2");
		markdownTwoHandle.setEtag("etag2");
		markdownTwoHandle.setFileName("markdown2");
		markdownTwoHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		markdownTwoHandle.setEtag(UUID.randomUUID().toString());

		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		fileHandleToCreate.add(fileOneHandle);
		fileHandleToCreate.add(fileOnePreviewHandle);
		fileHandleToCreate.add(markdownOneHandle);
		fileHandleToCreate.add(markdownTwoHandle);
		fileHandleDao.createBatch(fileHandleToCreate);

		fileOneHandle = (S3FileHandle) fileHandleDao.get(fileOneHandle.getId());
		fileOnePreviewHandle = (PreviewFileHandle) fileHandleDao.get(fileOnePreviewHandle.getId());
		markdownOneHandle = (S3FileHandle) fileHandleDao.get(markdownOneHandle.getId());
		markdownTwoHandle = (S3FileHandle) fileHandleDao.get(markdownTwoHandle.getId());
		fileHandleDao.setPreviewId(fileOneHandle.getId(), fileOnePreviewHandle.getId());
	}
	
	@After
	public void after() throws Exception{
		for(WikiPageKey key: toDelete){
			entityServletHelper.deleteV2WikiPage(key, adminUserId);
		}
		if(evaluation != null){
			try {
				entityServletHelper.deleteEvaluation(evaluation.getId(), adminUserId);
			} catch (Exception e) {}
		}
		if(entity != null){
			UserInfo userInfo = userManager.getUserInfo(adminUserId);
			nodeManager.delete(userInfo, entity.getId());
		}
		if(fileOneHandle != null && fileOneHandle.getId() != null){
			fileHandleDao.delete(fileOneHandle.getId());
		}
		if(fileOnePreviewHandle != null && fileOnePreviewHandle.getId() != null){
			fileHandleDao.delete(fileOnePreviewHandle.getId());
		}
		if(markdownOneHandle != null && markdownOneHandle.getId() != null) {
			fileHandleDao.delete(markdownOneHandle.getId());
		}
		if(markdownTwoHandle != null && markdownTwoHandle.getId() != null) {
			fileHandleDao.delete(markdownTwoHandle.getId());
		}
	}
	
	@Test
	public void testEntityWikiCRUD() throws Exception {
		// create an entity
		entity = new Project();
		entity.setEntityType(Project.class.getName());
		entity = (Project) entityServletHelper.createEntity(entity, adminUserId, null);
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
		evaluation = entityServletHelper.createEvaluation(evaluation, adminUserId);
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
		wiki.setMarkdownFileHandleId(markdownOneHandle.getId());
		wiki.setAttachmentFileHandleIds(new LinkedList<String>());
		wiki = entityServletHelper.createV2WikiPage(adminUserId, ownerId, ownerType, wiki);
		assertNotNull(wiki);
		
		assertNotNull(wiki.getId());
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wiki.getId());
		toDelete.add(key);
		assertNotNull(wiki.getEtag());
		assertNotNull(ownerId, wiki.getModifiedBy());
		assertNotNull(ownerId, wiki.getCreatedBy());
		
		URL markdownPresigned = entityServletHelper.getV2WikiMarkdownFileURL(adminUserId, key, null, null);
		assertNotNull(markdownPresigned);
		assertTrue(markdownPresigned.toString().indexOf("markdownKey") > 0);
		
		PaginatedResults<V2WikiHistorySnapshot> startHistory = entityServletHelper.getV2WikiHistory(key, adminUserId, new Long(0), new Long(10));
		assertNotNull(startHistory);
		List<V2WikiHistorySnapshot> firstSnapshot = startHistory.getResults();
		assertNotNull(firstSnapshot);
		assertEquals(1, firstSnapshot.size());
		// Results are ordered, descending
		// First snapshot is the most recent modification/highest version
		assertEquals("0", firstSnapshot.get(0).getVersion());
		
		// Get the wiki page.
		V2WikiPage clone = entityServletHelper.getV2WikiPage(key, adminUserId, null);
		assertNotNull(clone);
		System.out.println(clone);
		assertEquals(wiki, clone);
		V2WikiPage getFirstVersion = entityServletHelper.getV2WikiPage(key, adminUserId, new Long(0));
		assertEquals(wiki, getFirstVersion);
		
		// Get the root wiki
		V2WikiPage root = entityServletHelper.getRootV2WikiPage(ownerId, ownerType, adminUserId);
		// The root should match the clone
		assertEquals(clone, root);
		
		// Update the wiki
		clone.setMarkdownFileHandleId(markdownTwoHandle.getId());
		clone.getAttachmentFileHandleIds().add(fileOneHandle.getId());
		clone.setTitle("Version 1 title");
		String currentEtag = clone.getEtag();
		V2WikiPage cloneUpdated = entityServletHelper.updateWikiPage(adminUserId, ownerId, ownerType, clone);
		assertNotNull(cloneUpdated);
		assertEquals("Version 1 title", cloneUpdated.getTitle());
		assertEquals(cloneUpdated.getMarkdownFileHandleId(), markdownTwoHandle.getId());
		assertEquals(cloneUpdated.getAttachmentFileHandleIds().size(), 1);
		assertEquals(cloneUpdated.getAttachmentFileHandleIds().get(0), fileOneHandle.getId());
		assertFalse("The etag should have changed from the update", currentEtag.equals(cloneUpdated.getEtag()));
		
		// Update one more time
		cloneUpdated.getAttachmentFileHandleIds().add(fileOnePreviewHandle.getId());
		cloneUpdated.setTitle("Version 2 title");
		String currentEtag2 = cloneUpdated.getEtag();
		V2WikiPage cloneUpdated2 = entityServletHelper.updateWikiPage(adminUserId, ownerId, ownerType, cloneUpdated);
		assertNotNull(cloneUpdated2);
		assertEquals(cloneUpdated2.getMarkdownFileHandleId(), markdownTwoHandle.getId());
		assertEquals(cloneUpdated2.getAttachmentFileHandleIds().size(), 2);
		assertEquals(cloneUpdated2.getTitle(), "Version 2 title");
		assertFalse("The etag should have changed from the update", currentEtag2.equals(cloneUpdated2.getEtag()));
		
		URL markdownPresignedUpdated = entityServletHelper.getV2WikiMarkdownFileURL(adminUserId, key, new Long(0), null);
		assertNotNull(markdownPresignedUpdated);
		assertTrue(markdownPresignedUpdated.toString().indexOf("markdownKey") > 0);
		Boolean redirectMarkdown = Boolean.FALSE;
		markdownPresignedUpdated  = entityServletHelper.getV2WikiMarkdownFileURL(adminUserId, key, new Long(0), redirectMarkdown);
		assertNotNull(markdownPresignedUpdated);
		assertTrue(markdownPresignedUpdated.toString().indexOf("markdownKey") > 0);
		
		// Get history (there should be three snapshots returned)
		PaginatedResults<V2WikiHistorySnapshot> historyResults = entityServletHelper.getV2WikiHistory(key, adminUserId, new Long(0), new Long(10));
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
		FileHandleResults oldHandles = entityServletHelper.getV2WikiFileHandles(adminUserId, key, new Long(0));
		assertNotNull(oldHandles);
		assertNotNull(oldHandles.getList());
		assertEquals(0, oldHandles.getList().size());
		
		Long versionToRestore = new Long(1);
		// Get an older version
		V2WikiPage versionOne = entityServletHelper.getV2WikiPage(key, adminUserId, versionToRestore);
		assertEquals(markdownTwoHandle.getId(), versionOne.getMarkdownFileHandleId());
		assertEquals(1, versionOne.getAttachmentFileHandleIds().size());
		// Get its attachment's URL
		URL versionOneAttachment = entityServletHelper.getV2WikiAttachmentFileURL(adminUserId, key, fileOneHandle.getFileName(), null, new Long(1));
		assertNotNull(versionOneAttachment);
		assertTrue(versionOneAttachment.toString().indexOf("mainFileKey") > 0);
		
		assertEquals("Version 1 title", versionOne.getTitle());
		assertEquals(cloneUpdated.getModifiedOn(), versionOne.getModifiedOn());
		
		// Restore wiki to version 1 which had markdownTwo and one file attachment, and a title of "Version 1 title"
		String currentEtag3 = cloneUpdated2.getEtag();
		V2WikiPage restored = entityServletHelper.restoreWikiPage(adminUserId, ownerId, ownerType, cloneUpdated2, versionToRestore);
		assertNotNull(restored);
		assertFalse("The etag should have changed from the restore", currentEtag3.equals(restored.getEtag()));
		assertEquals(cloneUpdated2.getCreatedBy(), restored.getCreatedBy());
		assertEquals(cloneUpdated2.getCreatedOn(), restored.getCreatedOn());
		assertEquals(restored.getMarkdownFileHandleId(), markdownTwoHandle.getId());
		assertEquals(restored.getAttachmentFileHandleIds().size(), 1);
		assertEquals(clone.getTitle(), restored.getTitle());

		// Add a child wiki
		V2WikiPage child = new V2WikiPage();
		child.setTitle("Child");
		child.setMarkdownFileHandleId(markdownOneHandle.getId());
		child.setParentWikiId(wiki.getId());
		child.setAttachmentFileHandleIds(new LinkedList<String>());
		// Note, we are adding a file handle with a preview.
		// Both the S3FileHandle and its Preview should be returned from getWikiFileHandles()
		child.getAttachmentFileHandleIds().add(fileOneHandle.getId());
		
		// Create child!
		child = entityServletHelper.createV2WikiPage(adminUserId, ownerId, ownerType, child);
		assertNotNull(child);
		assertNotNull(child.getId());
		WikiPageKey childKey = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, child.getId());
		toDelete.add(childKey);
		// List the hierarchy
		PaginatedResults<V2WikiHeader> paginated = entityServletHelper.getV2WikiHeaderTree(adminUserId, ownerId, ownerType);
		assertNotNull(paginated);
		assertNotNull(paginated.getResults());
		assertEquals(2, paginated.getResults().size());
		
		// check the root header.
		V2WikiHeader rootHeader = paginated.getResults().get(0);
		assertEquals(clone.getId(), rootHeader.getId());
		assertEquals(clone.getTitle(), rootHeader.getTitle());
		assertEquals(null, rootHeader.getParentId());
		
		// Check the child header
		V2WikiHeader childHeader =  paginated.getResults().get(1);
		assertEquals(child.getId(), childHeader.getId());
		assertEquals(child.getTitle(), childHeader.getTitle());
		assertEquals(wiki.getId(), childHeader.getParentId());
		
		// Check that we can get the FileHandles	
		FileHandleResults handles = entityServletHelper.getV2WikiFileHandles(adminUserId, childKey, null);
		assertNotNull(handles);
		assertNotNull(handles.getList());
		assertEquals(2, handles.getList().size());
		// The first should be the S3FileHandle, the second should be the Preview.
		assertEquals(fileOneHandle.getId(), handles.getList().get(0).getId());
		assertEquals(fileOnePreviewHandle.getId(), handles.getList().get(1).getId());
		
		// PLFM-2727: restore child wiki
		V2WikiPage clonedChild = entityServletHelper.getV2WikiPage(childKey, adminUserId, null);
		// Create a new version of the child
		clonedChild.setMarkdownFileHandleId(markdownTwoHandle.getId()); // Change the markdown
		clonedChild.getAttachmentFileHandleIds().clear(); // Remove the attachment
		assertEquals(0, clonedChild.getAttachmentFileHandleIds().size());
		clonedChild.setTitle("Child Version 1 title"); // Change the title
		String childCurrentEtag1 = clonedChild.getEtag();
		V2WikiPage childUpdated = entityServletHelper.updateWikiPage(adminUserId, ownerId, ownerType, clonedChild);
		assertNotNull(childUpdated);
		assertEquals("Child Version 1 title", childUpdated.getTitle());
		assertEquals(markdownTwoHandle.getId(), childUpdated.getMarkdownFileHandleId());
		assertEquals(0, childUpdated.getAttachmentFileHandleIds().size());
		assertFalse("The etag should have changed from the update", childCurrentEtag1.equals(childUpdated.getEtag()));
		// Get history
		PaginatedResults<V2WikiHistorySnapshot> childHistoryResults = entityServletHelper.getV2WikiHistory(childKey, adminUserId, new Long(0), new Long(10));
		assertNotNull(childHistoryResults);
		List<V2WikiHistorySnapshot> childSnapshots = childHistoryResults.getResults();
		assertNotNull(childSnapshots);
		assertEquals(2, childSnapshots.size());
		assertEquals("1", childSnapshots.get(0).getVersion());
		assertEquals("0", childSnapshots.get(1).getVersion());
		// Restore wiki to version 0 which had markdownOne and one file attachment, and a title of "Child"
		String childCurrentEtag2 = childUpdated.getEtag();
		V2WikiPage childRestored = entityServletHelper.restoreWikiPage(adminUserId, ownerId, ownerType, childUpdated, 0L);
		assertNotNull(childRestored);
		assertFalse("The etag should have changed from the restore", childCurrentEtag2.equals(childRestored.getEtag()));
		assertEquals(clonedChild.getCreatedBy(), childRestored.getCreatedBy());
		assertEquals(clonedChild.getCreatedOn(), childRestored.getCreatedOn());
		assertEquals(childRestored.getMarkdownFileHandleId(), markdownOneHandle.getId());
		assertEquals(childRestored.getAttachmentFileHandleIds().size(), 1);
		assertEquals(childRestored.getAttachmentFileHandleIds().get(0), fileOneHandle.getId());
		assertEquals(child.getTitle(), childRestored.getTitle());	
	}
	
	@Test
	public void testWikiOrderHintReadUpdateForOwnerObject() throws Exception {
		// create an entity
		entity = new Project();
		entity.setEntityType(Project.class.getName());
		entity = (Project) entityServletHelper.createEntity(entity, adminUserId, null);
		
		String ownerId = entity.getId();
		ObjectType ownerType = ObjectType.ENTITY;
		
		// Make wiki page
		V2WikiPage wiki = new V2WikiPage();
		wiki.setTitle("testWikiOrderHintReadUpdateForOwnerObject-"+ownerId+"-"+ownerType);
		wiki.setMarkdownFileHandleId(markdownOneHandle.getId());
		wiki.setAttachmentFileHandleIds(new LinkedList<String>());
		wiki = entityServletHelper.createV2WikiPage(adminUserId, ownerId, ownerType, wiki);
		assertNotNull(wiki);
		
		WikiPageKey wikiKey = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wiki.getId());
		toDelete.add(wikiKey);
		
		// Get OrderHint for the created project (should have a null order hint).
		V2WikiOrderHint orderHint = entityServletHelper.getWikiOrderHint(adminUserId, ownerId, ownerType);
		
		// Order hint has not been set yet.
		assertNull(orderHint.getIdList());
		
		List<String> orderHintList = Arrays.asList(new String[] {"A", "B", "C", "D"});
		
		orderHint.setIdList(orderHintList);
		
		V2WikiOrderHint updatedOrderHint = entityServletHelper.updateWikiOrderHint(adminUserId, orderHint);
		
		assertNotNull(updatedOrderHint.getIdList());
		assertTrue(orderHintList.equals(updatedOrderHint.getIdList()));
		
		// Get the updated order hint (make sure it was recorded).
		V2WikiOrderHint postUpdateGetOrderHint = entityServletHelper.getWikiOrderHint(adminUserId, ownerId, ownerType);
		
		assertTrue(orderHintList.equals(postUpdateGetOrderHint.getIdList()));
		
	}

}
