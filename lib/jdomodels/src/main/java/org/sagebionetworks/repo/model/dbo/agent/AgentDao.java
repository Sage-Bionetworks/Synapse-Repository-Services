package org.sagebionetworks.repo.model.dbo.agent;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;

public interface AgentDao {

	AgentSession createSession(UserInfo userInfo, CreateAgentSessionRequest request);

}
