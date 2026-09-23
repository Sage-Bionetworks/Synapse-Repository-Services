package org.sagebionetworks.repo.manager.agent;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentRegistration;
import org.sagebionetworks.repo.model.agent.AgentRegistrationActSettingsBundle;
import org.sagebionetworks.repo.model.agent.AgentRegistrationActSettingsRequest;
import org.sagebionetworks.repo.model.agent.AgentRegistrationRequest;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.TraceEventsRequest;
import org.sagebionetworks.repo.model.agent.TraceEventsResponse;
import org.sagebionetworks.repo.model.agent.UpdateAgentSessionRequest;

public interface AgentManager {

	AgentSession createSession(UserInfo userInfo, CreateAgentSessionRequest request);

	AgentChatResponse invokeAgent(UserInfo user, String jobId, AgentChatRequest request);

	AgentSession updateSession(UserInfo userInfo, UpdateAgentSessionRequest request);

	AgentSession getSession(UserInfo userInfo, String sessionId);

	TraceEventsResponse getChatTrace(UserInfo userInfo, TraceEventsRequest request);

	AgentRegistration createOrGetAgentRegistration(UserInfo userInfo, AgentRegistrationRequest request);

	AgentRegistration getAgentRegistration(UserInfo userInfo, String agentRegistrationId);

	/**
	 * Create or update the ACT-managed settings for an agent registration. Only members of the ACT (or an
	 * administrator) may perform this operation.
	 *
	 * @param userInfo The user making the change.
	 * @param request  The settings to store, including the current etag for optimistic concurrency control.
	 * @return The stored settings along with their metadata.
	 */
	AgentRegistrationActSettingsBundle updateAgentRegistrationActSettings(UserInfo userInfo,
			AgentRegistrationActSettingsRequest request);

	/**
	 * Get the ACT-managed settings for an agent registration. Only members of the ACT (or an administrator) may
	 * perform this operation.
	 *
	 * @param userInfo            The user making the request.
	 * @param agentRegistrationId The ID of the agent registration.
	 * @return The stored settings along with their metadata. When no settings have been set, a bundle with a null
	 *         etag and empty settings is returned.
	 */
	AgentRegistrationActSettingsBundle getAgentRegistrationActSettings(UserInfo userInfo, String agentRegistrationId);

}
