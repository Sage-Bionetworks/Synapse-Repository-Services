package org.sagebionetworks.repo.manager.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.UserInfo;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;

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
		userInfo = new UserInfo(false);
		userInfo.setId(123L);
	}

	@Test
	public void testRunPythonWithSuccessfulExecution() {
		String script = "print(2 + 2)";
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(Map.of("userInfo", userInfo, "sessionId", sessionId));

		when(codeInterpreterClient.executeCode(sessionId, "python", script)).thenReturn(codeExecutionResult);
		when(codeExecutionResult.isError()).thenReturn(false);
		when(codeExecutionResult.textOutput()).thenReturn("4\n");

		// call under test
		String result = tools.runPython(script, toolContext);

		assertEquals("4\n", result);
		verify(codeInterpreterClient).executeCode(sessionId, "python", script);
	}

	@Test
	public void testRunPythonWithError() {
		String script = "raise ValueError('bad')";
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(Map.of("userInfo", userInfo, "sessionId", sessionId));

		when(codeInterpreterClient.executeCode(sessionId, "python", script)).thenReturn(codeExecutionResult);
		when(codeExecutionResult.isError()).thenReturn(true);
		when(codeExecutionResult.textOutput()).thenReturn("ValueError: bad");

		// call under test
		String result = tools.runPython(script, toolContext);

		assertTrue(result.startsWith("Error: "));
		assertTrue(result.contains("ValueError: bad"));
	}

	@Test
	public void testRunPythonWithMissingSessionId() {
		String script = "print('hello')";
		ToolContext toolContext = new ToolContext(Map.of("userInfo", userInfo));

		// call under test
		String result = tools.runPython(script, toolContext);

		assertEquals("Error: No code interpreter session ID available", result);
		verifyNoInteractions(codeInterpreterClient);
	}

	@Test
	public void testRunPythonWithOutputTruncation() {
		String script = "print('x' * 20000)";
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(Map.of("userInfo", userInfo, "sessionId", sessionId));

		String longOutput = "x".repeat(20_000);
		when(codeInterpreterClient.executeCode(sessionId, "python", script)).thenReturn(codeExecutionResult);
		when(codeExecutionResult.isError()).thenReturn(false);
		when(codeExecutionResult.textOutput()).thenReturn(longOutput);

		// call under test
		String result = tools.runPython(script, toolContext);

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
}
