package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.agent.AgentManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.UpdateAgentSessionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mchange.v1.cachedstore.CachedStore.Manager;

@Service
public class AgentServiceImpl implements AgentService {

	private final UserManager userManager;
	private final AgentManager agentManager;

	@Autowired
	public AgentServiceImpl(UserManager userManager, AgentManager agentManager) {
		this.userManager = userManager;
		this.agentManager = agentManager;
	}

	@Override
	public AgentSession createSession(Long userId, CreateAgentSessionRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return agentManager.createSession(userInfo, request);
	}

	@Override
	public AgentSession updateSession(Long userId, UpdateAgentSessionRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return agentManager.updateSession(userInfo, request);
	}

	@Override
	public AgentSession getSession(Long userId, String sessionId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return agentManager.getSesion(userInfo, sessionId);
	}

}
