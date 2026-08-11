package org.sagebionetworks.repo.manager.agent.specialist.gridmetadata;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * Factory for creating {@link GridMetadataSpecialist} instances. Each instance gets a fresh
 * conversation memory and a pre-rendered system prompt.
 */
@Service
public class GridMetadataSpecialistFactory {

	static final String PROMPT_TEMPLATE = "prompts/grid-metadata-specialist.vtp";

	private final ChatModel chatModel;
	private final StackConfiguration stackConfig;
	private final GridMetadataSpecialistTools gridMetadataSpecialistTools;
	private final CodeInterpreterTools codeInterpreterTools;
	private final String renderedSystemPrompt;

	public GridMetadataSpecialistFactory(ChatModel chatModel, StackConfiguration stackConfig,
			GridMetadataSpecialistTools gridMetadataSpecialistTools, CodeInterpreterTools codeInterpreterTools) {
		this.chatModel = chatModel;
		this.stackConfig = stackConfig;
		this.gridMetadataSpecialistTools = gridMetadataSpecialistTools;
		this.codeInterpreterTools = codeInterpreterTools;
		this.renderedSystemPrompt = renderSystemPrompt();
	}

	public GridMetadataSpecialist create() {
		return new GridMetadataSpecialist(chatModel, stackConfig, gridMetadataSpecialistTools, codeInterpreterTools,
				renderedSystemPrompt);
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
