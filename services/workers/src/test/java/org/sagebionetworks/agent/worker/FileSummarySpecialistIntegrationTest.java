package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.agent.Agent;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.CodeSessionSupplier;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.sagebionetworks.repo.manager.agent.specialist.filesummary.FileSummarySpecialistFactory;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileSummarySpecialistIntegrationTest {

	@Autowired
	private FileSummarySpecialistFactory specialistFactory;

	@Autowired
	private AgentCoreCodeInterpreterClient codeInterpreterClient;

	@Autowired
	private UserManager userManager;

	@Autowired
	private CodeInterpreterFileManager codeInterpreterFileManager;

	@Autowired
	private CodeInterpreterTools codeInterpreterTools;

	private UserInfo adminUser;

	private void setupUser() {
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}

	/**
	 * Builds the tool context the caller hands to the specialist: the acting user and the already-started
	 * code session the file to summarize lives on.
	 */
	private ToolContext toolContext(String sessionId) {
		return new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), adminUser,
				AgentToolContextKey.CODE_SESSION_SUPPLIER.getKey(), CodeSessionSupplier.of(sessionId)));
	}

	/**
	 * Write a known CSV file into the session filesystem so the specialist has something to summarize.
	 */
	private void writeSampleCsv(String sessionId, String path) {
		String script = "import os\n"
				+ "os.makedirs('summary_specialist', exist_ok=True)\n"
				+ "with open('" + path + "', 'w') as f:\n"
				+ "    f.write('name,age,score\\n')\n"
				+ "    f.write('Alice,22,95.5\\n')\n"
				+ "    f.write('Bob,25,87.3\\n')\n"
				+ "    f.write('Charlie,35,92.1\\n')\n"
				+ "print('written')";
		CodeExecutionResult result = codeInterpreterClient.executeCode(sessionId, "python", script);
		assertFalse(result.isError(), "Failed to write sample CSV: " + result.textOutput());
	}

	/**
	 * Copy a binary file from the test classpath onto the session filesystem via the staging bucket.
	 * Used to place the sample PDF on the session.
	 */
	private void writeClasspathBinaryToSession(String sessionId, String classpathResource, String contentType,
			String sessionPath) throws Exception {
		File tempFile = File.createTempFile("it_binary_", ".tmp");
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(classpathResource)) {
			if (in == null) {
				throw new IllegalArgumentException("Cannot find: '" + classpathResource + "' on the classpath");
			}
			Files.copy(in, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			CodeExecutionResult result = codeInterpreterFileManager.pushLocalFileToSession(
					sessionId, tempFile, contentType, sessionPath);
			assertFalse(result.isError(), "Failed to write sample PDF: " + result.textOutput());
		} finally {
			tempFile.delete();
		}
	}

	@Test
	public void testSummarizeCsvFile() {
		setupUser();
		String sessionId = codeInterpreterClient.startSession("fileSummaryIT-" + System.nanoTime());
		try {
			String path = "summary_specialist/people.csv";
			writeSampleCsv(sessionId, path);

			Agent specialist = specialistFactory.create();

			// call under test
			String response = specialist.chat("Summarize the file " + path, toolContext(sessionId));

			assertNotNull(response);
			assertTrue(response.toLowerCase().contains("name") || response.toLowerCase().contains("age")
					|| response.toLowerCase().contains("score") || response.toLowerCase().contains("column"),
					"Summary should mention the CSV columns. Got: " + response);
			assertTrue(response.contains("3") || response.toLowerCase().contains("three"),
					"Summary should indicate 3 data rows. Got: " + response);
		} finally {
			codeInterpreterClient.stopSession(sessionId);
		}
	}

	@Test
	public void testSummarizeMissingFile() {
		setupUser();
		String sessionId = codeInterpreterClient.startSession("fileSummaryIT-" + System.nanoTime());
		try {
			Agent specialist = specialistFactory.create();

			// call under test
			String response = specialist.chat(
					"Summarize the file summary_specialist/does_not_exist.csv", toolContext(sessionId));

			assertNotNull(response);
			assertTrue(response.toLowerCase().contains("not") || response.toLowerCase().contains("exist")
					|| response.toLowerCase().contains("no such") || response.toLowerCase().contains("could not"),
					"Response should indicate the file was not found. Got: " + response);
		} finally {
			codeInterpreterClient.stopSession(sessionId);
		}
	}

	@Test
	public void testSummarizePdfDocument() throws Exception {
		setupUser();
		String sessionId = codeInterpreterClient.startSession("fileSummaryIT-" + System.nanoTime());
		try {
			String path = "summary_specialist/report.pdf";
			writeClasspathBinaryToSession(sessionId, "summarySpecialist/sample-three-page.pdf", "application/pdf", path);

			Agent specialist = specialistFactory.create();

			// call under test — summarize the entire document
			String response = specialist.chat("Summarize the PDF document " + path, toolContext(sessionId));

			assertNotNull(response);
			assertTrue(response.contains("3") || response.toLowerCase().contains("three"),
					"Summary should indicate the document has 3 pages. Got: " + response);
			assertTrue(response.toLowerCase().contains("widget"),
					"Summary should reflect the document's subject. Got: " + response);
		} finally {
			codeInterpreterClient.stopSession(sessionId);
		}
	}

	@Test
	public void testSummarizePdfSecondPage() throws Exception {
		setupUser();
		String sessionId = codeInterpreterClient.startSession("fileSummaryIT-" + System.nanoTime());
		try {
			String path = "summary_specialist/report.pdf";
			writeClasspathBinaryToSession(sessionId, "summarySpecialist/sample-three-page.pdf", "application/pdf", path);

			Agent specialist = specialistFactory.create();

			// call under test — summarize only the second page
			String response = specialist.chat(
					"Summarize the text of the second page of the PDF " + path, toolContext(sessionId));

			assertNotNull(response);
			// The second page is about regional sales; its unique marker is BETA-MARKER-222.
			assertTrue(response.toLowerCase().contains("region") || response.toLowerCase().contains("sales")
					|| response.contains("BETA-MARKER-222"),
					"Summary should reflect the second page's content. Got: " + response);
			// It should NOT be reporting the first or third page markers as the second page.
			assertFalse(response.contains("ALPHA-MARKER-111"),
					"Second-page summary should not surface the first page marker. Got: " + response);
			assertFalse(response.contains("GAMMA-MARKER-333"),
					"Second-page summary should not surface the third page marker. Got: " + response);
		} finally {
			codeInterpreterClient.stopSession(sessionId);
		}
	}

	/**
	 * Validates the runPython tool-argument JSON parsing end-to-end, deterministically (no LLM in the
	 * loop). Bedrock/Claude emit the runPython {@code script} tool-argument with raw (unescaped)
	 * newlines; {@link org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolBase} normalizes those
	 * control characters through its lenient mapper, so the multi-line script parses and executes rather
	 * than costing the model a corrective round trip.
	 * <p>
	 * We drive the real Spring AI {@link ToolCallback} for runPython with a hand-authored arguments
	 * string that contains a genuine multi-line script (raw newlines), exactly as the model would send
	 * it. This exercises the same tool-argument parse the base performs, then really executes the script
	 * on the session. We verify by the script's printed side effect, so the test only passes if the
	 * multi-line argument actually parsed and executed with its newlines preserved.
	 */
	@Test
	public void testRunMultiLinePythonScriptToolCall() {
		setupUser();
		// Resolve the real runPython ToolCallback from the actual bean. CodeInterpreterTools is a
		// JSONEntityToolBase, so its callbacks come from getToolCallbacks(), not @Tool reflection.
		ToolCallback runPython = codeInterpreterTools.getToolCallbacks().stream()
				.filter(cb -> "runPython".equals(cb.getToolDefinition().name()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("runPython tool callback not found"));

		// A multi-line Python script with RAW newlines embedded in the JSON string value — the way a
		// model emits it when it forgets to escape control characters. The base normalizes those raw
		// control characters through the lenient mapper before parsing, so the newlines are preserved in
		// the script and it runs. A unique marker printed by the last line proves the multi-line script
		// parsed and executed.
		String marker = "multiline-marker-" + System.nanoTime();
		String script = "import os\n"
				+ "os.makedirs('summary_specialist', exist_ok=True)\n"
				+ "print('" + marker + "')";
		String toolArguments = "{\"script\": \"" + script.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";

		String sessionId = codeInterpreterClient.startSession("multiLineIT-" + System.nanoTime());
		try {
			// call under test — the real Spring AI tool-callback path, including the arguments JSON parse.
			ToolContext toolContext = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), adminUser,
					AgentToolContextKey.CODE_SESSION_SUPPLIER.getKey(), CodeSessionSupplier.of(sessionId)));
			String toolResult = runPython.call(toolArguments, toolContext);

			assertNotNull(toolResult);
			assertTrue(toolResult.contains(marker),
					"Multi-line script should have parsed and executed, printing its marker. Got: " + toolResult);
		} finally {
			codeInterpreterClient.stopSession(sessionId);
		}
	}
}
