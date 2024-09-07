package org.sagebionetworks.repo.manager.agent;

import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.dbo.agent.AgentDao;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AgentManagerImpl implements AgentManager {
	
	private final AgentDao agentDao;
	
	@Autowired
	public AgentManagerImpl(AgentDao agentDao) {
		super();
		this.agentDao = agentDao;
	}

	@Override
	public AgentSession createSession(UserInfo userInfo, CreateAgentSessionRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getAgentAccessLevel(), "request.agentAccessLevel");
		// only authenticated users can start a chat session.
		AuthorizationUtils.disallowAnonymous(userInfo);
		
		if(request.getAgentId() != null) {
			if(!AuthorizationUtils.isSageEmployeeOrAdmin(userInfo)){
				throw new UnauthorizedException("Currently, only internal users can set agentId.");
			}
		}

		return agentDao.createSession(userInfo, request);
	}

	@Override
	public AgentChatResponse invokeAgent(UserInfo user, AgentChatRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

}
