package org.sagebionetworks.repo.manager.agent;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.UpdateAgentSessionRequest;

public interface AgentManager {

	AgentSession createSession(UserInfo userInfo, CreateAgentSessionRequest request);

	AgentChatResponse invokeAgent(UserInfo user, AgentChatRequest request);

	AgentSession updateSession(UserInfo userInfo, UpdateAgentSessionRequest request);

	AgentSession getSesion(UserInfo userInfo, String sessionId);

}
