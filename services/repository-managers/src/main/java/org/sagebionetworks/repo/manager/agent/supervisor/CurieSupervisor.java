package org.sagebionetworks.repo.manager.agent.supervisor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.Agent;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterSessionProvider;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.sagebionetworks.repo.manager.agent.CodeSessionSupplier;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentChatAttachmentStatus;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

/**
 * A conversational supervisor agent (Curie) that orchestrates the specialist agents to curate
 * grid data. It runs on a stronger model than the Haiku specialists because it must plan a
 * multi-step workflow and delegate focused sub-tasks. It delegates through a focused subset of
 * specialist tools (JSON schema + grid query + grid update) and can also run Python directly on
 * the shared code interpreter session via {@link CodeInterpreterTools}. Curie is invoked from
 * asynchronous chat jobs, so consecutive turns of one conversation may run on different worker
 * machines. Its chat memory is therefore backed by a durable {@link ChatMemoryRepository} (Bedrock
 * AgentCore Memory) rather than an in-JVM store, keyed by a conversation id derived from the user
 * and the durable chat session id so any worker resolves the same conversation.
 */
public class CurieSupervisor implements Agent {

	private final ChatClient chatClient;
	private final CodeInterpreterSessionProvider sessionProvider;

	CurieSupervisor(ChatModel chatModel, StackConfiguration stackConfig, List<ToolCallback> specialistTools,
			CodeInterpreterTools codeInterpreterTools, CodeInterpreterSessionProvider sessionProvider,
			ChatMemoryRepository memoryRepository, String systemPrompt) {
		this.sessionProvider = sessionProvider;
		ChatMemory memory = MessageWindowChatMemory.builder()
				.chatMemoryRepository(memoryRepository)
				.maxMessages(40)
				.build();
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

	/**
	 * Conversation context is durable and cross-machine: it is keyed by a conversation id derived from
	 * the {@link AgentToolContextKey#USER_INFO caller} and the durable
	 * {@link AgentToolContextKey#CHAT_SESSION_ID chat session id}, so a later turn on a different worker
	 * continues the same conversation. Both are required for this reason. The
	 * {@link AgentToolContextKey#GRID_SESSION_CONTEXT grid context} and the optional
	 * {@link AgentToolContextKey#TRACE_CALLBACK trace callback} flow through to the grid specialists
	 * unchanged, so they operate against the user's replica and every tool call in the turn — at this
	 * supervisor and in the specialists it delegates to — is recorded as job trace.
	 * <p>
	 * The costly code interpreter session is provisioned lazily: a {@link CodeSessionSupplier} keyed by
	 * the chat session id is added to the context here, and the session is created (or reused across
	 * turns and workers) only on the first {@code runPython} or specialist delegation of the turn —
	 * never on a purely conversational turn. Any files staged into that session for this turn are
	 * described to the model via a preamble prepended to the user message.
	 */
	@Override
	public ChatClientRequestSpec prepareChatClientRequestSpec(String message, ToolContext context) {
		UserInfo user = (UserInfo) AgentToolContextKey.USER_INFO.get(context);
		String chatSessionId = (String) AgentToolContextKey.CHAT_SESSION_ID.get(context);
		ValidateArgument.required(user, "user");
		ValidateArgument.required(user.getId(), "user.getId()");
		ValidateArgument.requiredNotBlank(chatSessionId, "chatSessionId");
		// AgentCore Memory parses this as actorId:sessionId — the user is the actor, the durable
		// Synapse chat session is the session.
		String conversationId = user.getId() + ":" + chatSessionId;

		// Add the lazy, memoizing code-session supplier so the shared session is created only on the
		// first code activity of the turn. The rest of the caller-supplied context flows through to the
		// specialists unchanged.
		Map<String, Object> toolContext = new HashMap<>(context.getContext());
		AgentToolContextKey.CODE_SESSION_SUPPLIER.put(toolContext, sessionProvider.lazySupplier(chatSessionId));

		@SuppressWarnings("unchecked")
		List<AgentChatAttachmentStatus> stagedAttachments = (List<AgentChatAttachmentStatus>) AgentToolContextKey.STAGED_ATTACHMENTS
				.get(context);
		return chatClient.prompt()
				.user(withAttachmentPreamble(message, stagedAttachments))
				.toolContext(toolContext)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));
	}

	/**
	 * Prepends a delimited description of the files staged into the code interpreter session for this turn
	 * to the user's message, so the supervisor knows the files exist and where to read them. When no files
	 * were staged the message is returned unchanged. The preamble is part of the user turn, so the durable
	 * chat memory keeps the files referenceable on later turns.
	 */
	static String withAttachmentPreamble(String message, List<AgentChatAttachmentStatus> stagedAttachments) {
		if (stagedAttachments == null || stagedAttachments.isEmpty()) {
			return message;
		}
		StringBuilder preamble = new StringBuilder();
		preamble.append("The user attached the following file(s) to this message. They have been loaded into your ")
				.append("code interpreter session at the paths below; read them from those paths.\n");
		for (AgentChatAttachmentStatus attachment : stagedAttachments) {
			preamble.append("- path: ").append(attachment.getSessionPath())
					.append(", name: ").append(attachment.getFileName())
					.append(", content type: ").append(attachment.getContentType())
					.append(", size (bytes): ").append(attachment.getContentSizeBytes())
					.append("\n");
		}
		preamble.append("\n");
		return preamble.append(message).toString();
	}
}
