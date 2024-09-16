package org.sagebionetworks.repo.model.dbo.agent;

import java.util.Optional;

import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentSession;

public interface AgentDao {

	AgentSession createSession(Long userId, AgentAccessLevel accessLevel, String agentId);

	Optional<AgentSession> getAgentSession(String sessionId);

	AgentSession updateSession(String sessionId, AgentAccessLevel accessLevel);

	void truncateAll();

}
