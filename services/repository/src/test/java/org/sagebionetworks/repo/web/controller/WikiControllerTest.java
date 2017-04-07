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
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;

/**
 * 
 * @author jmhill
 *
 */
public class WikiControllerTest extends AbstractAutowiredControllerTestBase {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	private V2WikiPageDao v2WikiPageDao;
	
	@Autowired
	private AmazonS3Client s3Client;

	@Autowired
	private IdGenerator idGenerator;
	
	private Long adminUserId;
	private String adminUserIdString;
	
	private Project entity;
	private Evaluation evaluation;
	private List<WikiPageKey> toDelete;
	private S3FileHandle handleOne;
	private PreviewFileHandle handleTwo;
	
	@Before
	public void before() throws Exception{
		// get user IDs
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserIdString = adminUserId.toString();

		toDelete = new LinkedList<WikiPageKey>();
		// Create a file handle
		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(adminUserIdString);
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("mainFileKey");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handleOne.setEtag(UUID.randomUUID().toString());
		// Create a preview
		handleTwo = new PreviewFileHandle();
		handleTwo.setCreatedBy(adminUserIdString);
		handleTwo.setCreatedOn(new Date());
		handleTwo.setBucketName("bucket");
		handleTwo.setKey("previewFileKey");
		handleTwo.setEtag("etag");
		handleTwo.setFileName("bar.txt");
		handleTwo.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handleTwo.setEtag(UUID.randomUUID().toString());

		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		fileHandleToCreate.add(handleOne);
		fileHandleToCreate.add(handleTwo);
		fileHandleDao.createBatch(fileHandleToCreate);

		handleOne = (S3FileHandle) fileHandleDao.get(handleOne.getId());
		handleTwo = (PreviewFileHandle) fileHandleDao.get(handleTwo.getId());
		// Set two as the preview of one
		fileHandleDao.setPreviewId(handleOne.getId(), handleTwo.getId());
	}
	
	
	@After
	public void after() throws Exception{
		for(WikiPageKey key: toDelete){
			entityServletHelper.deleteWikiPage(key, adminUserId);
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
		if(handleOne != null && handleOne.getId() != null){
			fileHandleDao.delete(handleOne.getId());
		}
		if(handleTwo != null && handleTwo.getId() != null){
			fileHandleDao.delete(handleTwo.getId());
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
		WikiPage wiki = new WikiPage();
		wiki.setTitle("testCreateEntityWikiRoundTrip-"+ownerId+"-"+ownerType);
		wiki.setMarkdown("markdown");
		// Create it!
		wiki = entityServletHelper.createWikiPage(adminUserId, ownerId, ownerType, wiki);
		assertNotNull(wiki);
		assertNotNull(wiki.getId());
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wiki.getId());
		toDelete.add(key);
		assertNotNull(wiki.getEtag());
		assertNotNull(ownerId, wiki.getModifiedBy());
		assertNotNull(ownerId, wiki.getCreatedBy());
		assertEquals(1, v2WikiPageDao.getCount());
		// Get the wiki page.
		WikiPage clone = entityServletHelper.getWikiPage(key, adminUserId);
		assertNotNull(clone);
		System.out.println(clone);
		assertEquals(wiki, clone);
		// Get the root wiki
		WikiPage root = entityServletHelper.getRootWikiPage(ownerId, ownerType, adminUserId);
		// The root should match the clone
		assertEquals(clone, root);
		
		// Save the current file handle of the mirror wiki which will be lost when updating
		V2WikiPage v2Mirror = v2WikiPageDao.get(key, null);
		String abandonedFileHandleId = v2Mirror.getMarkdownFileHandleId();
		
		// Update the wiki
		clone.setTitle("updated title");
		String currentEtag = clone.getEtag();
		// update
		WikiPage cloneUpdated = entityServletHelper.updateWikiPage(adminUserId, ownerId, ownerType, clone);
		assertNotNull(cloneUpdated);
		// Title should be updated. V2 should have mirrored it too.
		assertEquals("updated title", cloneUpdated.getTitle());
		assertEquals("updated title", v2WikiPageDao.get(key, null).getTitle());
		assertFalse("The etag should have changed from the update", currentEtag.equals(cloneUpdated.getId()));
		
		// Add a child wiki
		WikiPage child = new WikiPage();
		child.setTitle("Child");
		child.setMarkdown("child markdown");
		child.setParentWikiId(wiki.getId());
		child.setAttachmentFileHandleIds(new LinkedList<String>());
		// Note, we are adding a file handle with a preview.
		// Both the S3FileHandle and its Preview should be returned from getWikiFileHandles()
		child.getAttachmentFileHandleIds().add(handleOne.getId());
		// Create it!
		child = entityServletHelper.createWikiPage(adminUserId, ownerId, ownerType, child);
		assertNotNull(child);
		assertNotNull(child.getId());
		WikiPageKey childKey = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, child.getId());
		toDelete.add(childKey);
		assertEquals(2, v2WikiPageDao.getCount());
		// List the hierarchy
		PaginatedResults<WikiHeader> paginated = entityServletHelper.getWikiHeaderTree(adminUserId, ownerId, ownerType);
		assertNotNull(paginated);
		assertNotNull(paginated.getResults());
		assertEquals(2, paginated.getResults().size());
		assertEquals(2l, paginated.getTotalNumberOfResults());
		// check the root header.
		WikiHeader rootHeader = paginated.getResults().get(0);
		assertEquals(cloneUpdated.getId(), rootHeader.getId());
		assertEquals(cloneUpdated.getTitle(), rootHeader.getTitle());
		assertEquals(null, rootHeader.getParentId());
		// Check the child header
		WikiHeader childeHeader =  paginated.getResults().get(1);
		assertEquals(childeHeader.getId(), childeHeader.getId());
		assertEquals(childeHeader.getTitle(), childeHeader.getTitle());
		assertEquals(wiki.getId(), childeHeader.getParentId());
		// Check that we can get the FileHandles for each wiki		
		FileHandleResults handles = entityServletHelper.getWikiFileHandles(adminUserId, childKey);
		assertNotNull(handles);
		assertNotNull(handles.getList());
		assertEquals(2, handles.getList().size());
		// The first should be the S3FileHandle, the second should be the Preview.
		assertEquals(handleOne.getId(), handles.getList().get(0).getId());
		assertEquals(handleTwo.getId(), handles.getList().get(1).getId());
		
		// Get the presigned URL for the first file
		URL presigned  = entityServletHelper.getWikiAttachmentFileURL(adminUserId, childKey, handleOne.getFileName(), null);
		assertNotNull(presigned);
		assertTrue(presigned.toString().indexOf("mainFileKey") > 0);
		System.out.println(presigned);
		// Get the preview presigned URL.
		presigned  = entityServletHelper.getWikiAttachmentPreviewFileURL(adminUserId, childKey, handleOne.getFileName(), null);
		assertNotNull(presigned);
		assertTrue(presigned.toString().indexOf("previewFileKey") > 0);
		System.out.println(presigned);
		
		// Make sure we can get the URLs without a redirect
		Boolean redirect = Boolean.FALSE;
		presigned  = entityServletHelper.getWikiAttachmentFileURL(adminUserId, childKey, handleOne.getFileName(), redirect);
		assertNotNull(presigned);
		assertTrue(presigned.toString().indexOf("mainFileKey") > 0);
		System.out.println(presigned);
		// again without the redirct
		presigned  = entityServletHelper.getWikiAttachmentPreviewFileURL(adminUserId, childKey, handleOne.getFileName(), redirect);
		assertNotNull(presigned);
		assertTrue(presigned.toString().indexOf("previewFileKey") > 0);
		System.out.println(presigned);
		
		// Delete file handles etc made when creating V2 wikis, starting with abandoned handles
		// Start with child so resources aren't lost when deleting the parent first
		S3FileHandle abandonedHandle = (S3FileHandle) fileHandleDao.get(abandonedFileHandleId);
		s3Client.deleteObject(abandonedHandle.getBucketName(), abandonedHandle.getKey());
		fileHandleDao.delete(abandonedFileHandleId);
		for(int i = toDelete.size() - 1; i >= 0; i--) {
			V2WikiPage wikiPage = v2WikiPageDao.get(toDelete.get(i), null);
			String markdownHandleId = wikiPage.getMarkdownFileHandleId();
			S3FileHandle markdownHandle = (S3FileHandle) fileHandleDao.get(markdownHandleId);
			s3Client.deleteObject(markdownHandle.getBucketName(), markdownHandle.getKey());
			fileHandleDao.delete(markdownHandleId);
		}
		
		// Now delete the wiki
		entityServletHelper.deleteWikiPage(key, adminUserId);
		try {
			entityServletHelper.getWikiPage(key, adminUserId);
			fail("The wiki should have been deleted");
		} catch (NotFoundException e) {
			// this is expected
		}
		// the child should be deleted as well
		try {
			entityServletHelper.getWikiPage(childKey, adminUserId);
			fail("The wiki should have been deleted");
		} catch (NotFoundException e) {
			// this is expected
		}
		assertEquals(0, v2WikiPageDao.getCount());
	}
}
