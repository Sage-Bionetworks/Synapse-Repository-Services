package org.sagebionetworks.repo.manager.agent.specialist.tablequery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

@ExtendWith(MockitoExtension.class)
public class TableQuerySpecialistFactoryTest {

	@Mock
	private ChatModel mockChatModel;

	@Mock
	private StackConfiguration mockStackConfig;

	@Mock
	private TableQueryTools mockTableQueryTools;

	@Mock
	private CodeInterpreterTools mockCodeInterpreterTools;

	private TableQuerySpecialistFactory factory;

	@BeforeEach
	public void setup() {
		factory = new TableQuerySpecialistFactory(mockChatModel, mockStackConfig, mockTableQueryTools, mockCodeInterpreterTools,
				new ManagerConfiguration().velocityEngine());
	}

	@Test
	public void testCreate() {
		// call under test
		Agent specialist = factory.create();

		assertNotNull(specialist);
	}

	@Test
	public void testRenderSystemPrompt() {
		// call under test
		String prompt = factory.renderSystemPrompt();

		assertNotNull(prompt);
		assertTrue(prompt.contains("Table Query specialist"));
		assertTrue(prompt.contains("Synapse SQL Reference:"));
		assertTrue(prompt.contains("JOINs and sub-queries are NOT supported"));
		assertTrue(prompt.contains("select * from syn123"));
	}

	@Test
	public void testLoadSqlExamples() {
		// call under test
		List<TableQuerySpecialistFactory.SqlExample> examples = factory.loadSqlExamples();

		assertNotNull(examples);
		assertFalse(examples.isEmpty());
		assertNotNull(examples.get(0).getCategory());
		assertNotNull(examples.get(0).getDescription());
		assertNotNull(examples.get(0).getSql());
	}
}
