package org.sagebionetworks.repo.manager.agent.specialist.entitymetadata;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * Factory for creating {@link EntityMetadataSpecialist} instances. Each instance gets a fresh
 * conversation memory and a pre-rendered system prompt.
 */
@Service
public class EntityMetadataSpecialistFactory {

	static final String PROMPT_TEMPLATE = "prompts/entity-metadata-specialist.vtp";

	private final ChatModel chatModel;
	private final StackConfiguration stackConfig;
	private final EntityMetadataSpecialistTools entityMetadataSpecialistTools;
	private final CodeInterpreterTools codeInterpreterTools;
	private final VelocityEngine velocityEngine;
	private final String renderedSystemPrompt;

	public EntityMetadataSpecialistFactory(ChatModel chatModel, StackConfiguration stackConfig,
			EntityMetadataSpecialistTools entityMetadataSpecialistTools, CodeInterpreterTools codeInterpreterTools,
			VelocityEngine velocityEngine) {
		this.chatModel = chatModel;
		this.stackConfig = stackConfig;
		this.entityMetadataSpecialistTools = entityMetadataSpecialistTools;
		this.codeInterpreterTools = codeInterpreterTools;
		this.velocityEngine = velocityEngine;
		this.renderedSystemPrompt = renderSystemPrompt();
	}

	public EntityMetadataSpecialist create() {
		return new EntityMetadataSpecialist(chatModel, stackConfig, entityMetadataSpecialistTools, codeInterpreterTools,
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
