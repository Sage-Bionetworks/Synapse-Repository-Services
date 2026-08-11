package org.sagebionetworks.repo.manager.agent.supervisor;

import java.io.StringWriter;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

/**
 * Factory for creating {@link SampleSheetSupervisor} instances. Each instance gets a fresh
 * conversation memory and a pre-rendered system prompt. The supervisor delegates to a focused
 * subset of specialists (table query + JSON schema + file summary), selected by name from
 * {@link SpecialistToolProvider}.
 */
@Service
public class SampleSheetSupervisorFactory {

	static final String PROMPT_TEMPLATE = "prompts/sample-sheet-supervisor.vtp";

	private final ChatModel chatModel;
	private final StackConfiguration stackConfig;
	private final CodeInterpreterTools codeInterpreterTools;
	private final List<ToolCallback> specialistTools;
	private final String renderedSystemPrompt;

	public SampleSheetSupervisorFactory(ChatModel chatModel, StackConfiguration stackConfig,
			SpecialistToolProvider specialistToolProvider, CodeInterpreterTools codeInterpreterTools) {
		this.chatModel = chatModel;
		this.stackConfig = stackConfig;
		this.codeInterpreterTools = codeInterpreterTools;
		this.specialistTools = specialistToolProvider.getTools(SupervisorTools.TOOL_TABLE_QUERY,
				SupervisorTools.TOOL_JSON_SCHEMA, SupervisorTools.TOOL_FILE_SUMMARY);
		this.renderedSystemPrompt = renderSystemPrompt();
	}

	public SampleSheetSupervisor create() {
		return new SampleSheetSupervisor(chatModel, stackConfig, specialistTools, codeInterpreterTools, renderedSystemPrompt);
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
