package org.sagebionetworks.repo.manager.agent.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.agent.SessionContext;

public class ReturnControlEventTest {

	private Long runAsUserId;
	private String actionGroup;
	private String function;
	private List<Parameter> parameters;
	private List<Parameter> requestBodyParameters;
	private SessionContext context;
	private ReturnControlEvent event;

	@BeforeEach
	public void before() {
		runAsUserId = 123L;
		actionGroup = "action-group";
		function = "function";
		parameters = null;
		requestBodyParameters = List.of(
				//
				new Parameter("one", "string", "aString"),
				//
				new Parameter("three", "object", "{\"isValid\":true}"),
				//
				new Parameter("four", "array", "[1,2,3]"),
				//
				new Parameter("two", "integer", "123"));
		context = new GridAgentSessionContext().setGridSessionId("123");
		event = new ReturnControlEvent(runAsUserId, actionGroup, function, parameters, requestBodyParameters, context);
	}

	@Test
	public void testGetRequestBody() {

		assertEquals(Optional.of("{\"one\":\"aString\",\"three\":{\"isValid\":true},\"four\":[1,2,3],\"two\":\"123\"}"),
				event.getRequestBody());
	}

	@Test
	public void testGetRequestBodyWithMisingQuotes() {
		requestBodyParameters = List.of(new Parameter("three", "object", "{isValid:true}"));
		event = new ReturnControlEvent(runAsUserId, actionGroup, function, parameters, requestBodyParameters, context);
		assertEquals(Optional.of("{\"three\":{\"isValid\":true}}"), event.getRequestBody());
	}

	@Test
	public void testGetRequestBodyWithNullBody() {
		requestBodyParameters = null;
		event = new ReturnControlEvent(runAsUserId, actionGroup, function, parameters, requestBodyParameters, context);
		assertEquals(Optional.empty(), event.getRequestBody());
	}

	/**
	 * We added this test because the Agents were occasionally providing JSON without quotes.  It was unexpected 
	 * that such a case can be parsed correctly.  Do we need to guarantee that we can parse invalid JSON?
	 */
	@Test
	public void testGetRequestBodyWithInvalidJson() {
		requestBodyParameters = List.of(new Parameter("three", "object", "[123]"));
		event = new ReturnControlEvent(runAsUserId, actionGroup, function, parameters, requestBodyParameters, context);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			event.getRequestBody();
		}).getMessage();
		assertEquals(
				"Failed to parse the JSON value for parameter 'three' with value: [123]. Error: A JSONObject text must begin with '{' at 1 [character 2 line 1]",
				message);
	}
	
	@Test
	public void testGetRequestBodyWithUnknowType() {
		requestBodyParameters = List.of(new Parameter("three", "other", "true"));
		event = new ReturnControlEvent(runAsUserId, actionGroup, function, parameters, requestBodyParameters, context);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			event.getRequestBody();
		}).getMessage();
		assertEquals(
				"Unknown type: other",
				message);
	}

}
