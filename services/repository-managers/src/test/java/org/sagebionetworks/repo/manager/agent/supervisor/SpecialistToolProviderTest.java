package org.sagebionetworks.repo.manager.agent.supervisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.specialist.entitymetadata.EntityMetadataSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.filesummary.FileSummarySpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.gridmetadata.GridMetadataSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.gridquery.GridQuerySpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.gridupdate.GridUpdateSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.jsonschema.JsonSchemaSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.tablequery.TableQuerySpecialistFactory;
import org.springframework.ai.tool.ToolCallback;

@ExtendWith(MockitoExtension.class)
public class SpecialistToolProviderTest {

	@Mock
	private TableQuerySpecialistFactory tableQuerySpecialistFactory;
	@Mock
	private JsonSchemaSpecialistFactory jsonSchemaSpecialistFactory;
	@Mock
	private FileSummarySpecialistFactory fileSummarySpecialistFactory;
	@Mock
	private EntityMetadataSpecialistFactory entityMetadataSpecialistFactory;
	@Mock
	private GridQuerySpecialistFactory gridQuerySpecialistFactory;
	@Mock
	private GridUpdateSpecialistFactory gridUpdateSpecialistFactory;
	@Mock
	private GridMetadataSpecialistFactory gridMetadataSpecialistFactory;

	private SpecialistToolProvider provider;

	@BeforeEach
	public void setup() {
		// The tools are never invoked here; the provider only reflects over their @JSONEntityTool annotations.
		SupervisorTools supervisorTools = new SupervisorTools(tableQuerySpecialistFactory, jsonSchemaSpecialistFactory,
				fileSummarySpecialistFactory, entityMetadataSpecialistFactory, gridQuerySpecialistFactory,
				gridUpdateSpecialistFactory, gridMetadataSpecialistFactory);
		provider = new SpecialistToolProvider(supervisorTools);
	}

	@Test
	public void testGetToolsReturnsRequestedToolsInOrder() {
		// call under test
		List<ToolCallback> tools = provider.getTools(SupervisorTools.TOOL_JSON_SCHEMA, SupervisorTools.TOOL_GRID_QUERY,
				SupervisorTools.TOOL_GRID_UPDATE);

		List<String> names = tools.stream().map(t -> t.getToolDefinition().name()).collect(Collectors.toList());
		assertEquals(List.of(SupervisorTools.TOOL_JSON_SCHEMA, SupervisorTools.TOOL_GRID_QUERY,
				SupervisorTools.TOOL_GRID_UPDATE), names);
	}

	@Test
	public void testGetToolsResolvesEveryDelegationTool() {
		// Every declared specialist tool name must resolve to a callback.
		for (String name : List.of(SupervisorTools.TOOL_TABLE_QUERY, SupervisorTools.TOOL_JSON_SCHEMA,
				SupervisorTools.TOOL_FILE_SUMMARY, SupervisorTools.TOOL_ENTITY_METADATA, SupervisorTools.TOOL_GRID_QUERY,
				SupervisorTools.TOOL_GRID_UPDATE, SupervisorTools.TOOL_GRID_METADATA)) {
			// call under test
			List<ToolCallback> tools = provider.getTools(name);
			assertEquals(1, tools.size());
			assertEquals(name, tools.get(0).getToolDefinition().name());
		}
	}

	@Test
	public void testGetToolsWithUnknownName() {
		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> provider.getTools("bogusTool"));
		assertTrue(e.getMessage().contains("bogusTool"));
	}
}
