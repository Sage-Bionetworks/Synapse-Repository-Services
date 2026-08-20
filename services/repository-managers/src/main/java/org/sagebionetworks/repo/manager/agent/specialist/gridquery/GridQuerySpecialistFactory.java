package org.sagebionetworks.repo.manager.agent.specialist.gridquery;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.Agent;
import org.sagebionetworks.repo.model.grid.GridExamples;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * Factory for creating {@link GridQuerySpecialist} instances. Each instance gets a fresh
 * conversation memory and a pre-rendered system prompt. The request structure is advertised to the
 * model by the tool's generated input schema, so the prompt supplies only persona, operational
 * rules, and conformance-checked examples.
 */
@Service
public class GridQuerySpecialistFactory {

	static final String PROMPT_TEMPLATE = "prompts/grid-query-specialist.vtp";

	private final ChatModel chatModel;
	private final StackConfiguration stackConfig;
	private final GridQueryTools gridQueryTools;
	private final VelocityEngine velocityEngine;
	private final String renderedSystemPrompt;

	public GridQuerySpecialistFactory(ChatModel chatModel, StackConfiguration stackConfig,
			GridQueryTools gridQueryTools, VelocityEngine velocityEngine) {
		this.chatModel = chatModel;
		this.stackConfig = stackConfig;
		this.gridQueryTools = gridQueryTools;
		this.velocityEngine = velocityEngine;
		this.renderedSystemPrompt = renderSystemPrompt();
	}

	public Agent create() {
		return new GridQuerySpecialist(chatModel, stackConfig, gridQueryTools, renderedSystemPrompt);
	}

	String renderSystemPrompt() {
		VelocityContext context = new VelocityContext();
		context.put("queryExamples", GridExamples.getQueryExamples());

		Template template = velocityEngine.getTemplate(PROMPT_TEMPLATE);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		return writer.toString();
	}
}
