package org.sagebionetworks.repo.manager.agent.specialist.filesummary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;

@ExtendWith(MockitoExtension.class)
public class FileSummaryToolsTest {

	@Mock
	private AgentCoreCodeInterpreterClient mockCodeInterpreterClient;

	private FileSummaryTools tools;
	private ToolContext toolContext;
	private ToolContext toolContextWithSession;

	@BeforeEach
	public void setup() {
		tools = new FileSummaryTools(mockCodeInterpreterClient);
		toolContext = new ToolContext(Map.of());
		toolContextWithSession = new ToolContext(Map.of("sessionId", "session-123"));
	}

	@Test
	public void testInspectFileWithValidFile() {
		CodeExecutionResult result = new CodeExecutionResult(
				"path: query_specialist/results.csv\nsize_bytes: 42\nline_count: 3\nhead:\nname,age\nAlice,30\n", false, List.of());
		ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
		when(mockCodeInterpreterClient.executeCode(eq("session-123"), eq("python"), scriptCaptor.capture()))
				.thenReturn(result);

		// call under test
		String response = tools.inspectFile("query_specialist/results.csv", toolContextWithSession);

		assertTrue(response.contains("line_count: 3"));
		assertTrue(response.contains("size_bytes: 42"));

		// The rendered script must target the requested file and pass the byte cap
		String script = scriptCaptor.getValue();
		assertTrue(script.contains("query_specialist/results.csv"));
		assertTrue(script.contains(String.valueOf(FileSummaryTools.MAX_HEAD_BYTES)));
		verify(mockCodeInterpreterClient).executeCode(eq("session-123"), eq("python"), eq(script));
	}

	@Test
	public void testInspectFileWithNoSessionId() {
		// call under test
		String response = tools.inspectFile("query_specialist/results.csv", toolContext);

		assertEquals("Error: No code interpreter session ID available", response);
		verifyNoInteractions(mockCodeInterpreterClient);
	}

	@Test
	public void testInspectFileWithExecutionError() {
		CodeExecutionResult result = new CodeExecutionResult("PermissionError: denied", true, List.of());
		when(mockCodeInterpreterClient.executeCode(eq("session-123"), eq("python"), org.mockito.ArgumentMatchers.anyString()))
				.thenReturn(result);

		// call under test
		String response = tools.inspectFile("secret.csv", toolContextWithSession);

		assertTrue(response.contains("Error inspecting file 'secret.csv'"));
		assertTrue(response.contains("PermissionError: denied"));
	}

	@Test
	public void testExtractPdfTextWithWholeDocument() {
		CodeExecutionResult result = new CodeExecutionResult("page_count: 3\n--- page 1 ---\nhello\n", false, List.of());
		ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
		when(mockCodeInterpreterClient.executeCode(eq("session-123"), eq("python"), scriptCaptor.capture()))
				.thenReturn(result);

		// call under test — no page number means whole document (rendered as 0)
		String response = tools.extractPdfText("summary_specialist/report.pdf", null, toolContextWithSession);

		assertTrue(response.contains("page_count: 3"));
		String script = scriptCaptor.getValue();
		assertTrue(script.contains("summary_specialist/report.pdf"));
		assertTrue(script.contains("page_number = 0"));
		assertTrue(script.contains(String.valueOf(FileSummaryTools.MAX_PDF_TEXT_CHARS)));
	}

	@Test
	public void testExtractPdfTextWithSpecificPage() {
		CodeExecutionResult result = new CodeExecutionResult("page: 2\ntext:\nregional sales\n", false, List.of());
		ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
		when(mockCodeInterpreterClient.executeCode(eq("session-123"), eq("python"), scriptCaptor.capture()))
				.thenReturn(result);

		// call under test
		String response = tools.extractPdfText("summary_specialist/report.pdf", 2, toolContextWithSession);

		assertTrue(response.contains("page: 2"));
		String script = scriptCaptor.getValue();
		assertTrue(script.contains("page_number = 2"));
	}

	@Test
	public void testExtractPdfTextWithNoSessionId() {
		// call under test
		String response = tools.extractPdfText("summary_specialist/report.pdf", null, toolContext);

		assertEquals("Error: No code interpreter session ID available", response);
		verifyNoInteractions(mockCodeInterpreterClient);
	}

	@Test
	public void testExtractPdfTextWithExecutionError() {
		CodeExecutionResult result = new CodeExecutionResult("boom", true, List.of());
		when(mockCodeInterpreterClient.executeCode(eq("session-123"), eq("python"), org.mockito.ArgumentMatchers.anyString()))
				.thenReturn(result);

		// call under test
		String response = tools.extractPdfText("summary_specialist/report.pdf", null, toolContextWithSession);

		assertTrue(response.contains("Error extracting text from PDF 'summary_specialist/report.pdf'"));
		assertTrue(response.contains("boom"));
	}

	@Test
	public void testTruncateOutputWithNull() {
		// call under test
		assertEquals("", tools.truncateOutput(null));
	}

	@Test
	public void testTruncateOutputWithShortString() {
		// call under test
		assertEquals("hello", tools.truncateOutput("hello"));
	}

	@Test
	public void testTruncateOutputWithLongString() {
		String longOutput = "x".repeat(FileSummaryTools.MAX_RESPONSE_CHARS + 500);

		// call under test
		String truncated = tools.truncateOutput(longOutput);

		assertEquals(FileSummaryTools.MAX_RESPONSE_CHARS,
				truncated.substring(0, truncated.indexOf("\n...")).length());
		assertTrue(truncated.endsWith("... [truncated at " + FileSummaryTools.MAX_RESPONSE_CHARS + " chars]"));
	}
}
