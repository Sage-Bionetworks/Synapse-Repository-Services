package org.sagebionetworks.repo.manager.agent.specialist.gridquery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.config.ManagerConfiguration;
import org.springframework.ai.chat.model.ChatModel;

@ExtendWith(MockitoExtension.class)
public class GridQuerySpecialistFactoryTest {

	@Mock
	private ChatModel mockChatModel;

	@Mock
	private StackConfiguration mockStackConfig;

	@Mock
	private GridQueryTools mockGridQueryTools;

	private GridQuerySpecialistFactory factory;

	@BeforeEach
	public void setup() {
		factory = new GridQuerySpecialistFactory(mockChatModel, mockStackConfig, mockGridQueryTools,
				new ManagerConfiguration().velocityEngine());
	}

	@Test
	public void testCreate() {
		// call under test
		GridQuerySpecialist specialist = factory.create();

		assertNotNull(specialist);
	}

	@Test
	public void testRenderSystemPrompt() {
		// call under test
		String prompt = factory.renderSystemPrompt();

		assertNotNull(prompt);
		assertTrue(prompt.contains("grid query specialist"));
		// The request structure now lives in the tool's generated input schema, not the prompt, so the
		// prompt no longer embeds the generated schema paragraph (the FQN keys of the $defs envelope).
		assertFalse(prompt.contains("\"org.sagebionetworks.repo.model.grid.query.CellValueFilter\": {"));
		assertFalse(prompt.contains("structure (generated from the model)"));
		// At least one conformance-checked example is still rendered into the prompt.
		assertTrue(prompt.contains("\"species\""));
	}
}
