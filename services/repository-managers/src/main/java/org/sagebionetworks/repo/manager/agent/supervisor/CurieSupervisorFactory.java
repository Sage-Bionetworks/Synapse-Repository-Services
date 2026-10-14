package org.sagebionetworks.repo.manager.agent.supervisor;

import java.io.StringWriter;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.Agent;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterSessionProvider;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

/**
 * Factory for creating {@link CurieSupervisor} instances. Each instance shares the durable,
 * cross-machine {@link ChatMemoryRepository} (Bedrock AgentCore Memory) and a pre-rendered system
 * prompt; the conversation is keyed per-turn from the user and chat session id. Curie delegates to a
 * focused subset of specialists (JSON schema + grid query + grid update + grid metadata + file
 * summary), selected by name from {@link SpecialistToolProvider}.
 */
@Service
public class CurieSupervisorFactory {

	static final String PROMPT_TEMPLATE = "prompts/curie-supervisor.vtp";

	private final ChatModel chatModel;
	private final StackConfiguration stackConfig;
	private final CodeInterpreterTools codeInterpreterTools;
	private final CodeInterpreterSessionProvider sessionProvider;
	private final ChatMemoryRepository memoryRepository;
	private final List<ToolCallback> specialistTools;
	private final VelocityEngine velocityEngine;
	private final String renderedSystemPrompt;

	public CurieSupervisorFactory(ChatModel chatModel, StackConfiguration stackConfig,
			SpecialistToolProvider specialistToolProvider, CodeInterpreterTools codeInterpreterTools,
			CodeInterpreterSessionProvider sessionProvider, ChatMemoryRepository curieChatMemoryRepository,
			VelocityEngine velocityEngine) {
		this.chatModel = chatModel;
		this.stackConfig = stackConfig;
		this.codeInterpreterTools = codeInterpreterTools;
		this.sessionProvider = sessionProvider;
		this.memoryRepository = curieChatMemoryRepository;
		this.specialistTools = specialistToolProvider.getTools(SupervisorTools.TOOL_JSON_SCHEMA,
				SupervisorTools.TOOL_GRID_QUERY, SupervisorTools.TOOL_GRID_UPDATE,
				SupervisorTools.TOOL_GRID_METADATA, SupervisorTools.TOOL_FILE_SUMMARY);
		this.velocityEngine = velocityEngine;
		this.renderedSystemPrompt = renderSystemPrompt();
	}

	public Agent create() {
		return new CurieSupervisor(chatModel, stackConfig, specialistTools, codeInterpreterTools, sessionProvider,
				memoryRepository, renderedSystemPrompt);
	}

	String renderSystemPrompt() {
		VelocityContext context = new VelocityContext();
		// The shared engine runs in strict-reference mode, so bind $D to a literal '$' and use ${D} in
		// the template wherever a literal dollar sign is intended (e.g. JSON Schema keywords like $ref/$id).
		context.put("D", "$");

		Template template = velocityEngine.getTemplate(PROMPT_TEMPLATE);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		return writer.toString();
	}
}
