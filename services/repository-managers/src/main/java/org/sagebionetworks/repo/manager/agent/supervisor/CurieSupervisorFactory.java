package org.sagebionetworks.repo.manager.agent.supervisor;

import java.io.StringWriter;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.sagebionetworks.StackConfiguration;
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
	private final String renderedSystemPrompt;

	public CurieSupervisorFactory(ChatModel chatModel, StackConfiguration stackConfig,
			SpecialistToolProvider specialistToolProvider, CodeInterpreterTools codeInterpreterTools,
			CodeInterpreterSessionProvider sessionProvider, ChatMemoryRepository curieChatMemoryRepository) {
		this.chatModel = chatModel;
		this.stackConfig = stackConfig;
		this.codeInterpreterTools = codeInterpreterTools;
		this.sessionProvider = sessionProvider;
		this.memoryRepository = curieChatMemoryRepository;
		this.specialistTools = specialistToolProvider.getTools(SupervisorTools.TOOL_JSON_SCHEMA,
				SupervisorTools.TOOL_GRID_QUERY, SupervisorTools.TOOL_GRID_UPDATE,
				SupervisorTools.TOOL_GRID_METADATA, SupervisorTools.TOOL_FILE_SUMMARY);
		this.renderedSystemPrompt = renderSystemPrompt();
	}

	public CurieSupervisor create() {
		return new CurieSupervisor(chatModel, stackConfig, specialistTools, codeInterpreterTools, sessionProvider,
				memoryRepository, renderedSystemPrompt);
	}

	String renderSystemPrompt() {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		engine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());

		VelocityContext context = new VelocityContext();

		Template template = engine.getTemplate(PROMPT_TEMPLATE);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		return writer.toString();
	}
}
