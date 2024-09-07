package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;

public interface AgentService {

	AgentSession createSession(Long userId, CreateAgentSessionRequest request);

}
