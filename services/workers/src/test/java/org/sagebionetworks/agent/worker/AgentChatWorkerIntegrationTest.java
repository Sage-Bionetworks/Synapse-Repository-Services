package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.UpdateAgentSessionRequest;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.service.AgentService;
import org.sagebionetworks.repo.service.EntityService;
import org.sagebionetworks.repo.service.WikiService;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AgentChatWorkerIntegrationTest {

	public static final long MAX_WAIT_MS = 60_000;

	@Autowired
	private AgentService agentService;

	@Autowired
	private EntityService entityService;

	@Autowired
	private WikiService wikiService;

	@Autowired
	private UserManager userManager;

	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;

	@Autowired
	private SynapseS3Client s3Client;

	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private FileHandleManager fileHandleManager;

    private UserInfo admin;
    private List<String> entitiesToDelete = new ArrayList<>();
    private List<S3FileHandle> fileHandlesToDelete = new ArrayList<>();

	@BeforeEach
	public void before() {

		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}

	@AfterEach
	public void after() {
		// delete project, folder and file entities
		for (final String id : Lists.reverse(entitiesToDelete)) {
			entityService.deleteEntity(admin.getId(), id);
		}

		if (fileHandlesToDelete != null && s3Client != null) {
			// Delete file handle created
			for (S3FileHandle meta : fileHandlesToDelete) {
				// delete the file from S3.
				s3Client.deleteObject(meta.getBucketName(), meta.getKey());
				if (meta.getId() != null) {
					// We also need to delete the data from the database
					fileHandleDao.delete(meta.getId());
				}
			}
		}
	}

    @Test
    public void testChatWithEmptyRequest() throws AssertionError, AsynchJobFailedException {

		AgentSession session = agentService.createSession(admin.getId(),
				new CreateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.PUBLICLY_ACCESSIBLE));

		assertNotNull(session);
		// an empty request will return an empty response.
		String chatRequest = "";

		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new AgentChatRequest().setSessionId(session.getSessionId()).setChatText(chatRequest),
				(AgentChatResponse response) -> {
					assertNotNull(response);
					assertEquals(session.getSessionId(), response.getSessionId());
					assertEquals("", response.getResponseText());
				}, MAX_WAIT_MS).getResponse();

	}

	@Test
	public void testGetEntityMetadataHandler() throws AssertionError, AsynchJobFailedException {
		Project project = entityService.createEntity(admin.getId(), new Project().setName(UUID.randomUUID().toString()),
				null);
		entitiesToDelete.add(project.getId());

		AgentSession session = agentService.createSession(admin.getId(),
				new CreateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.READ_YOUR_PRIVATE_DATA));

		assertNotNull(session);
		// an empty request will return an empty response.
		String chatRequest = "What is the name of entity: " + project.getId();

		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new AgentChatRequest().setSessionId(session.getSessionId()).setChatText(chatRequest),
				(AgentChatResponse response) -> {
					assertNotNull(response);
					assertEquals(session.getSessionId(), response.getSessionId());
					assertNotNull(response.getResponseText());
					System.out.println(response.getResponseText());
					assertTrue(response.getResponseText().contains(project.getName()));
				}, MAX_WAIT_MS).getResponse();

	}

	@Test
	public void testGetEntityChildrenHandler() throws AssertionError, AsynchJobFailedException {
		Project project = entityService.createEntity(admin.getId(), new Project().setName(UUID.randomUUID().toString()),
				null);
		entitiesToDelete.add(project.getId());

		Folder f1 = entityService.createEntity(admin.getId(), new Folder().setName("f1").setParentId(project.getId()),
				null);
		entitiesToDelete.add(f1.getId());

		Folder f2 = entityService.createEntity(admin.getId(), new Folder().setName("f2").setParentId(project.getId()),
				null);
		entitiesToDelete.add(f2.getId());

		AgentSession session = agentService.createSession(admin.getId(),
				new CreateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.READ_YOUR_PRIVATE_DATA));

		assertNotNull(session);
		// an empty request will return an empty response.
		String chatRequest = "What are the names and synIDs of the children of project: " + project.getId();

		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new AgentChatRequest().setSessionId(session.getSessionId()).setChatText(chatRequest),
				(AgentChatResponse response) -> {
					assertNotNull(response);
					assertEquals(session.getSessionId(), response.getSessionId());
					assertNotNull(response.getResponseText());
					assertTrue(response.getResponseText().contains(f1.getName()));
					assertTrue(response.getResponseText().contains(f2.getName()));
					assertTrue(response.getResponseText().contains(f1.getId()));
					assertTrue(response.getResponseText().contains(f2.getId()));
				}, MAX_WAIT_MS).getResponse();

	}

	@Test
	public void testGetFolderEntityChildren() throws AssertionError, AsynchJobFailedException, IOException {
		Project project = entityService.createEntity(admin.getId(), new Project().setName(UUID.randomUUID().toString()),
				null);
		entitiesToDelete.add(project.getId());

		Folder f1 = entityService.createEntity(admin.getId(), new Folder().setName("f1").setParentId(project.getId()),
				null);
		entitiesToDelete.add(f1.getId());

		Folder f2 = entityService.createEntity(admin.getId(), new Folder().setName("f2").setParentId(project.getId()),
				null);
		entitiesToDelete.add(f2.getId());

		S3FileHandle fileHandle = fileHandleManager.createFileFromByteArray(admin.getId().toString(), new Date(),
				"Test file content".getBytes(StandardCharsets.UTF_8), "TestFile.txt", ContentTypeUtil.TEXT_PLAIN_UTF8, null);
		fileHandlesToDelete.add(fileHandle);

		FileEntity fileOne = entityService.createEntity(admin.getId(),
				new FileEntity().setName("TestFile1").setParentId(project.getId()).setDataFileHandleId(fileHandle.getId()), null);
		entitiesToDelete.add(fileOne.getId());

		AgentSession session = agentService.createSession(admin.getId(),
				new CreateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.READ_YOUR_PRIVATE_DATA));

		assertNotNull(session);
		// an empty request will return an empty response.
		String chatRequest = "What are the names and synIDs of the folders in the project: " + project.getId();

		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new AgentChatRequest().setSessionId(session.getSessionId()).setChatText(chatRequest),
				(AgentChatResponse response) -> {
					assertNotNull(response);
					assertEquals(session.getSessionId(), response.getSessionId());
					assertNotNull(response.getResponseText());
					assertTrue(response.getResponseText().contains(f1.getName()));
					assertTrue(response.getResponseText().contains(f2.getName()));
					assertTrue(response.getResponseText().contains(f1.getId()));
					assertTrue(response.getResponseText().contains(f2.getId()));
					assertFalse(response.getResponseText().contains(fileOne.getName()));
				}, MAX_WAIT_MS);

	}

	@Test
	public void testGetFileEntityChildren() throws AssertionError, AsynchJobFailedException, IOException {
		Project project = entityService.createEntity(admin.getId(), new Project().setName(UUID.randomUUID().toString()),
				null);
		entitiesToDelete.add(project.getId());

		Folder f1 = entityService.createEntity(admin.getId(), new Folder().setName("f1").setParentId(project.getId()),
				null);
		entitiesToDelete.add(f1.getId());

		Folder f2 = entityService.createEntity(admin.getId(), new Folder().setName("f2").setParentId(project.getId()),
				null);
		entitiesToDelete.add(f2.getId());

		S3FileHandle fileHandle = fileHandleManager.createFileFromByteArray(admin.getId().toString(), new Date(),
				"Test file content".getBytes(StandardCharsets.UTF_8), "TestFile.txt", ContentTypeUtil.TEXT_PLAIN_UTF8, null);
		fileHandlesToDelete.add(fileHandle);

		FileEntity fileOne = entityService.createEntity(admin.getId(),
				new FileEntity().setName("TestFile1").setParentId(project.getId()).setDataFileHandleId(fileHandle.getId()), null);
		entitiesToDelete.add(fileOne.getId());

		AgentSession session = agentService.createSession(admin.getId(),
				new CreateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.READ_YOUR_PRIVATE_DATA));

		assertNotNull(session);
		// an empty request will return an empty response.
		String chatRequest = "What are the names and synIDs of the files in the project: " + project.getId();

		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new AgentChatRequest().setSessionId(session.getSessionId()).setChatText(chatRequest),
				(AgentChatResponse response) -> {
					assertNotNull(response);
					assertEquals(session.getSessionId(), response.getSessionId());
					assertNotNull(response.getResponseText());
					assertFalse(response.getResponseText().contains(f1.getName()));
					assertFalse(response.getResponseText().contains(f2.getName()));
					assertTrue(response.getResponseText().contains(fileOne.getName()));
				}, MAX_WAIT_MS);

	}

	@Test
	public void testGetEntityChildrenHandlerWithPagination() throws AssertionError, AsynchJobFailedException {
		Project project = entityService.createEntity(admin.getId(), new Project().setName(UUID.randomUUID().toString()),
				null);
		entitiesToDelete.add(project.getId());

		// create more than 50 children as one page can show up to 50 entities.
		for(int i=1; i<100;i++){
			 entityService.createEntity(admin.getId(), new Folder().setName("f"+i).setParentId(project.getId()),
					null);
		}

		AgentSession session = agentService.createSession(admin.getId(),
				new CreateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.READ_YOUR_PRIVATE_DATA));

		assertNotNull(session);
		// an empty request will return an empty response.
		String chatRequest = "What are the names and synIDs of the children of project: " + project.getId();

		//call under test
		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new AgentChatRequest().setSessionId(session.getSessionId()).setChatText(chatRequest),
				(AgentChatResponse response) -> {
					assertNotNull(response);
					assertEquals(session.getSessionId(), response.getSessionId());
					assertNotNull(response.getResponseText());
					assertTrue(response.getResponseText().contains("f1"));
					assertTrue(response.getResponseText().contains("f99"));
				}, MAX_WAIT_MS);

	}

	@Test
	public void testGetEntityDescriptionHandler() throws DatastoreException, NotFoundException, IOException, AssertionError, AsynchJobFailedException {
		Project project = entityService.createEntity(admin.getId(), new Project().setName(UUID.randomUUID().toString()),
				null);
		entitiesToDelete.add(project.getId());
		WikiPage wp = wikiService.createWikiPage(admin.getId(), project.getId(), ObjectType.ENTITY,
				new WikiPage().setTitle("The meaning of life")
						.setMarkdown("This is the root wiki of this project and it contains the first uuid: "
								+ UUID.randomUUID().toString()));
		WikiPage sub = wikiService.createWikiPage(admin.getId(), project.getId(), ObjectType.ENTITY,
				new WikiPage().setParentWikiId(wp.getId()).setTitle("Sub-page working title")
						.setMarkdown("This sub-page also contains another uuid:" + UUID.randomUUID().toString()));
		
		AgentSession session = agentService.createSession(admin.getId(),
				new CreateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.READ_YOUR_PRIVATE_DATA));

		assertNotNull(session);
		// an empty request will return an empty response.
		String chatRequest = "What is the full description of project: " + project.getId();

		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new AgentChatRequest().setSessionId(session.getSessionId()).setChatText(chatRequest),
				(AgentChatResponse response) -> {
					assertNotNull(response);
					assertEquals(session.getSessionId(), response.getSessionId());
					assertNotNull(response.getResponseText());
					System.out.println(response.getResponseText());
					assertTrue(response.getResponseText().contains(wp.getTitle()));
					assertTrue(response.getResponseText().contains(wp.getMarkdown()));
					assertTrue(response.getResponseText().contains(sub.getTitle()));
					assertTrue(response.getResponseText().contains(sub.getMarkdown()));
				}, MAX_WAIT_MS).getResponse();
	}
	
	@Test
	public void testGetAccessLevel() throws AssertionError, AsynchJobFailedException {
		AgentSession session = agentService.createSession(admin.getId(),
				new CreateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.PUBLICLY_ACCESSIBLE));
		
		String sessionId = session.getSessionId();
		
		String chatRequest = "What is the current access level?";
		
		asynchronousJobWorkerHelper.assertJobResponse(admin, new AgentChatRequest().setSessionId(sessionId).setChatText(chatRequest),
			(AgentChatResponse response) -> {
				assertNotNull(response);
				assertEquals(sessionId, response.getSessionId());
				assertNotNull(response.getResponseText());
				System.out.println(response.getResponseText());
				assertTrue(response.getResponseText().contains("PUBLICLY_ACCESSIBLE"));
			}, MAX_WAIT_MS).getResponse();
		
		Project project = entityService.createEntity(admin.getId(), new Project().setName(UUID.randomUUID().toString()), null);
		entitiesToDelete.add(project.getId());

		chatRequest = "What is the name of entity: " + project.getId();

		asynchronousJobWorkerHelper.assertJobResponse(admin, new AgentChatRequest().setSessionId(sessionId).setChatText(chatRequest),
			(AgentChatResponse response) -> {
				assertNotNull(response);
				assertEquals(sessionId, response.getSessionId());
				assertNotNull(response.getResponseText());
				System.out.println(response.getResponseText());
				assertTrue(response.getResponseText().contains(project.getId()));
				assertFalse(response.getResponseText().contains(project.getName()));
				
			}, MAX_WAIT_MS).getResponse();
		
		session = agentService.updateSession(admin.getId(), new UpdateAgentSessionRequest().setSessionId(sessionId).setAgentAccessLevel(AgentAccessLevel.READ_YOUR_PRIVATE_DATA));
		
		chatRequest = "Ok, I updated the access level. What is the name of entity: " + project.getId();

		asynchronousJobWorkerHelper.assertJobResponse(admin, new AgentChatRequest().setSessionId(sessionId).setChatText(chatRequest),
			(AgentChatResponse response) -> {
				assertNotNull(response);
				assertEquals(sessionId, response.getSessionId());
				assertNotNull(response.getResponseText());
				System.out.println(response.getResponseText());
				assertTrue(response.getResponseText().contains(project.getId()));
				assertTrue(response.getResponseText().contains(project.getName()));
				
			}, MAX_WAIT_MS).getResponse();
	}
}
