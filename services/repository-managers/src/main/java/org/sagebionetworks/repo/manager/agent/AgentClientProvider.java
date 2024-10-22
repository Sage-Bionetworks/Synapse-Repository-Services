package org.sagebionetworks.repo.manager.agent;

import java.util.Map;

import org.sagebionetworks.repo.model.agent.AgentType;

import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;

public class AgentClientProvider {

	private final Map<AgentType, BedrockAgentRuntimeAsyncClient> clientMap;

	public AgentClientProvider(Map<AgentType, BedrockAgentRuntimeAsyncClient> clientMap) {
		super();
		this.clientMap = clientMap;
	}

	/**
	 * Get the BedrockAgentRuntimeAsyncClient to be used for the provide Agent type.
	 * 
	 * @param type
	 * @return
	 */
	public BedrockAgentRuntimeAsyncClient getBedrockAgentRuntimeAsyncClient(AgentType type) {
		return clientMap.get(type);
	}
}
