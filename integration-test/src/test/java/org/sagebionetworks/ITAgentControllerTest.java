package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.AsynchJobType;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentRegistration;
import org.sagebionetworks.repo.model.agent.AgentRegistrationRequest;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.TraceEventsRequest;
import org.sagebionetworks.repo.model.agent.TraceEventsResponse;
import org.sagebionetworks.repo.model.agent.UpdateAgentSessionRequest;

@ExtendWith(ITTestExtension.class)
public class ITAgentControllerTest {

	private static final long MAX_WAIT_MS = 30_000;
	private static final int MAX_RETIES = 3;

	private final SynapseAdminClient adminSynapse;
	private final SynapseClient synapse;
	private final StackConfiguration config;

	public ITAgentControllerTest(SynapseAdminClient adminSynapse, SynapseClient synapse, StackConfiguration config) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
		this.config = config;
	}

	@Test
	public void testChat() throws SynapseException {

		AgentSession session = synapse.createAgentSession(
				new CreateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.PUBLICLY_ACCESSIBLE));
		assertNotNull(session);
		assertEquals(AgentAccessLevel.PUBLICLY_ACCESSIBLE, session.getAgentAccessLevel());

		AgentSession fromGet = synapse.getAgentSession(session.getSessionId());
		assertEquals(session, fromGet);

		AgentSession updated = synapse.updateAgentSession(new UpdateAgentSessionRequest()
				.setSessionId(session.getSessionId()).setAgentAccessLevel(AgentAccessLevel.READ_YOUR_PRIVATE_DATA));
		assertNotNull(updated);
		assertEquals(session.getSessionId(), updated.getSessionId());
		assertEquals(AgentAccessLevel.READ_YOUR_PRIVATE_DATA, updated.getAgentAccessLevel());

		// call under test, empty input should result in empty response.
		var jobResult = AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.AgentChat,
				new AgentChatRequest().setEnableTrace(true).setChatText("hello").setSessionId(session.getSessionId()),
				body -> {
					assertTrue(body instanceof AgentChatResponse);
					AgentChatResponse r = (AgentChatResponse) body;
					assertEquals(session.getSessionId(), r.getSessionId());
					assertNotNull(r.getResponseText());
				}, MAX_WAIT_MS, MAX_RETIES);

		// call under test
		TraceEventsResponse trace = synapse.getAgentTrace(new TraceEventsRequest().setJobId(jobResult.getJobToken()));
		assertNotNull(trace);
		assertEquals(jobResult.getJobToken(), trace.getJobId());
	}

	@Test
	public void testChatCustomAgent() throws SynapseException {

		// call under test
		AgentRegistration reg = adminSynapse.createOrGetAgentRegistration(
				new AgentRegistrationRequest().setAwsAgentId(config.getCustomHelloWorldBedrockAgentId()));
		assertNotNull(reg);
		assertNotNull(reg.getAgentRegistrationId());
		assertNotNull(reg.getAwsAliasId());
		assertEquals(config.getCustomHelloWorldBedrockAgentId(), reg.getAwsAgentId());
		// call under test
		AgentRegistration reg2 = synapse.getAgentRegistration(reg.getAgentRegistrationId());
		assertNotNull(reg2);
		assertEquals(reg, reg2);

		AgentSession session = synapse.createAgentSession(
				new CreateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.PUBLICLY_ACCESSIBLE)
						.setAgentRegistrationId(reg.getAgentRegistrationId()));
		assertNotNull(session);

		// call under test, empty input should result in empty response.
		var jobResult = AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.AgentChat,
				new AgentChatRequest().setEnableTrace(true).setChatText("hello").setSessionId(session.getSessionId()),
				body -> {
					assertTrue(body instanceof AgentChatResponse);
					AgentChatResponse r = (AgentChatResponse) body;
					assertEquals(session.getSessionId(), r.getSessionId());
					assertTrue(r.getResponseText().toLowerCase().contains("world"));
					assertNotNull(r.getResponseText());
				}, MAX_WAIT_MS, MAX_RETIES);

	}

}
