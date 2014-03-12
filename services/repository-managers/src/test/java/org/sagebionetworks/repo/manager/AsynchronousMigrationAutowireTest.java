package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.wiki.V2WikiManager;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AsynchronousMigrationAutowireTest {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private V2WikiManager wikiManager;
	
	@Autowired
	private S3TokenManager s3TokenManager;
	
	@Autowired
	private AsynchronousDAO asynchronousDAO;
	
	@Autowired
	private V2WikiPageDao wikiPageDao;

	private List<String> toDelete;
	
	private Long adminUserId;
	UserInfo adminUserInfo;
	private Project project;
	private V2WikiPage wikiPage;
	
	@Before
	public void before() throws Exception{
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserInfo = userManager.getUserInfo(adminUserId);
		toDelete = new ArrayList<String>();
		
		// Create a project
		project = new Project();
		project.setName("AsynchronousMigrationAutowireTest.project");
		project.setDescription("This string should end up in the wikipage");
		
		// Save the project
		String id = entityManager.createEntity(adminUserInfo, project, null);
		toDelete.add(id);
		project = entityManager.getEntity(adminUserInfo, id, Project.class);
		
		// Add an attachment
		S3AttachmentToken at = new S3AttachmentToken();
		at.setMd5("3b54d27920bfe247442f8005dd071664");
		at.setContentType("application/json");
		at.setFileName("foo.bar");
		S3AttachmentToken token = s3TokenManager.createS3AttachmentToken(adminUserId, project.getId(), at);

		AttachmentData ad = new AttachmentData();
		ad.setContentType("application/json");
		ad.setName("foo.bar");
		ad.setMd5(token.getMd5());
		ad.setTokenId(token.getTokenId());
		
		// Add it to the project
		project.setAttachments(new LinkedList<AttachmentData>());
		project.getAttachments().add(ad);
		entityManager.updateEntity(adminUserInfo, project, false, null);
		project = entityManager.getEntity(adminUserInfo, id, Project.class);
	}
	
	@After
	public void after() throws Exception {
		for (String id: toDelete) {
			try {
				entityManager.deleteEntity(adminUserInfo, id);
			} catch(Exception e) {}
		}
	}
	
	@Ignore // This test is currently not needed now that we have mirrored WikiPages for projects and folders
	@Test
	public void testPLFM_1709() throws NotFoundException, IOException{
		// Before we start
		// Trigger the creation of the mirror
		asynchronousDAO.createEntity(project.getId());
		// Now we should now have a wiki page for this project.
		wikiPage = wikiManager.getRootWikiPage(adminUserInfo, project.getId(), ObjectType.ENTITY);
		assertNotNull(wikiPage);
		WikiPageKey key = new WikiPageKey(project.getId(), ObjectType.ENTITY, wikiPage.getId());

		String markdownString = wikiPageDao.getMarkdown(key, null);
		
		assertEquals(project.getDescription(), markdownString);
		assertNotNull(wikiPage.getAttachmentFileHandleIds());
		assertEquals(1, wikiPage.getAttachmentFileHandleIds().size());
		FileHandleResults results = wikiManager.getAttachmentFileHandles(adminUserInfo, key, null);
		assertNotNull(results);
		assertEquals(1, results.getList().size());
		S3FileHandle handle = (S3FileHandle) results.getList().get(0);
		// The handle should match the project's attachment
		AttachmentData ad = project.getAttachments().get(0);
		assertEquals(ad.getContentType(), handle.getContentType());
		assertEquals(ad.getMd5(), handle.getContentMd5());
		assertEquals(ad.getName(), handle.getFileName());
		System.out.println(handle.getKey());
		
	}
}
