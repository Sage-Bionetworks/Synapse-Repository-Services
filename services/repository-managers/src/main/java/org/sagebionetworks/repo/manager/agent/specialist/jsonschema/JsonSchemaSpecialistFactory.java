package org.sagebionetworks.repo.manager.agent.specialist.jsonschema;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.Agent;
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
	private final VelocityEngine velocityEngine;
	private final String renderedSystemPrompt;

	public JsonSchemaSpecialistFactory(ChatModel chatModel, StackConfiguration stackConfig,
			JsonSchemaTools jsonSchemaTools, CodeInterpreterTools codeInterpreterTools,
			VelocityEngine velocityEngine) {
		this.chatModel = chatModel;
		this.stackConfig = stackConfig;
		this.jsonSchemaTools = jsonSchemaTools;
		this.codeInterpreterTools = codeInterpreterTools;
		this.velocityEngine = velocityEngine;
		this.renderedSystemPrompt = renderSystemPrompt();
	}

	public Agent create() {
		return new JsonSchemaSpecialist(chatModel, stackConfig, jsonSchemaTools, codeInterpreterTools, renderedSystemPrompt);
	}

	String renderSystemPrompt() {
		VelocityContext context = new VelocityContext();
		// The shared engine runs in strict-reference mode, so bind $D to a literal '$' and use ${D} in
		// the template wherever a literal dollar sign is intended. This prompt contains JSON Schema
		// keywords such as $ref, $id, and $schema, which are written as ${D}ref, ${D}id, etc.
		context.put("D", "$");

		Template template = velocityEngine.getTemplate(PROMPT_TEMPLATE);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		return writer.toString();
	}
}
