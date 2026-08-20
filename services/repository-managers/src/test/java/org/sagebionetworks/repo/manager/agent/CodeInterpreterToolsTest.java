package org.sagebionetworks.repo.manager.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.RunPythonRequest;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

@ExtendWith(MockitoExtension.class)
public class CodeInterpreterToolsTest {

	@Mock
	private AgentCoreCodeInterpreterClient codeInterpreterClient;
	@Mock
	private CodeExecutionResult codeExecutionResult;

	private CodeInterpreterTools tools;
	private UserInfo userInfo;

	@BeforeEach
	public void before() {
		tools = new CodeInterpreterTools(codeInterpreterClient);
		userInfo = new UserInfo(false, 123L, AuthorizationConstants.DEFAULT_REALM_ID);
	}

	@Test
	public void testRunPythonWithSuccessfulExecution() {
		String script = "print(2 + 2)";
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(
				Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo, AgentToolContextKey.CODE_SESSION_SUPPLIER.getKey(), CodeSessionSupplier.of(sessionId)));

		when(codeInterpreterClient.executeCode(sessionId, "python", script)).thenReturn(codeExecutionResult);
		when(codeExecutionResult.isError()).thenReturn(false);
		when(codeExecutionResult.textOutput()).thenReturn("4\n");

		// call under test
		String result = tools.runPython(new RunPythonRequest().setScript(script), toolContext);

		assertEquals("4\n", result);
		verify(codeInterpreterClient).executeCode(sessionId, "python", script);
	}

	@Test
	public void testRunPythonWithError() {
		String script = "raise ValueError('bad')";
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(
				Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo, AgentToolContextKey.CODE_SESSION_SUPPLIER.getKey(), CodeSessionSupplier.of(sessionId)));

		when(codeInterpreterClient.executeCode(sessionId, "python", script)).thenReturn(codeExecutionResult);
		when(codeExecutionResult.isError()).thenReturn(true);
		when(codeExecutionResult.textOutput()).thenReturn("ValueError: bad");

		// call under test
		String result = tools.runPython(new RunPythonRequest().setScript(script), toolContext);

		assertTrue(result.startsWith("Error: "));
		assertTrue(result.contains("ValueError: bad"));
	}

	@Test
	public void testRunPythonWithSessionSupplier() {
		String script = "print(2 + 2)";
		String sessionId = "lazyResolvedSession";
		// The interactive Curie path installs a supplier rather than a raw sessionId; runPython must
		// invoke it to resolve (and lazily create) the session.
		CodeSessionSupplier supplier = () -> sessionId;
		ToolContext toolContext = new ToolContext(
				Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo, AgentToolContextKey.CODE_SESSION_SUPPLIER.getKey(), supplier));

		when(codeInterpreterClient.executeCode(sessionId, "python", script)).thenReturn(codeExecutionResult);
		when(codeExecutionResult.isError()).thenReturn(false);
		when(codeExecutionResult.textOutput()).thenReturn("4\n");

		// call under test
		String result = tools.runPython(new RunPythonRequest().setScript(script), toolContext);

		assertEquals("4\n", result);
		verify(codeInterpreterClient).executeCode(sessionId, "python", script);
	}

	@Test
	public void testRunPythonWithMissingSessionId() {
		String script = "print('hello')";
		ToolContext toolContext = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo));

		// call under test
		String result = tools.runPython(new RunPythonRequest().setScript(script), toolContext);

		assertEquals("Error: No code interpreter session ID available", result);
		verifyNoInteractions(codeInterpreterClient);
	}

	@Test
	public void testRunPythonWithOutputTruncation() {
		String script = "print('x' * 20000)";
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(
				Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo, AgentToolContextKey.CODE_SESSION_SUPPLIER.getKey(), CodeSessionSupplier.of(sessionId)));

		String longOutput = "x".repeat(20_000);
		when(codeInterpreterClient.executeCode(sessionId, "python", script)).thenReturn(codeExecutionResult);
		when(codeExecutionResult.isError()).thenReturn(false);
		when(codeExecutionResult.textOutput()).thenReturn(longOutput);

		// call under test
		String result = tools.runPython(new RunPythonRequest().setScript(script), toolContext);

		assertEquals(CodeInterpreterTools.MAX_RESPONSE_CHARS, result.substring(0, result.indexOf("\n...")).length());
		assertTrue(result.endsWith("... [truncated at " + CodeInterpreterTools.MAX_RESPONSE_CHARS + " chars]"));
		assertTrue(result.length() < longOutput.length());
	}

	@Test
	public void testTruncateOutputWithNull() {
		// call under test
		assertEquals("", tools.truncateOutput(null));
	}

	@Test
	public void testTruncateOutputWithShortString() {
		String shortOutput = "hello world";

		// call under test
		assertEquals(shortOutput, tools.truncateOutput(shortOutput));
	}

	@Test
	public void testGetToolCallbacksExposesRunPython() {
		// call under test
		List<ToolCallback> callbacks = tools.getToolCallbacks();

		assertEquals(1, callbacks.size());
		assertEquals("runPython", callbacks.get(0).getToolDefinition().name());
	}

	@Test
	public void testRunPythonCallbackWithValidJson() {
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(
				Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo, AgentToolContextKey.CODE_SESSION_SUPPLIER.getKey(), CodeSessionSupplier.of(sessionId)));
		when(codeInterpreterClient.executeCode(sessionId, "python", "print(2 + 2)")).thenReturn(codeExecutionResult);
		when(codeExecutionResult.isError()).thenReturn(false);
		when(codeExecutionResult.textOutput()).thenReturn("4\n");

		// call under test -- drive the tool through the ToolCallback the model actually invokes.
		String result = runPythonCallback().call("{\"script\":\"print(2 + 2)\"}", toolContext);

		assertEquals("4\n", result);
		verify(codeInterpreterClient).executeCode(sessionId, "python", "print(2 + 2)");
	}

	@Test
	public void testRunPythonCallbackWithUnescapedControlCharacter() {
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(
				Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo, AgentToolContextKey.CODE_SESSION_SUPPLIER.getKey(), CodeSessionSupplier.of(sessionId)));
		// A raw (unescaped) newline inside the JSON string value is technically invalid JSON, and the
		// model routinely emits a multi-line script this way. The base normalizes it through the lenient
		// mapper before parsing, so the newline is preserved in the script rather than costing a round trip.
		String multiLineScript = "line1\nline2";
		when(codeInterpreterClient.executeCode(sessionId, "python", multiLineScript)).thenReturn(codeExecutionResult);
		when(codeExecutionResult.isError()).thenReturn(false);
		when(codeExecutionResult.textOutput()).thenReturn("ok\n");

		// call under test
		String result = runPythonCallback().call("{\"script\":\"line1\nline2\"}", toolContext);

		assertEquals("ok\n", result);
		verify(codeInterpreterClient).executeCode(sessionId, "python", multiLineScript);
	}

	private ToolCallback runPythonCallback() {
		ToolCallback callback = tools.getToolCallbacks().stream()
				.filter(c -> "runPython".equals(c.getToolDefinition().name())).findFirst().orElse(null);
		assertNotNull(callback);
		return callback;
	}
}
