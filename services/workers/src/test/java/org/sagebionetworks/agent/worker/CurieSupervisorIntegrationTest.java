package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Verifies that Curie's conversation memory is durable across supervisor instances. Curie is invoked
 * from asynchronous chat jobs, so consecutive turns of one conversation may run on different worker
 * machines — each chat job builds a fresh {@link org.sagebionetworks.repo.manager.agent.supervisor.CurieSupervisor}
 * (a new ChatClient and a new windowing memory) from the factory. Memory must therefore live in the
 * shared, durable {@code ChatMemoryRepository} (Bedrock AgentCore Memory), keyed by the user and the
 * durable chat {@code sessionId}, not in any per-instance store. These tests drive Curie the same way
 * production does: through the agent chat async job against an experimental grid agent session.
 * Requires live Bedrock + AgentCore Memory access.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class CurieSupervisorIntegrationTest {

	private static final long MAX_WAIT_MS = 1000L * 60 * 3;

	@Autowired
	private UserManager userManager;
	@Autowired
	private AgentService agentService;
	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	@Autowired
	private GridManager gridManager;

	private UserInfo adminUser;
	private GridSession gridSession;
	private Long usersReplicaId;

	@BeforeEach
	public void setup() throws Exception {
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		// A minimal empty grid is enough: these tests exercise Curie's durable conversation memory, not
		// grid data. The experimental grid context still needs a real session and user replica so the agent
		// session validates and routes through the Curie supervisor.
		gridSession = asynchronousJobWorkerHelper.assertJobResponse(adminUser, new CreateGridRequest(),
				(CreateGridResponse response) -> {
					assertNotNull(response);
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		usersReplicaId = gridManager
				.createReplica(adminUser, new CreateReplicaRequest().setGridSessionId(gridSession.getSessionId()))
				.getReplica().getReplicaId();
	}

	/**
	 * Create an experimental grid agent session so chat turns route through the Curie supervisor. Each
	 * session gets its own durable chat id, which is also the conversation-memory key.
	 */
	private AgentSession createCurieSession() {
		GridAgentSessionContext context = new GridAgentSessionContext().setGridSessionId(gridSession.getSessionId())
				.setUsersReplicaId(usersReplicaId).setExperimental(true);
		AgentSession agentSession = agentService.createSession(adminUser.getId(), new CreateAgentSessionRequest()
				.setSessionContext(context).setAgentAccessLevel(AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA));
		assertNotNull(agentSession);
		return agentSession;
	}

	/**
	 * Send one chat turn through the agent chat async job and return the supervisor's response text.
	 */
	private String chat(AgentSession agentSession, String chatText) throws Exception {
		return asynchronousJobWorkerHelper.assertJobResponse(adminUser,
				new AgentChatRequest().setSessionId(agentSession.getSessionId()).setChatText(chatText),
				(AgentChatResponse response) -> {
					assertNotNull(response);
					assertEquals(agentSession.getSessionId(), response.getSessionId());
					assertNotNull(response.getResponseText());
				}, MAX_WAIT_MS).getResponse().getResponseText();
	}

	@Test
	public void testMemoryPersistsAcrossSupervisorInstances() throws Exception {
		String referenceCode = "ZEBRA-4417";
		// A single durable agent session across both turns. Each turn runs as its own chat job that builds
		// a fresh supervisor — possibly on a different worker — so the second turn recalling the first turn's
		// fact is only possible if the memory was loaded from the shared durable store by the user:sessionId
		// conversation key.
		AgentSession agentSession = createCurieSession();

		String ack = chat(agentSession, "Please remember this reference code for our session: " + referenceCode
				+ ". Just confirm you have noted it.");
		assertNotNull(ack);

		// call under test
		String recall = chat(agentSession,
				"What reference code did I give you earlier in this session? Reply with only the code.");
		assertTrue(recall.toUpperCase().contains(referenceCode),
				"A later turn should recall the earlier turn's reference code. Got: " + recall);
	}

	@Test
	public void testMemoryIsolatedBySession() throws Exception {
		String referenceCode = "ZEBRA-4417";
		// Establish the fact under one agent session.
		AgentSession establishedSession = createCurieSession();
		chat(establishedSession, "Please remember this reference code for our session: " + referenceCode
				+ ". Just confirm you have noted it.");

		// A different agent session for the same user must not see it — proving memory is keyed by session,
		// not shared globally across the user's conversations.
		AgentSession otherSession = createCurieSession();

		// call under test
		String recall = chat(otherSession, "What reference code did I give you earlier in this session? "
				+ "If you have no reference code on record, reply exactly NONE.");
		assertFalse(recall.toUpperCase().contains(referenceCode),
				"A separate session must not recall another session's reference code. Got: " + recall);
	}
}
