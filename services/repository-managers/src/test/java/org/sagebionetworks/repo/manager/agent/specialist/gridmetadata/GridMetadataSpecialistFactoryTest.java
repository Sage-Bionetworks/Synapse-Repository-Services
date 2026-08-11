package org.sagebionetworks.repo.manager.agent.specialist.gridmetadata;

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
public class GridMetadataSpecialistFactoryTest {

	@Mock
	private ChatModel mockChatModel;

	@Mock
	private StackConfiguration mockStackConfig;

	@Mock
	private GridMetadataSpecialistTools mockGridMetadataSpecialistTools;

	@Mock
	private CodeInterpreterTools mockCodeInterpreterTools;

	private GridMetadataSpecialistFactory factory;

	@BeforeEach
	public void setup() {
		factory = new GridMetadataSpecialistFactory(mockChatModel, mockStackConfig, mockGridMetadataSpecialistTools,
				mockCodeInterpreterTools);
	}

	@Test
	public void testCreate() {
		// call under test
		GridMetadataSpecialist specialist = factory.create();

		assertNotNull(specialist);
	}

	@Test
	public void testRenderSystemPrompt() {
		// call under test
		String prompt = factory.renderSystemPrompt();

		assertNotNull(prompt);
		assertTrue(prompt.contains("grid metadata specialist"));
		assertTrue(prompt.contains("getGridSession"));
		assertTrue(prompt.contains("getReplicaInfo"));
		assertTrue(prompt.contains("listReplicas"));
		assertTrue(prompt.contains("getUserName"));
	}
}
