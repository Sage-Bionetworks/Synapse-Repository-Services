package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.AsynchJobType;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentRegistration;
import org.sagebionetworks.repo.model.agent.AgentRegistrationActSettings;
import org.sagebionetworks.repo.model.agent.AgentRegistrationActSettingsBundle;
import org.sagebionetworks.repo.model.agent.AgentRegistrationActSettingsRequest;
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
	public void testAgentRegistrationActSettingsAndAnonymousSession() throws SynapseException {
		// Register a custom agent (registration is just metadata; no model invocation needed here).
		AgentRegistration reg = adminSynapse.createOrGetAgentRegistration(
				new AgentRegistrationRequest().setAwsAgentId(config.getCustomHelloWorldBedrockAgentId()));
		assertNotNull(reg.getAgentRegistrationId());

		// An admin is a member of the ACT, so they may set the ACT-managed settings. First write has no etag.
		AgentRegistrationActSettingsBundle bundle = adminSynapse
				.updateAgentRegistrationActSettings(new AgentRegistrationActSettingsRequest()
						.setAgentRegistrationId(reg.getAgentRegistrationId())
						.setSettings(new AgentRegistrationActSettings().setAllowAnonymousChatSession(true)));
		assertEquals(reg.getAgentRegistrationId(), bundle.getAgentRegistrationId());
		assertNotNull(bundle.getEtag());
		assertNotNull(bundle.getModifiedOn());
		assertNotNull(bundle.getModifiedBy());
		assertEquals(Boolean.TRUE, bundle.getSettings().getAllowAnonymousChatSession());

		// The GET returns the stored settings.
		AgentRegistrationActSettingsBundle fromGet = adminSynapse
				.getAgentRegistrationActSettings(reg.getAgentRegistrationId());
		assertEquals(bundle, fromGet);

		// A non-ACT user may neither read nor write the settings.
		assertThrows(SynapseForbiddenException.class, () -> synapse
				.getAgentRegistrationActSettings(reg.getAgentRegistrationId()));
		assertThrows(SynapseForbiddenException.class,
				() -> synapse.updateAgentRegistrationActSettings(new AgentRegistrationActSettingsRequest()
						.setAgentRegistrationId(reg.getAgentRegistrationId())
						.setSettings(new AgentRegistrationActSettings().setAllowAnonymousChatSession(false))));

		// An anonymous client may now start a session against this registration; the session is forced to public.
		SynapseClient anonymousSynapse = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(anonymousSynapse);
		AgentSession anonymousSession = anonymousSynapse
				.createAgentSession(new CreateAgentSessionRequest().setAgentRegistrationId(reg.getAgentRegistrationId())
						.setAgentAccessLevel(AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA));
		assertNotNull(anonymousSession.getSessionId());
		assertEquals(AgentAccessLevel.PUBLICLY_ACCESSIBLE, anonymousSession.getAgentAccessLevel());
	}

	@Disabled // We disabled this test as the custom agent (id= 0O3IDUIR36 ) uses a model that has "reached the end of its life".
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
