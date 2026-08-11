package org.sagebionetworks.repo.manager.agent.specialist.gridupdate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.springframework.ai.chat.model.ChatModel;

@ExtendWith(MockitoExtension.class)
public class GridUpdateSpecialistFactoryTest {

	@Mock
	private ChatModel mockChatModel;

	@Mock
	private StackConfiguration mockStackConfig;

	@Mock
	private GridUpdateTools mockGridUpdateTools;

	private GridUpdateSpecialistFactory factory;

	@BeforeEach
	public void setup() {
		factory = new GridUpdateSpecialistFactory(mockChatModel, mockStackConfig, mockGridUpdateTools);
	}

	@Test
	public void testCreate() {
		// call under test
		GridUpdateSpecialist specialist = factory.create();

		assertNotNull(specialist);
	}

	@Test
	public void testRenderSystemPrompt() {
		// call under test
		String prompt = factory.renderSystemPrompt();

		assertNotNull(prompt);
		assertTrue(prompt.contains("grid update specialist"));
		// The request structure now lives in the tool's generated input schema, not the prompt, so the
		// prompt no longer embeds the GridUpdateRequest schema.
		assertFalse(prompt.contains("\"org.sagebionetworks.repo.model.grid.update.LiteralSetValue\": {"));
		// The null-vs-undefined operational rule is still taught in the prompt.
		assertTrue(prompt.contains("undefined"));
		// At least one conformance-checked example is still rendered into the prompt.
		assertTrue(prompt.contains("\"batch\""));
	}
}
