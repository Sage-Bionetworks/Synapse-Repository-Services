package org.sagebionetworks.repo.manager.agent.specialist.jsonschema;

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
 * Factory for creating {@link JsonSchemaSpecialist} instances. Each instance gets a fresh
 * conversation memory and a pre-rendered system prompt.
 */
@Service
public class JsonSchemaSpecialistFactory {

	static final String PROMPT_TEMPLATE = "prompts/json-schema-specialist.vtp";

	private final ChatModel chatModel;
	private final StackConfiguration stackConfig;
	private final JsonSchemaTools jsonSchemaTools;
	private final CodeInterpreterTools codeInterpreterTools;
	private final String renderedSystemPrompt;

	public JsonSchemaSpecialistFactory(ChatModel chatModel, StackConfiguration stackConfig,
			JsonSchemaTools jsonSchemaTools, CodeInterpreterTools codeInterpreterTools) {
		this.chatModel = chatModel;
		this.stackConfig = stackConfig;
		this.jsonSchemaTools = jsonSchemaTools;
		this.codeInterpreterTools = codeInterpreterTools;
		this.renderedSystemPrompt = renderSystemPrompt();
	}

	public JsonSchemaSpecialist create() {
		return new JsonSchemaSpecialist(chatModel, stackConfig, jsonSchemaTools, codeInterpreterTools, renderedSystemPrompt);
	}

	String renderSystemPrompt() {
		// Note: strict reference mode is intentionally NOT enabled. The prompt contains JSON Schema
		// keywords such as $id, $ref, and $schema, which Velocity would otherwise treat as undefined
		// variable references. In non-strict mode these render literally, which is what we want.
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
