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
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.UpdateAgentSessionRequest;

@ExtendWith(ITTestExtension.class)
public class ITAgentControllerTest {

	private static final long MAX_WAIT_MS = 30_000;
	private static final int MAX_RETIES = 3;

	private SynapseAdminClient adminSynapse;
	private SynapseClient synapse;

	public ITAgentControllerTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
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
		AgentChatResponse response = (AgentChatResponse) AsyncJobHelper
				.assertAysncJobResult(synapse, AsynchJobType.AgentChat,
						new AgentChatRequest().setChatText("").setSessionId(session.getSessionId()), body -> {
							assertTrue(body instanceof AgentChatResponse);
							AgentChatResponse r = (AgentChatResponse) body;
							assertEquals(session.getSessionId(), r.getSessionId());
							assertEquals("", r.getResponseText());
						}, MAX_WAIT_MS, MAX_RETIES)
				.getResponse();
	}

}
