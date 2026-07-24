package org.sagebionetworks.repo.manager.agent.specialist.jsonschema;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.springframework.ai.chat.model.ChatModel;

@ExtendWith(MockitoExtension.class)
public class JsonSchemaSpecialistFactoryTest {

	@Mock
	private ChatModel mockChatModel;

	@Mock
	private StackConfiguration mockStackConfig;

	@Mock
	private JsonSchemaTools mockJsonSchemaTools;

	@Mock
	private CodeInterpreterTools mockCodeInterpreterTools;

	private JsonSchemaSpecialistFactory factory;

	@BeforeEach
	public void setup() {
		factory = new JsonSchemaSpecialistFactory(mockChatModel, mockStackConfig, mockJsonSchemaTools, mockCodeInterpreterTools);
	}

	@Test
	public void testCreate() {
		// call under test
		JsonSchemaSpecialist specialist = factory.create();

		assertNotNull(specialist);
	}

	@Test
	public void testRenderSystemPrompt() {
		// call under test
		String prompt = factory.renderSystemPrompt();

		assertNotNull(prompt);
		assertTrue(prompt.contains("JSON Schema specialist"));
		assertTrue(prompt.contains("#/definitions/"));
		assertTrue(prompt.contains("describeSchema"));
		assertTrue(prompt.contains("writeSchemaToSession"));
	}
}
