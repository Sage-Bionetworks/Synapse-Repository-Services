package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.entity.FileEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.wiki.WikiManager;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AsynchronousMigrationAutowireTest {
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private WikiManager wikiManager;
	
	@Autowired
	private S3TokenManager s3TokenManager;
	
	@Autowired
	private AsynchronousDAO asynchronousDAO;
	
	@Autowired
	public UserProvider testUserProvider;

	private List<String> toDelete;
	
	private UserInfo userInfo;
	
	Project project;
	Folder folder;
	FileEntity file;
	WikiPage wikiPage;
	
	@Before
	public void before() throws Exception{
		assertNotNull(entityManager);
		assertNotNull(testUserProvider);
		userInfo = testUserProvider.getTestAdminUserInfo();
		toDelete = new ArrayList<String>();
		// Create a project
		project = new Project();
		project.setName("AsynchronousMigrationAutowireTest.project");
		project.setDescription("This string should end up in the wikipage");
		// Save the project
		String id = entityManager.createEntity(userInfo, project, null);
		toDelete.add(id);
		project = entityManager.getEntity(userInfo, id, Project.class);
		// Add an attachment
		S3AttachmentToken at = new S3AttachmentToken();
		at.setMd5("3b54d27920bfe247442f8005dd071664");
		at.setContentType("application/json");
		at.setFileName("foo.bar");
		S3AttachmentToken token = s3TokenManager.createS3AttachmentToken(userInfo.getIndividualGroup().getId(), project.getId(), at);

		AttachmentData ad = new AttachmentData();
		ad.setContentType("application/json");
		ad.setName("foo.bar");
		ad.setMd5(token.getMd5());
		ad.setTokenId(token.getTokenId());
		// Add it to the project
		project.setAttachments(new LinkedList<AttachmentData>());
		project.getAttachments().add(ad);
		entityManager.updateEntity(userInfo, project, false, null);
		project = entityManager.getEntity(userInfo, id, Project.class);
	}
	
	@After
	public void after(){
		if(entityManager != null && toDelete != null){
			for(String id: toDelete){
				try{
					entityManager.deleteEntity(userInfo, id);
				}catch(Exception e){}
			}
		}
	}
	
	@Ignore // This test is currently not needed now that we have mirrored WikiPages for projects and folders
	@Test
	public void testPLFM_1709() throws NotFoundException{
		// Before we start
		// Trigger the creation of the mirror
		asynchronousDAO.createEntity(project.getId());
		// Now we should now have a wiki page for this project.
		wikiPage = wikiManager.getRootWikiPage(userInfo, project.getId(), ObjectType.ENTITY);
		assertNotNull(wikiPage);
		WikiPageKey key = new WikiPageKey(project.getId(), ObjectType.ENTITY, wikiPage.getId());

		assertEquals(project.getDescription(), wikiPage.getMarkdown());
		assertNotNull(wikiPage.getAttachmentFileHandleIds());
		assertEquals(1, wikiPage.getAttachmentFileHandleIds().size());
		FileHandleResults results = wikiManager.getAttachmentFileHandles(userInfo, key);
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
