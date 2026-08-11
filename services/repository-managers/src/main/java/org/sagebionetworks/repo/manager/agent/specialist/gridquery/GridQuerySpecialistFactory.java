package org.sagebionetworks.repo.manager.agent.specialist.gridquery;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.sagebionetworks.StackConfiguration;
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
	private final String renderedSystemPrompt;

	public GridQuerySpecialistFactory(ChatModel chatModel, StackConfiguration stackConfig,
			GridQueryTools gridQueryTools) {
		this.chatModel = chatModel;
		this.stackConfig = stackConfig;
		this.gridQueryTools = gridQueryTools;
		this.renderedSystemPrompt = renderSystemPrompt();
	}

	public GridQuerySpecialist create() {
		return new GridQuerySpecialist(chatModel, stackConfig, gridQueryTools, renderedSystemPrompt);
	}

	String renderSystemPrompt() {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		engine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		engine.setProperty("runtime.references.strict", true);

		VelocityContext context = new VelocityContext();
		context.put("queryExamples", GridExamples.getQueryExamples());

		Template template = engine.getTemplate(PROMPT_TEMPLATE);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		return writer.toString();
	}
}
