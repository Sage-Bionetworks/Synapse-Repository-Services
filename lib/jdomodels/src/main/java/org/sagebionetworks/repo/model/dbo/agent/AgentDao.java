package org.sagebionetworks.repo.model.dbo.agent;

import java.util.Optional;

import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.UpdateAgentSessionRequest;

public interface AgentDao {

	AgentSession createSession(Long userId, CreateAgentSessionRequest request);

	Optional<AgentSession> getAgentSession(String sessionId);

	AgentSession updateSession(UpdateAgentSessionRequest request);

	void truncateAll();

}
