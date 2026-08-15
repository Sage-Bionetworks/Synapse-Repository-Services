package org.sagebionetworks.repo.manager.agent.specialist.filesummary;

import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.Agent;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;

/**
 * A conversational file summary specialist agent. It reads files that already exist on the shared
 * code interpreter session and produces compact summaries for a supervisor, protecting the
 * supervisor's context window. Each instance maintains its own chat memory and is intended for a
 * single task delegation (multi-turn within that task, but discarded after).
 */
public class FileSummarySpecialist implements Agent {

	private final ChatClient chatClient;
	private final String conversationId;

	FileSummarySpecialist(ChatModel chatModel, StackConfiguration stackConfig,
			FileSummaryTools fileSummaryTools, CodeInterpreterTools codeInterpreterTools, String systemPrompt) {
		this.conversationId = UUID.randomUUID().toString();
		ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(20).build();
		this.chatClient = ChatClient.builder(chatModel)
				.defaultSystem(systemPrompt)
				.defaultToolCallbacks(fileSummaryTools.getToolCallbacks())
				.defaultToolCallbacks(codeInterpreterTools.getToolCallbacks())
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
				.defaultOptions(BedrockChatOptions.builder()
						.model(stackConfig.getModelIdClaudeHaiku())
						.maxTokens(Agent.MODELS_MAX_TOKENS)
						.build())
				.build();
	}

	@Override
	public AgentRole getAgentRole() {
		return AgentRole.SPECIALIST;
	}

	@Override
	public ChatClientRequestSpec prepareChatClientRequestSpec(String message, ToolContext context) {
		return chatClient.prompt()
				.user(message)
				.toolContext(context.getContext())
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));
	}
}
