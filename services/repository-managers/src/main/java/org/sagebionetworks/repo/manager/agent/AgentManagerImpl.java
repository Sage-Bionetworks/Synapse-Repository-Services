package org.sagebionetworks.repo.manager.agent;

import java.util.Optional;

import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.UpdateAgentSessionRequest;
import org.sagebionetworks.repo.model.dbo.agent.AgentDao;
import org.sagebionetworks.repo.web.NotFoundException;
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

		if (request.getAgentId() != null) {
			if (!AuthorizationUtils.isSageEmployeeOrAdmin(userInfo)) {
				throw new UnauthorizedException("Currently, only internal users can override the agentId.");
			}
		}

		return agentDao.createSession(userInfo.getId(), request);
	}

	@Override
	public AgentSession updateSession(UserInfo userInfo, UpdateAgentSessionRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSessionId(), "request.sessionId");
		ValidateArgument.required(request.getAgentAccessLevel(), "request.agentAccessLevel");
		AgentSession s = getAndValidateAgentSession(userInfo, request.getSessionId());
		if (request.getAgentAccessLevel().equals(s.getAgentAccessLevel())) {
			return s;
		}
		return agentDao.updateSession(request);
	}

	@Override
	public AgentChatResponse invokeAgent(UserInfo userInfo, AgentChatRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSessionId(), "request.sessionId");
		AgentSession session = getAndValidateAgentSession(userInfo, request.getSessionId());
		// do nothing with an empty of blank input.
		if(request.getChatText() == null || request.getChatText().isBlank()) {
			return new AgentChatResponse().setResponseText("").setSessionId(request.getSessionId());
		}
		// stub response
		return new AgentChatResponse().setResponseText("hello").setSessionId(request.getSessionId());
	}

	/**
	 * Helper to get and validate the session for the provided sessionId.
	 * 
	 * @param userInfo
	 * @param sessionId
	 * @return
	 */
	AgentSession getAndValidateAgentSession(UserInfo userInfo, String sessionId) {
		Optional<AgentSession> op = agentDao.getAgentSession(sessionId);
		if (op.isEmpty()) {
			throw new NotFoundException("Agent session does not exist");
		}
		AgentSession s = op.get();
		if (!userInfo.getId().equals(s.getStartedBy())) {
			throw new UnauthorizedException("Only the user that started a session may access it");
		}
		return s;
	}

	@Override
	public AgentSession getSesion(UserInfo userInfo, String sessionId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(sessionId, "sessionId");
		return getAndValidateAgentSession(userInfo, sessionId);
	}

}
