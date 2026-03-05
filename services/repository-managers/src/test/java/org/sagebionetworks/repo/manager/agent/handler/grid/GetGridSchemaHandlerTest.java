package org.sagebionetworks.repo.manager.agent.handler.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.agent.SessionContext;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class GetGridSchemaHandlerTest {

	@Mock
	private GridDao mockGridDao;
	@Mock
	private JsonSchemaManager mockJsonSchemaManager;

	@InjectMocks
	private GetGridSchemaHandler handler;

	private Long runAsUserId;
	private String actionGroup;
	private String function;
	private List<Parameter> parameters;
	private List<Parameter> requestBody;
	private SessionContext context;
	private String gridSessionId;
	private Long replicaId;
	private ReturnControlEvent event;
	private JsonSchema schema;
	private String schemaId;

	@BeforeEach
	public void before() {
		runAsUserId = 123L;
		actionGroup = "action-group";
		function = "function";
		parameters = List.of();
		requestBody = null;
		gridSessionId = "g123";
		replicaId = 1999L;
		context = new GridAgentSessionContext().setGridSessionId(gridSessionId).setUsersReplicaId(replicaId);
		event = new ReturnControlEvent(runAsUserId, actionGroup, function, parameters, requestBody, context);
		schemaId = "some.org-some.schema";
		schema = new JsonSchema().set$id(schemaId);
	}

	@Test
	public void testHandleEvent() throws Exception {
		when(mockGridDao.getGridSession(gridSessionId))
				.thenReturn(Optional.of(new GridSession().setGridJsonSchema$Id(schemaId).setSessionId(gridSessionId)));
		when(mockJsonSchemaManager.getValidationSchema(schemaId)).thenReturn(schema);
		// call under test
		String json = handler.handleEvent(event);
		assertEquals("{\"$id\":\"some.org-some.schema\"}", json);
	}

	@Test
	public void testHandleEventWithEmptySession() throws Exception {
		when(mockGridDao.getGridSession(gridSessionId)).thenReturn(Optional.empty());
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			handler.handleEvent(event);
		}).getMessage();
		assertEquals("Grid session no longer exists", message);

		verifyZeroInteractions(mockJsonSchemaManager);
	}

	@Test
	public void testHandleEventWithNullSchemaId() throws Exception {
		when(mockGridDao.getGridSession(gridSessionId))
				.thenReturn(Optional.of(new GridSession().setGridJsonSchema$Id(null).setSessionId(gridSessionId)));
		// call under test
		String json = handler.handleEvent(event);
		assertEquals("{}", json);
		verifyZeroInteractions(mockJsonSchemaManager);
	}

	@Test
	public void testHandleEventWithNoContext() throws Exception {
		event = new ReturnControlEvent(runAsUserId, actionGroup, function, parameters, requestBody, null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.handleEvent(event);
		}).getMessage();
		assertEquals("GridAgentSessionContext cannot be null", message);

		verifyZeroInteractions(mockGridDao, mockJsonSchemaManager);
	}

}
