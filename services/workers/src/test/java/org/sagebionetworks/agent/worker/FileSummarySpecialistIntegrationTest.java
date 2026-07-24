package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.sagebionetworks.repo.manager.agent.specialist.filesummary.FileSummarySpecialist;
import org.sagebionetworks.repo.manager.agent.specialist.filesummary.FileSummarySpecialistFactory;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
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

			FileSummarySpecialist specialist = specialistFactory.create();

			// call under test
			String response = specialist.chat("Summarize the file " + path, adminUser, sessionId);

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
			FileSummarySpecialist specialist = specialistFactory.create();

			// call under test
			String response = specialist.chat(
					"Summarize the file summary_specialist/does_not_exist.csv", adminUser, sessionId);

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

			FileSummarySpecialist specialist = specialistFactory.create();

			// call under test — summarize the entire document
			String response = specialist.chat("Summarize the PDF document " + path, adminUser, sessionId);

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

			FileSummarySpecialist specialist = specialistFactory.create();

			// call under test — summarize only the second page
			String response = specialist.chat(
					"Summarize the text of the second page of the PDF " + path, adminUser, sessionId);

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
	 * Validates the SpringAiConfiguration tool-argument JSON parsing fix end-to-end, deterministically
	 * (no LLM in the loop). Bedrock/Claude emit the runPython {@code script} tool-argument with raw
	 * (unescaped) newlines; without the fix this fails during tool-argument parsing with
	 * "Illegal unquoted character (CTRL-CHAR, code 10)" before the tool ever runs.
	 * <p>
	 * We drive the real Spring AI {@link ToolCallback} for runPython with a hand-authored arguments
	 * string that contains a genuine multi-line script (raw newlines), exactly as the model would send
	 * it. This exercises the same {@code MethodToolCallback.call} -> extractToolArguments parse that was
	 * failing, then really executes the script on the session. We verify by the script's side effect,
	 * so the test only passes if the multi-line argument actually parsed and executed.
	 */
	@Test
	public void testRunMultiLinePythonScriptToolCall() {
		setupUser();
		String sessionId = codeInterpreterClient.startSession("fileSummaryIT-" + System.nanoTime());
		try {
			// Resolve the real runPython ToolCallback from the actual bean.
			ToolCallback runPython = Arrays.stream(ToolCallbacks.from(codeInterpreterTools))
					.filter(cb -> "runPython".equals(cb.getToolDefinition().name()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("runPython tool callback not found"));

			String marker = "MULTILINE_OK_" + System.nanoTime();
			// A multi-line Python script with RAW newlines, embedded as a JSON string value the same way
			// the model emits it. The \n here are actual newline characters in the JSON payload.
			String script = "import os\n"
					+ "os.makedirs('summary_specialist', exist_ok=True)\n"
					+ "with open('summary_specialist/multiline_check.txt', 'w') as f:\n"
					+ "    for i in range(3):\n"
					+ "        f.write('line ' + str(i) + '\\n')\n"
					+ "    f.write('" + marker + "\\n')\n"
					+ "print('done')";
			String toolArguments = "{\"script\": \"" + script.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";

			// call under test — the real Spring AI tool-callback path, including the arguments JSON parse.
			ToolContext toolContext = new ToolContext(Map.of("userInfo", adminUser, "sessionId", sessionId));
			String toolResult = runPython.call(toolArguments, toolContext);
			assertNotNull(toolResult);

			// Verify the side effect directly: the file exists and contains the marker the script wrote.
			CodeExecutionResult readResult = codeInterpreterClient.executeCode(sessionId, "python",
					"print(open('summary_specialist/multiline_check.txt').read())");
			assertFalse(readResult.isError(),
					"Multi-line script should have created the file. Got: " + readResult.textOutput());
			assertTrue(readResult.textOutput().contains(marker),
					"File written by the multi-line script should contain the marker. Got: " + readResult.textOutput());
		} finally {
			codeInterpreterClient.stopSession(sessionId);
		}
	}
}
