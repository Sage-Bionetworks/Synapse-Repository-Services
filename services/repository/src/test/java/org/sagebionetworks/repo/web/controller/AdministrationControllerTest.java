package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.migration.WikiMigrationResult;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AdministrationControllerTest {

	@Autowired
	public UserManager userManager;
	
	@Autowired
	public NodeManager nodeManager;
	
	@Autowired
	private EntityServletTestHelper entityServletHelper;
	
	@Autowired
	private V2WikiPageDao v2wikiPageDAO;
	
	@Autowired
	private WikiPageDao wikiPageDao;
	
	@Autowired
	private FileHandleDao fileMetadataDao;
	
	@Autowired
	private AmazonS3Client s3Client;
	
	private static HttpServlet dispatchServlet;
	
	@Autowired
	private StackStatusDao stackStatusDao;
	
	private List<String> toDelete;
	private List<WikiPageKey> wikisToDelete;
	private UserInfo adminUserInfo;
	private Project entity;

	@Before
	public void before() throws DatastoreException, NotFoundException {
		toDelete = new ArrayList<String>();
		wikisToDelete = new ArrayList<WikiPageKey>();
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}
	
	@BeforeClass
	public static void beforeClass() throws ServletException {
		dispatchServlet = DispatchServletSingleton.getInstance();
	}

	@After
	public void after() throws UnauthorizedException {
		// Always restore the status to read-write
		StackStatus status = new StackStatus();
		status.setStatus(StatusEnum.READ_WRITE);
		stackStatusDao.updateStatus(status);
		// Delete starting from children to avoid losing
		// resources on cascade delete
		for(int i = wikisToDelete.size() - 1; i >= 0; i--) {
			try {
				WikiPageKey key = wikisToDelete.get(i);
				V2WikiPage wiki = v2wikiPageDAO.get(key, null);
				String markdownHandleId = wiki.getMarkdownFileHandleId();
				S3FileHandle markdownHandle = (S3FileHandle) fileMetadataDao.get(markdownHandleId);
				s3Client.deleteObject(markdownHandle.getBucketName(), markdownHandle.getKey());
				fileMetadataDao.delete(markdownHandleId);
				wikiPageDao.delete(key);
				v2wikiPageDAO.delete(key);
			} catch (Exception e) {
				// nothing to do here
			}
		}
		if(entity != null){
			try {
				nodeManager.delete(adminUserInfo, entity.getId());
			} catch (DatastoreException e) {
				// nothing to do here
			} catch (NotFoundException e) {
				// nothing to do here
			}	
		}
		
		if (nodeManager != null && toDelete != null) {
			for (String idToDelete : toDelete) {
				try {
					nodeManager.delete(adminUserInfo, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
	}
	
	@Test
	public void testGetStackStatus() throws Exception {
		// Make sure we can get the stack status
		StackStatus status = ServletTestHelper.getStackStatus(dispatchServlet);
		assertNotNull(status);
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
	}
	
	@Test
	public void testUpdateStatus() throws Exception {
		// Make sure we can get the stack status
		StackStatus status = ServletTestHelper.getStackStatus(dispatchServlet);
		assertNotNull(status);
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
		// Make sure we can update the status
		status.setPendingMaintenanceMessage("AdministrationControllerTest.testUpdateStatus");
		StackStatus back = ServletTestHelper.updateStackStatus(dispatchServlet, adminUserInfo.getIndividualGroup().getName(), status);
		assertEquals(status, back);
	}
	
	@Test
	public void testGetAndUpdateStatusWhenDown() throws Exception {
		// Make sure we can get the status when down.
		StackStatus setDown = new StackStatus();
		setDown.setStatus(StatusEnum.DOWN);
		setDown.setCurrentMessage("Synapse is going down for a test: AdministrationControllerTest.testGetStatusWhenDown");
		StackStatus back = ServletTestHelper.updateStackStatus(dispatchServlet, adminUserInfo.getIndividualGroup().getName(), setDown);
		assertEquals(setDown, back);
		// Make sure we can still get the status
		StackStatus current = ServletTestHelper.getStackStatus(dispatchServlet);
		assertEquals(setDown, current);
		
		// Now make sure we can turn it back on when down.
		setDown.setStatus(StatusEnum.READ_WRITE);
		setDown.setCurrentMessage(null);
		back = ServletTestHelper.updateStackStatus(dispatchServlet, adminUserInfo.getIndividualGroup().getName(), setDown);
		assertEquals(setDown, back);
	}

	@Test
	public void testMigrateWikis() throws Exception {
		// create an entity
		entity = new Project();
		entity.setEntityType(Project.class.getName());
		entity = (Project) entityServletHelper.createEntity(entity, adminUserInfo.getIndividualGroup().getName(), null);
		createWikiPages(entity.getId());
		
		Map<String, String> extraParams = new HashMap<String, String>();
		extraParams.put("offset", "0");
		extraParams.put("limit", "5");
		
		PaginatedResults<WikiMigrationResult> results = 
			ServletTestHelper.migrateWikisToV2(dispatchServlet, adminUserInfo.getIndividualGroup().getName(), extraParams);
		assertEquals(2, results.getResults().size());
	}
	
	private void createWikiPages(String ownerId) throws NotFoundException {
		ObjectType ownerType = ObjectType.ENTITY;
		// Create wiki pages with the DAO to avoid the translation bridge 
		// which will create V2 wiki pages.
		WikiPage page = new WikiPage();
		page.setId("1");
		page.setCreatedBy(adminUserInfo.getIndividualGroup().getId());
		page.setModifiedBy(adminUserInfo.getIndividualGroup().getId());
		page.setMarkdown("markdown1");
		page.setTitle("title1");
		page.setAttachmentFileHandleIds(new ArrayList<String>());
		page.setParentWikiId(null);
		WikiPage result = wikiPageDao.create(page, new HashMap<String, FileHandle>(), ownerId, ownerType);
		WikiPageKey parentKey = new WikiPageKey(ownerId, ownerType, page.getId());
		wikisToDelete.add(parentKey);
		
		// Child
		WikiPage pageChild = new WikiPage();
		pageChild.setId("2");
		pageChild.setCreatedBy(adminUserInfo.getIndividualGroup().getId());
		pageChild.setModifiedBy(adminUserInfo.getIndividualGroup().getId());
		pageChild.setMarkdown("markdown2");
		pageChild.setTitle("title2");
		pageChild.setAttachmentFileHandleIds(new ArrayList<String>());
		pageChild.setParentWikiId(page.getId());
		WikiPage result2 = wikiPageDao.create(pageChild, new HashMap<String, FileHandle>(), ownerId, ownerType);
		WikiPageKey childKey = new WikiPageKey(ownerId, ownerType, pageChild.getId());
		wikisToDelete.add(childKey);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testMigrateWikisAsNonAdmit() throws Exception {
		Map<String, String> extraParams = new HashMap<String, String>();
		extraParams.put("offset", "0");
		extraParams.put("limit", "10");
		
		// Not an admin, so this should fail with a 403
		String anonUsername = userManager.getGroupName(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString());
		ServletTestHelper.migrateWikisToV2(dispatchServlet, anonUsername, extraParams);
	}
}
