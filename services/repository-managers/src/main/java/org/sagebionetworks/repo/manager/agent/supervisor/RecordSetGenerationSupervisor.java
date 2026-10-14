package org.sagebionetworks.repo.manager.agent.supervisor;

import java.util.List;
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
import org.springframework.ai.tool.ToolCallback;

/**
 * A conversational supervisor agent that orchestrates the specialist agents to generate a RecordSet
 * CSV from a folder of source FileEntities, following the data manager's transformation instructions
 * and conforming to a target JSON Schema. It runs on a stronger model than the Haiku specialists
 * because it must plan a multi-step workflow and delegate focused sub-tasks. It is given only the
 * focused subset of specialist delegation tools it needs (selected by its factory) and can also run
 * Python directly on the shared code interpreter session via {@link CodeInterpreterTools}. Each
 * instance maintains its own chat memory and is intended for a single task delegation (multi-turn
 * within that task, but discarded after).
 */
public class RecordSetGenerationSupervisor implements Agent {

	private final ChatClient chatClient;
	private final String conversationId;

	RecordSetGenerationSupervisor(ChatModel chatModel, StackConfiguration stackConfig,
			List<ToolCallback> specialistTools, CodeInterpreterTools codeInterpreterTools, String systemPrompt) {
		this.conversationId = UUID.randomUUID().toString();
		ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(40).build();
		this.chatClient = ChatClient.builder(chatModel)
				.defaultSystem(systemPrompt)
				.defaultToolCallbacks(specialistTools)
				.defaultToolCallbacks(codeInterpreterTools.getToolCallbacks())
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
				.defaultOptions(BedrockChatOptions.builder()
						.model(stackConfig.getModelIdClaudeSonnet())
						.maxTokens(Agent.MODELS_MAX_TOKENS)
						.build())
				.build();
	}

	@Override
	public AgentRole getAgentRole() {
		return AgentRole.SUPERVISOR;
	}

	@Override
	public ChatClientRequestSpec prepareChatClientRequestSpec(String message, ToolContext context) {
		return chatClient.prompt()
				.user(message)
				.toolContext(context.getContext())
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));
	}
}
