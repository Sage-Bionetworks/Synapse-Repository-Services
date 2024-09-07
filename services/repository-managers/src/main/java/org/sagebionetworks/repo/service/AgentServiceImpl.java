package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.agent.AgentManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AgentServiceImpl implements AgentService {

	private final UserManager userManager;
	private final AgentManager agentManger;

	@Autowired
	public AgentServiceImpl(UserManager userManager, AgentManager agentManger) {
		this.userManager = userManager;
		this.agentManger = agentManger;
	}

	@Override
	public AgentSession createSession(Long userId, CreateAgentSessionRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return agentManger.createSession(userInfo, request);
	}

}
