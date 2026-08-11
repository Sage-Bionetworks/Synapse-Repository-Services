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
import org.sagebionetworks.repo.manager.agent.CodeInterpreterSessionProvider;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

@ExtendWith(MockitoExtension.class)
public class CurieSupervisorFactoryTest {

	@Mock
	private ChatModel mockChatModel;

	@Mock
	private StackConfiguration mockStackConfig;

	@Mock
	private SpecialistToolProvider mockSpecialistToolProvider;

	@Mock
	private CodeInterpreterTools mockCodeInterpreterTools;

	@Mock
	private CodeInterpreterSessionProvider mockSessionProvider;

	@Mock
	private ChatMemoryRepository mockMemoryRepository;

	@Mock
	private ToolCallback mockToolCallback;

	private CurieSupervisorFactory factory;

	@BeforeEach
	public void setup() {
		// The factory selects its specialist subset in the constructor.
		when(mockSpecialistToolProvider.getTools(SupervisorTools.TOOL_JSON_SCHEMA, SupervisorTools.TOOL_GRID_QUERY,
				SupervisorTools.TOOL_GRID_UPDATE, SupervisorTools.TOOL_GRID_METADATA, SupervisorTools.TOOL_FILE_SUMMARY))
				.thenReturn(List.of(mockToolCallback));
		factory = new CurieSupervisorFactory(mockChatModel, mockStackConfig, mockSpecialistToolProvider,
				mockCodeInterpreterTools, mockSessionProvider, mockMemoryRepository);
	}

	@Test
	public void testCreate() {
		// call under test
		CurieSupervisor supervisor = factory.create();

		assertNotNull(supervisor);
	}

	@Test
	public void testSelectsOnlyCurationSpecialists() {
		// The factory must request exactly the JSON schema, grid, and file summary specialists, not the full set.
		verify(mockSpecialistToolProvider).getTools(SupervisorTools.TOOL_JSON_SCHEMA, SupervisorTools.TOOL_GRID_QUERY,
				SupervisorTools.TOOL_GRID_UPDATE, SupervisorTools.TOOL_GRID_METADATA, SupervisorTools.TOOL_FILE_SUMMARY);
	}

	@Test
	public void testRenderSystemPrompt() {
		// call under test
		String prompt = factory.renderSystemPrompt();

		assertNotNull(prompt);
		assertTrue(prompt.contains("Curie"));
		assertTrue(prompt.contains(SupervisorTools.TOOL_JSON_SCHEMA));
		assertTrue(prompt.contains(SupervisorTools.TOOL_GRID_QUERY));
		assertTrue(prompt.contains(SupervisorTools.TOOL_GRID_UPDATE));
		assertTrue(prompt.contains(SupervisorTools.TOOL_GRID_METADATA));
		assertTrue(prompt.contains(SupervisorTools.TOOL_FILE_SUMMARY));
		assertTrue(prompt.contains("PREVIEW"));
		assertTrue(prompt.contains("Confirm before committing"));
	}
}
