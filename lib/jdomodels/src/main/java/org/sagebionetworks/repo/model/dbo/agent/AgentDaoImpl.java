package org.sagebionetworks.repo.model.dbo.agent;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.springframework.stereotype.Repository;

@Repository
public class AgentDaoImpl implements AgentDao {

	@Override
	public AgentSession createSession(UserInfo userInfo, CreateAgentSessionRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

}
