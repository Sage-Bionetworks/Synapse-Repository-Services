package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.service.AgentService;
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
}
