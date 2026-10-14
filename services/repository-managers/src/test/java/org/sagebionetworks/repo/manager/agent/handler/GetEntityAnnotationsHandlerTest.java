package org.sagebionetworks.repo.manager.agent.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.manager.agent.handler.GetEntityAnnotationsHandler;
import org.sagebionetworks.repo.manager.agent.handler.HttpCode;
import org.sagebionetworks.repo.manager.agent.handler.HttpMethod;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.service.EntityService;

@ExtendWith(MockitoExtension.class)
public class GetEntityAnnotationsHandlerTest {

	@Mock
	private EntityService mockEntityService;
	@Mock
	private LoggerProvider mockLoggerProvider;
	@Mock
	private Logger mockLog;

	private Long userId;
	private String entityId;
	private List<Parameter> parameters;
	private ReturnControlEvent event;

	private GetEntityAnnotationsHandler handler;
	private Annotations body;

	@BeforeEach
	private void before() {
		when(mockLoggerProvider.getLogger(GetEntityAnnotationsHandler.class.getName())).thenReturn(mockLog);
		handler = new GetEntityAnnotationsHandler(mockEntityService, mockLoggerProvider);

		userId = 123L;
		entityId = "syn456";
		parameters = List.of(new Parameter("entityId", "string", entityId));
		body = new Annotations().setId(entityId).setEtag("etag");
		event = new ReturnControlEvent(userId, handler.getActionGroup(), handler.getFunction(), parameters);
	}

	@Test
	public void testGetActionGroup() {
		assertEquals("org_sage_one", handler.getActionGroup());
	}

	@Test
	public void testGetFunction() {
		assertEquals("GET /entity/{entityId}/annotations", handler.getFunction());
	}

	@Test
	public void testGetHttpMethod() {
		assertEquals(HttpMethod.get, handler.getHttpMethod());
	}

	@Test
	public void testGetSuccessCode() {
		assertEquals(HttpCode.ok, handler.getSuccessHttpCode());
	}

	@Test
	public void testGetPath() {
		assertEquals("/entity/{entityId}/annotations", handler.getPath());
	}

	@Test
	public void testHandleEvent() throws Exception {

		boolean includeDerived = true;
		when(mockEntityService.getEntityAnnotations(userId, entityId, includeDerived)).thenReturn(body);

		// call under test
		String result = handler.handleEvent(event);
		verify(mockLog).info("Agent called '{}' entityId = {} userId = {} results = '{}'", handler.getFunction(),
				entityId, event.getRunAsUserId(), result);

		assertEquals("{\"id\":\"syn456\",\"etag\":\"etag\"}", result);

	}

	@Test
	public void testHandleEventWithNoParam() throws Exception {

		event = new ReturnControlEvent(userId, handler.getActionGroup(), handler.getFunction(),
				Collections.emptyList());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.handleEvent(event);
		}).getMessage();
		assertEquals("Parameter 'entityId' of type string is required", message);

		verifyNoMoreInteractions(mockLog, mockEntityService);

	}
}
