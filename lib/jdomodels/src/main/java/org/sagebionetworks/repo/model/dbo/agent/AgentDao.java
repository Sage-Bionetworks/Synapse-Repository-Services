package org.sagebionetworks.repo.model.dbo.agent;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentRegistration;
import org.sagebionetworks.repo.model.agent.AgentRegistrationRequest;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.AgentType;
import org.sagebionetworks.repo.model.agent.TraceEvent;

public interface AgentDao {

	AgentSession createSession(Long userId, AgentAccessLevel accessLevel, String registrationId);

	Optional<AgentSession> getAgentSession(String sessionId);

	AgentSession updateSession(String sessionId, AgentAccessLevel accessLevel);

	void addTraceToJob(String jobId, long timestamp, String message);

	List<TraceEvent> listTraceEvents(String jobId, Long timestamp);

	void truncateAll();

	Optional<AgentRegistration> getRegeistration(String registrationId);

	AgentRegistration createOrGetRegistration(AgentType type, AgentRegistrationRequest request);

}
