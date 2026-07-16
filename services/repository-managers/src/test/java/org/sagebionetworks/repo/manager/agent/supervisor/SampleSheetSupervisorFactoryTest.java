package org.sagebionetworks.repo.manager.agent.supervisor;

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
public class SampleSheetSupervisorFactoryTest {

	@Mock
	private ChatModel mockChatModel;

	@Mock
	private StackConfiguration mockStackConfig;

	@Mock
	private SupervisorTools mockSupervisorTools;

	@Mock
	private CodeInterpreterTools mockCodeInterpreterTools;

	private SampleSheetSupervisorFactory factory;

	@BeforeEach
	public void setup() {
		factory = new SampleSheetSupervisorFactory(mockChatModel, mockStackConfig, mockSupervisorTools, mockCodeInterpreterTools);
	}

	@Test
	public void testCreate() {
		// call under test
		SampleSheetSupervisor supervisor = factory.create();

		assertNotNull(supervisor);
	}

	@Test
	public void testRenderSystemPrompt() {
		// call under test
		String prompt = factory.renderSystemPrompt();

		assertNotNull(prompt);
		assertTrue(prompt.contains("sample sheet generation supervisor"));
		assertTrue(prompt.contains("askTableQuerySpecialist"));
		assertTrue(prompt.contains("askJsonSchemaSpecialist"));
		assertTrue(prompt.contains("askFileSummarySpecialist"));
		assertTrue(prompt.contains("RESULT: SUCCESS"));
		assertTrue(prompt.contains("RESULT: ERROR"));
	}
}
