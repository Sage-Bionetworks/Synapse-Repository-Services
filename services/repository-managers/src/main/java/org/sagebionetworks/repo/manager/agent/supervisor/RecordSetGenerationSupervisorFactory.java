package org.sagebionetworks.repo.manager.agent.supervisor;

import java.io.StringWriter;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

/**
 * Factory for creating {@link RecordSetGenerationSupervisor} instances. Each instance gets a fresh
 * conversation memory and a pre-rendered system prompt. The supervisor delegates to a focused
 * subset of specialists (entity metadata + JSON schema + file summary), selected by name from
 * {@link SpecialistToolProvider}.
 */
@Service
public class RecordSetGenerationSupervisorFactory {

	static final String PROMPT_TEMPLATE = "prompts/recordset-generation-supervisor.vtp";

	private final ChatModel chatModel;
	private final StackConfiguration stackConfig;
	private final CodeInterpreterTools codeInterpreterTools;
	private final List<ToolCallback> specialistTools;
	private final VelocityEngine velocityEngine;
	private final String renderedSystemPrompt;

	public RecordSetGenerationSupervisorFactory(ChatModel chatModel, StackConfiguration stackConfig,
			SpecialistToolProvider specialistToolProvider, CodeInterpreterTools codeInterpreterTools,
			VelocityEngine velocityEngine) {
		this.chatModel = chatModel;
		this.stackConfig = stackConfig;
		this.codeInterpreterTools = codeInterpreterTools;
		this.specialistTools = specialistToolProvider.getTools(SupervisorTools.TOOL_ENTITY_METADATA,
				SupervisorTools.TOOL_JSON_SCHEMA, SupervisorTools.TOOL_FILE_SUMMARY);
		this.velocityEngine = velocityEngine;
		this.renderedSystemPrompt = renderSystemPrompt();
	}

	public RecordSetGenerationSupervisor create() {
		return new RecordSetGenerationSupervisor(chatModel, stackConfig, specialistTools, codeInterpreterTools,
				renderedSystemPrompt);
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
