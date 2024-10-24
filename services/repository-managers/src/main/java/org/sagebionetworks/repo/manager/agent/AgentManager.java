package org.sagebionetworks.repo.manager.agent;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentRegistration;
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

}
