package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
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
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.service.AgentService;
import org.sagebionetworks.repo.service.EntityService;
import org.sagebionetworks.repo.service.WikiService;
import org.sagebionetworks.repo.web.NotFoundException;
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

	private UserInfo admin;

	@BeforeEach
	public void before() {

		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
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
	public void testGetEntityChildernHandler() throws AssertionError, AsynchJobFailedException {
		Project project = entityService.createEntity(admin.getId(), new Project().setName(UUID.randomUUID().toString()),
				null);

		Folder f1 = entityService.createEntity(admin.getId(), new Folder().setName("f1").setParentId(project.getId()),
				null);
		Folder f2 = entityService.createEntity(admin.getId(), new Folder().setName("f2").setParentId(project.getId()),
				null);

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
					System.out.println(response.getResponseText());
					assertTrue(response.getResponseText().contains(f1.getName()));
					assertTrue(response.getResponseText().contains(f2.getName()));
					assertTrue(response.getResponseText().contains(f1.getId()));
					assertTrue(response.getResponseText().contains(f2.getId()));
				}, MAX_WAIT_MS).getResponse();

	}

	@Test
	public void testGetEntityDescriptionHandler() throws DatastoreException, NotFoundException, IOException, AssertionError, AsynchJobFailedException {
		Project project = entityService.createEntity(admin.getId(), new Project().setName(UUID.randomUUID().toString()),
				null);
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
		
		chatRequest = "What is the name of entity: " + project.getId();

		asynchronousJobWorkerHelper.assertJobResponse(admin, new AgentChatRequest().setSessionId(sessionId).setChatText(chatRequest),
			(AgentChatResponse response) -> {
				assertNotNull(response);
				assertEquals(sessionId, response.getSessionId());
				assertNotNull(response.getResponseText());
				System.out.println(response.getResponseText());
				assertTrue(response.getResponseText().contains(project.getId()));
				assertTrue(response.getResponseText().contains("Public Data Only"));
				assertTrue(response.getResponseText().contains("increase"));
				assertTrue(response.getResponseText().contains("Read your Private Data"));
				
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
