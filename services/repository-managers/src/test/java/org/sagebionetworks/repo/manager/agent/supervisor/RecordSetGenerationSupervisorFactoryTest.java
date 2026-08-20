package org.sagebionetworks.repo.manager.agent.supervisor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.Agent;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.sagebionetworks.repo.manager.config.ManagerConfiguration;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

@ExtendWith(MockitoExtension.class)
public class RecordSetGenerationSupervisorFactoryTest {

	@Mock
	private ChatModel mockChatModel;

	@Mock
	private StackConfiguration mockStackConfig;

	@Mock
	private SpecialistToolProvider mockSpecialistToolProvider;

	@Mock
	private CodeInterpreterTools mockCodeInterpreterTools;

	@Mock
	private ToolCallback mockToolCallback;

	private RecordSetGenerationSupervisorFactory factory;

	@BeforeEach
	public void setup() {
		// The factory selects its specialist subset in the constructor.
		when(mockSpecialistToolProvider.getTools(SupervisorTools.TOOL_ENTITY_METADATA, SupervisorTools.TOOL_JSON_SCHEMA,
				SupervisorTools.TOOL_FILE_SUMMARY)).thenReturn(List.of(mockToolCallback));
		factory = new RecordSetGenerationSupervisorFactory(mockChatModel, mockStackConfig, mockSpecialistToolProvider,
				mockCodeInterpreterTools, new ManagerConfiguration().velocityEngine());
	}

	@Test
	public void testCreate() {
		// call under test
		Agent supervisor = factory.create();

		assertNotNull(supervisor);
	}

	@Test
	public void testSelectsOnlyRecordSetSpecialists() {
		// The factory must request exactly the entity metadata, JSON schema, and file summary specialists — no grid tools.
		verify(mockSpecialistToolProvider).getTools(SupervisorTools.TOOL_ENTITY_METADATA, SupervisorTools.TOOL_JSON_SCHEMA,
				SupervisorTools.TOOL_FILE_SUMMARY);
	}

	@Test
	public void testRenderSystemPrompt() {
		// call under test
		String prompt = factory.renderSystemPrompt();

		assertNotNull(prompt);
		assertTrue(prompt.contains("RecordSet generation supervisor"));
		assertTrue(prompt.contains("askEntityMetadataSpecialist"));
		assertTrue(prompt.contains("askJsonSchemaSpecialist"));
		assertTrue(prompt.contains("askFileSummarySpecialist"));
		assertTrue(prompt.contains("RESULT: SUCCESS"));
		assertTrue(prompt.contains("RESULT: ERROR"));
	}
}
