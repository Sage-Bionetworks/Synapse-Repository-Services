package org.sagebionetworks.repo.model.dbo.agent;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentRegistration;
import org.sagebionetworks.repo.model.agent.AgentRegistrationActSettings;
import org.sagebionetworks.repo.model.agent.AgentRegistrationActSettingsBundle;
import org.sagebionetworks.repo.model.agent.AgentRegistrationRequest;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.AgentType;
import org.sagebionetworks.repo.model.agent.SessionContext;
import org.sagebionetworks.repo.model.agent.TraceEvent;

public interface AgentDao {

	AgentSession createSession(Long userId, AgentAccessLevel accessLevel, String registrationId, SessionContext context);

	Optional<AgentSession> getAgentSession(String sessionId);

	AgentSession updateSession(String sessionId, AgentAccessLevel accessLevel);

	void addTraceToJob(String jobId, long timestamp, String message);

	List<TraceEvent> listTraceEvents(String jobId, Long timestamp);

	void truncateAll();

	Optional<AgentRegistration> getRegeistration(String registrationId);

	AgentRegistration createOrGetRegistration(AgentType type, AgentRegistrationRequest request);

	/**
	 * Create or update the ACT-managed settings for an agent registration.
	 * <p>
	 * When settings already exist for the registration, the provided etag must match the current etag or a
	 * {@link org.sagebionetworks.repo.model.ConflictingUpdateException} is thrown (optimistic concurrency control).
	 * When no settings exist yet, the etag is ignored and a new row is created.
	 *
	 * @param registrationId The ID of the agent registration.
	 * @param modifiedBy     The principal ID of the ACT member making the change.
	 * @param etag           The etag the caller last saw, or null when creating settings for the first time.
	 * @param settings       The settings to store.
	 * @return The stored settings along with their metadata.
	 */
	AgentRegistrationActSettingsBundle setAgentRegistrationActSettings(String registrationId, Long modifiedBy,
			String etag, AgentRegistrationActSettings settings);

	/**
	 * Get the ACT-managed settings for an agent registration.
	 *
	 * @param registrationId The ID of the agent registration.
	 * @return The stored settings with their metadata, or {@link Optional#empty()} when no settings have been set.
	 */
	Optional<AgentRegistrationActSettingsBundle> getAgentRegistrationActSettings(String registrationId);

}
