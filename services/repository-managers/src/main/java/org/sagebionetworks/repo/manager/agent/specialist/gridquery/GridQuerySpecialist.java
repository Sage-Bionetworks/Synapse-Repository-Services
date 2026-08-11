package org.sagebionetworks.repo.manager.agent.specialist.gridquery;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;

/**
 * A conversational grid query specialist agent. Each instance maintains its own chat memory
 * and is intended for a single task delegation (multi-turn within that task, but discarded
 * after).
 */
public class GridQuerySpecialist {

	private final ChatClient chatClient;
	private final String conversationId;

	GridQuerySpecialist(ChatModel chatModel, StackConfiguration stackConfig, GridQueryTools gridQueryTools,
			String systemPrompt) {
		this.conversationId = UUID.randomUUID().toString();
		ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(20).build();
		this.chatClient = ChatClient.builder(chatModel)
				.defaultSystem(systemPrompt)
				.defaultToolCallbacks(gridQueryTools.getToolCallbacks())
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
				.defaultOptions(BedrockChatOptions.builder()
						.model(stackConfig.getModelIdClaudeSonnet())
						.maxTokens(4096)
						.build())
				.build();
	}

	/**
	 * Send a message to this specialist and get a response. Maintains conversation context
	 * across multiple calls within the same specialist instance. The trusted
	 * {@link GridAgentSessionContext} is forwarded to the query tool via the agent-immutable
	 * tool context.
	 */
	public String chat(String message, UserInfo user, String sessionId, GridAgentSessionContext gridContext) {
		Map<String, Object> context = new HashMap<>();
		AgentToolContextKey.USER_INFO.put(context, user);
		if (sessionId != null) {
			AgentToolContextKey.CODE_SESSION_ID.put(context, sessionId);
		}
		AgentToolContextKey.GRID_SESSION_CONTEXT.put(context, gridContext);
		return chatClient.prompt()
				.user(message)
				.toolContext(context)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
				.call()
				.content();
	}
}
