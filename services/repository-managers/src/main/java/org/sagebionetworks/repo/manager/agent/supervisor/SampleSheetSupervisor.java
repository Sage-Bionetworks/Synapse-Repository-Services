package org.sagebionetworks.repo.manager.agent.supervisor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;

/**
 * A conversational supervisor agent that orchestrates the specialist agents to produce a sample
 * sheet. It runs on a stronger model than the Haiku specialists because it must plan a multi-step
 * workflow and delegate focused sub-tasks. It delegates through {@link SupervisorTools} (one tool
 * per specialist) and can also run Python directly on the shared code interpreter session via
 * {@link CodeInterpreterTools}. Each instance maintains its own chat memory and is intended for a
 * single task delegation (multi-turn within that task, but discarded after).
 */
public class SampleSheetSupervisor {

	private final ChatClient chatClient;
	private final String conversationId;

	SampleSheetSupervisor(ChatModel chatModel, StackConfiguration stackConfig,
			SupervisorTools supervisorTools, CodeInterpreterTools codeInterpreterTools, String systemPrompt) {
		this.conversationId = UUID.randomUUID().toString();
		ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(40).build();
		this.chatClient = ChatClient.builder(chatModel)
				.defaultSystem(systemPrompt)
				.defaultTools(supervisorTools, codeInterpreterTools)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
				.defaultOptions(BedrockChatOptions.builder()
						.model(stackConfig.getModelIdClaudeSonnet())
						.maxTokens(8192)
						.build())
				.build();
	}

	/**
	 * Send a message to this supervisor and get a response. Maintains conversation context across
	 * multiple calls within the same supervisor instance.
	 */
	public String chat(String message, UserInfo user, String sessionId) {
		Map<String, Object> context = new HashMap<>();
		context.put("userInfo", user);
		if (sessionId != null) {
			context.put("sessionId", sessionId);
		}
		return chatClient.prompt()
				.user(message)
				.toolContext(context)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
				.call()
				.content();
	}
}
