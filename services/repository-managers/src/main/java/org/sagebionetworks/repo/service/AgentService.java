package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.UpdateAgentSessionRequest;

public interface AgentService {

	AgentSession createSession(Long userId, CreateAgentSessionRequest request);

	AgentSession updateSession(Long userId, UpdateAgentSessionRequest request);

	AgentSession getSession(Long userId, String sessionId);

}
