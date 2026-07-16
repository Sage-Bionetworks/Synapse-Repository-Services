package org.sagebionetworks.repo.manager.agent.specialist.filesummary;

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
 * A conversational file summary specialist agent. It reads files that already exist on the shared
 * code interpreter session and produces compact summaries for a supervisor, protecting the
 * supervisor's context window. Each instance maintains its own chat memory and is intended for a
 * single task delegation (multi-turn within that task, but discarded after).
 */
public class FileSummarySpecialist {

	private final ChatClient chatClient;
	private final String conversationId;

	FileSummarySpecialist(ChatModel chatModel, StackConfiguration stackConfig,
			FileSummaryTools fileSummaryTools, CodeInterpreterTools codeInterpreterTools, String systemPrompt) {
		this.conversationId = UUID.randomUUID().toString();
		ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(20).build();
		this.chatClient = ChatClient.builder(chatModel)
				.defaultSystem(systemPrompt)
				.defaultTools(fileSummaryTools, codeInterpreterTools)
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
