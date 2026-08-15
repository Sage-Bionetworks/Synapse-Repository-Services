package org.sagebionetworks.repo.manager.agent.supervisor;

import java.io.StringWriter;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.Agent;
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
	private final VelocityEngine velocityEngine;
	private final String renderedSystemPrompt;

	public SampleSheetSupervisorFactory(ChatModel chatModel, StackConfiguration stackConfig,
			SpecialistToolProvider specialistToolProvider, CodeInterpreterTools codeInterpreterTools,
			VelocityEngine velocityEngine) {
		this.chatModel = chatModel;
		this.stackConfig = stackConfig;
		this.codeInterpreterTools = codeInterpreterTools;
		this.specialistTools = specialistToolProvider.getTools(SupervisorTools.TOOL_TABLE_QUERY,
				SupervisorTools.TOOL_JSON_SCHEMA, SupervisorTools.TOOL_FILE_SUMMARY);
		this.velocityEngine = velocityEngine;
		this.renderedSystemPrompt = renderSystemPrompt();
	}

	public Agent create() {
		return new SampleSheetSupervisor(chatModel, stackConfig, specialistTools, codeInterpreterTools, renderedSystemPrompt);
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
