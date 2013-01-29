package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 
 * @author jmhill
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class WikiControllerTest {
	
	@Autowired
	EntityServletTestHelper entityServletHelper;
	@Autowired
	UserManager userManager;
	
	@Autowired
	FileMetadataDao fileMetadataDao;
	
	private String userName;
	private String ownerId;
	
	Project entity;
	Competition competition;
	List<WikiPageKey> toDelete;
	S3FileHandle handleOne;
	PreviewFileHandle handleTwo;
	
	@Before
	public void before() throws Exception{
		// get user IDs
		userName = TestUserDAO.TEST_USER_NAME;
		ownerId = userManager.getUserInfo(userName).getIndividualGroup().getId();
		toDelete = new LinkedList<WikiPageKey>();
		// Create a file handle
		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(ownerId);
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("key");
		handleOne.setEtag("etag");
		handleOne = fileMetadataDao.createFile(handleOne);
		// Create a preview
		handleTwo = new PreviewFileHandle();
		handleTwo.setCreatedBy(ownerId);
		handleTwo.setCreatedOn(new Date());
		handleTwo.setBucketName("bucket");
		handleTwo.setKey("key");
		handleTwo.setEtag("etag");
		handleTwo = fileMetadataDao.createFile(handleTwo);
		// Set two as the preview of one
		fileMetadataDao.setPreviewId(handleOne.getId(), handleTwo.getId());
	}
	
	
	@After
	public void after() throws Exception{
		// Delete the project
		if(entity != null){
			entityServletHelper.deleteEntity(entity.getId(), userName);
		}
		if(competition != null){
			entityServletHelper.deleteCompetition(competition.getId(), userName);
		}
		for(WikiPageKey key: toDelete){
			entityServletHelper.deleteWikiPage(key, userName);
		}
		if(handleOne != null && handleOne.getId() != null){
			fileMetadataDao.delete(handleOne.getId());
		}
		if(handleTwo != null && handleTwo.getId() != null){
			fileMetadataDao.delete(handleTwo.getId());
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
		competition = new Competition();
		competition.setName("testCompetitionWikiCRUD");
		competition.setContentSource("not sure what this is");
		competition.setDescription("a test descrption");
		competition.setStatus(CompetitionStatus.OPEN);
		competition = entityServletHelper.createCompetition(competition, userName);
		// Test all wiki CRUD for an entity
		doWikiCRUDForOwnerObject(competition.getId(), ObjectType.COMPETITION);
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
		wiki = entityServletHelper.createWikiPage(userName, ownerId, ownerType, wiki);
		assertNotNull(wiki);
		assertNotNull(wiki.getId());
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, wiki.getId());
		toDelete.add(key);
		assertNotNull(wiki.getEtag());
		assertNotNull(ownerId, wiki.getModifiedBy());
		assertNotNull(ownerId, wiki.getCreatedBy());
		// Get the wiki page.
		WikiPage clone = entityServletHelper.getWikiPage(key, userName);
		assertNotNull(clone);
		System.out.println(clone);
		assertEquals(wiki, clone);
		// Update the wiki
		clone.setTitle("updated title");
		String currentEtag = clone.getEtag();
		// update
		WikiPage cloneUpdated = entityServletHelper.updateWikiPage(userName, ownerId, ownerType, clone);
		assertNotNull(cloneUpdated);
		assertEquals("updated title", cloneUpdated.getTitle());
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
		child = entityServletHelper.createWikiPage(userName, ownerId, ownerType, child);
		assertNotNull(child);
		assertNotNull(child.getId());
		WikiPageKey childKey = new WikiPageKey(ownerId, ownerType, child.getId());
		toDelete.add(childKey);
		// List the hierarchy
		PaginatedResults<WikiHeader> paginated = entityServletHelper.getWikiHeaderTree(userName, ownerId, ownerType);
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
		FileHandleResults handles = entityServletHelper.getWikiFileHandles(userName, childKey);
		assertNotNull(handles);
		assertNotNull(handles.getList());
		assertEquals(2, handles.getList().size());
		// The first should be the S3FileHandle, the second should be the Preview.
		assertEquals(handleOne.getId(), handles.getList().get(0).getId());
		assertEquals(handleTwo.getId(), handles.getList().get(1).getId());
		
		// Now delete the wiki
		entityServletHelper.deleteWikiPage(key, userName);
		try{
			entityServletHelper.getWikiPage(key, userName);
			fail("The wiki should have been deleted");
		}catch(ServletTestHelperException e){
			// this is expected
		}
		// the child should be delete as well
		try{
			entityServletHelper.getWikiPage(childKey, userName);
			fail("The wiki should have been deleted");
		}catch(ServletTestHelperException e){
			// this is expected
		}
	}
}
