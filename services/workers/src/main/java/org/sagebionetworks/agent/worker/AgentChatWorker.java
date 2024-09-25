package org.sagebionetworks.agent.worker;

import org.sagebionetworks.repo.manager.agent.AgentManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AgentChatWorker implements AsyncJobRunner<AgentChatRequest, AgentChatResponse> {

	private final AgentManager agentManager;

	@Autowired
	public AgentChatWorker(AgentManager agentManager) {
		super();
		this.agentManager = agentManager;
	}

	@Override
	public Class<AgentChatRequest> getRequestType() {
		return AgentChatRequest.class;
	}

	@Override
	public Class<AgentChatResponse> getResponseType() {
		return AgentChatResponse.class;
	}

	@Override
	public AgentChatResponse run(String jobId, UserInfo user, AgentChatRequest request,
			AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		return agentManager.invokeAgent(user, jobProgressCallback.getJobId(), request);
	}

}
