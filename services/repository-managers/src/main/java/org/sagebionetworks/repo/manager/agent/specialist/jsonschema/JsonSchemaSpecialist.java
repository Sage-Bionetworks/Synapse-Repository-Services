package org.sagebionetworks.repo.manager.agent.specialist.jsonschema;

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
 * A conversational JSON Schema specialist agent. Each instance maintains its own
 * chat memory and is intended for a single task delegation (multi-turn within that task,
 * but discarded after).
 */
public class JsonSchemaSpecialist {

	private final ChatClient chatClient;
	private final String conversationId;

	JsonSchemaSpecialist(ChatModel chatModel, StackConfiguration stackConfig,
			JsonSchemaTools jsonSchemaTools, CodeInterpreterTools codeInterpreterTools, String systemPrompt) {
		this.conversationId = UUID.randomUUID().toString();
		ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(20).build();
		this.chatClient = ChatClient.builder(chatModel)
				.defaultSystem(systemPrompt)
				.defaultTools(jsonSchemaTools, codeInterpreterTools)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
				.defaultOptions(BedrockChatOptions.builder()
						.model(stackConfig.getModelIdClaudeHaiku())
						.maxTokens(4096)
						.build())
				.build();
	}

	/**
	 * Send a message to this specialist and get a response. Maintains conversation
	 * context across multiple calls within the same specialist instance.
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
