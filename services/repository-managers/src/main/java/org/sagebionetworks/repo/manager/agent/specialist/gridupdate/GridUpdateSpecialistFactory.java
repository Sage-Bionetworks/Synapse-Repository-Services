package org.sagebionetworks.repo.manager.agent.specialist.gridupdate;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.grid.GridExamples;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * Factory for creating {@link GridUpdateSpecialist} instances. Each instance gets a fresh
 * conversation memory and a pre-rendered system prompt. The request structure is advertised to the
 * model by the tool's generated input schema, so the prompt supplies only persona, operational
 * rules (including the null-vs-undefined distinction), and conformance-checked examples.
 */
@Service
public class GridUpdateSpecialistFactory {

	static final String PROMPT_TEMPLATE = "prompts/grid-update-specialist.vtp";

	private final ChatModel chatModel;
	private final StackConfiguration stackConfig;
	private final GridUpdateTools gridUpdateTools;
	private final VelocityEngine velocityEngine;
	private final String renderedSystemPrompt;

	public GridUpdateSpecialistFactory(ChatModel chatModel, StackConfiguration stackConfig,
			GridUpdateTools gridUpdateTools, VelocityEngine velocityEngine) {
		this.chatModel = chatModel;
		this.stackConfig = stackConfig;
		this.gridUpdateTools = gridUpdateTools;
		this.velocityEngine = velocityEngine;
		this.renderedSystemPrompt = renderSystemPrompt();
	}

	public GridUpdateSpecialist create() {
		return new GridUpdateSpecialist(chatModel, stackConfig, gridUpdateTools, renderedSystemPrompt);
	}

	String renderSystemPrompt() {
		VelocityContext context = new VelocityContext();
		context.put("updateExamples", GridExamples.getUpdateExamples());

		Template template = velocityEngine.getTemplate(PROMPT_TEMPLATE);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		return writer.toString();
	}
}
