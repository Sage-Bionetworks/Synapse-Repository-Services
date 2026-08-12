package org.sagebionetworks.repo.manager.agent.specialist.filesummary;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.sagebionetworks.repo.manager.config.ManagerConfiguration;
import org.springframework.ai.chat.model.ChatModel;

@ExtendWith(MockitoExtension.class)
public class FileSummarySpecialistFactoryTest {

	@Mock
	private ChatModel mockChatModel;

	@Mock
	private StackConfiguration mockStackConfig;

	@Mock
	private FileSummaryTools mockFileSummaryTools;

	@Mock
	private CodeInterpreterTools mockCodeInterpreterTools;

	private FileSummarySpecialistFactory factory;

	@BeforeEach
	public void setup() {
		factory = new FileSummarySpecialistFactory(mockChatModel, mockStackConfig, mockFileSummaryTools, mockCodeInterpreterTools,
				new ManagerConfiguration().velocityEngine());
	}

	@Test
	public void testCreate() {
		// call under test
		FileSummarySpecialist specialist = factory.create();

		assertNotNull(specialist);
	}

	@Test
	public void testRenderSystemPrompt() {
		// call under test
		String prompt = factory.renderSystemPrompt();

		assertNotNull(prompt);
		assertTrue(prompt.contains("file summary specialist"));
		assertTrue(prompt.contains("inspectFile"));
		assertTrue(prompt.contains("extractPdfText"));
		assertTrue(prompt.contains("context window"));
		assertTrue(prompt.contains("PDF"));
	}
}
