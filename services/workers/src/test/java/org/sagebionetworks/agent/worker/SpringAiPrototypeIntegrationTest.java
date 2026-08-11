package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.RunPythonRequest;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.springaicommunity.agentcore.artifacts.GeneratedFile;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Integration test validating the Spring AI foundation:
 * <ul>
 *   <li>ChatModel bean injection and Bedrock Converse API connectivity</li>
 *   <li>ToolContext propagation of UserInfo to @Tool methods</li>
 *   <li>ChatMemory persistence across turns</li>
 * </ul>
 *
 * Gated by system property: run with -Dspring.ai.tests.enabled=true
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SpringAiPrototypeIntegrationTest {

	@Autowired
	private ChatModel bedrockChatModel;

	@Autowired
	private StackConfiguration stackConfig;

	@Autowired
	private AgentCoreCodeInterpreterClient codeInterpreterClient;

	@Autowired
	private FileHandleManager fileHandleManager;

	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private SynapseS3Client s3Client;

	@Autowired
	private S3Client s3ClientV2;

	@Autowired
	private UserManager userManager;

	@Autowired
	private CodeInterpreterTools codeInterpreterTools;

	@Autowired
	private CodeInterpreterFileManager codeInterpreterFileManager;

	@Autowired
	private S3Presigner presigner;

	private UserInfo admin;
	private List<S3FileHandle> fileHandlesToDelete = new ArrayList<>();
	private List<String> stagingKeysToDelete = new ArrayList<>();

	private String stagingBucket;

	@BeforeEach
	public void before() {
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		stagingBucket = stackConfig.getStack() + ".code-interpreter.staging.sagebase.org";
	}

	@AfterEach
	public void after() {
		for (String key : stagingKeysToDelete) {
			s3ClientV2.deleteObject(DeleteObjectRequest.builder()
					.bucket(stagingBucket).key(key).build());
		}
		for (S3FileHandle handle : fileHandlesToDelete) {
			s3Client.deleteObject(handle.getBucketName(), handle.getKey());
			if (handle.getId() != null) {
				fileHandleDao.delete(handle.getId());
			}
		}
	}

	@Test
	public void testChatModelBeanInjection() {
		assertNotNull(bedrockChatModel, "ChatModel bean should be autowired from SpringAiConfiguration");
	}

	@Test
	public void testModelIdClaudeHaikuIsValid() {
		String modelId = stackConfig.getModelIdClaudeHaiku();
		// call under test
		String response = ChatClient.builder(bedrockChatModel).build()
				.prompt()
				.user("Reply with exactly: ok")
				.options(BedrockChatOptions.builder().model(modelId).maxTokens(10).build())
				.call()
				.content();
		assertNotNull(response, "Haiku model (" + modelId + ") should return a response");
	}

	@Test
	public void testModelIdClaudeSonnetIsValid() {
		String modelId = stackConfig.getModelIdClaudeSonnet();
		// call under test
		String response = ChatClient.builder(bedrockChatModel).build()
				.prompt()
				.user("Reply with exactly: ok")
				.options(BedrockChatOptions.builder().model(modelId).maxTokens(10).build())
				.call()
				.content();
		assertNotNull(response, "Sonnet model (" + modelId + ") should return a response");
	}

	@Test
	public void testModelIdClaudeOpusIsValid() {
		String modelId = stackConfig.getModelIdClaudeOpus();
		// call under test
		String response = ChatClient.builder(bedrockChatModel).build()
				.prompt()
				.user("Reply with exactly: ok")
				.options(BedrockChatOptions.builder().model(modelId).maxTokens(10).build())
				.call()
				.content();
		assertNotNull(response, "Opus model (" + modelId + ") should return a response");
	}

	@Test
	public void testToolContextPropagatesUserInfo() {
		UserInfo testUser = new UserInfo(false);
		testUser.setId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		ProfileTools tools = new ProfileTools();

		ChatClient chatClient = ChatClient.builder(bedrockChatModel)
				.defaultTools(tools)
				.build();

		String response = chatClient.prompt()
				.user("What is my user ID? Use the getUserId tool to find out.")
				.toolContext(Map.of("userInfo", testUser, "accessLevel", AgentAccessLevel.READ_YOUR_PRIVATE_DATA))
				.call()
				.content();

		assertNotNull(response);
		assertTrue(response.contains(testUser.getId().toString()),
				"Response should contain the user ID from ToolContext. Got: " + response);
	}

	@Test
	public void testChatMemoryAcrossTurns() {
		ChatMemory memory = MessageWindowChatMemory.builder()
				.maxMessages(20)
				.build();

		ChatClient chatClient = ChatClient.builder(bedrockChatModel)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
				.build();

		String conversationId = "test-conversation-" + System.nanoTime();

		chatClient.prompt()
				.user("My favorite color is blue. Remember that.")
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
				.call()
				.content();

		String response = chatClient.prompt()
				.user("What is my favorite color?")
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
				.call()
				.content();

		assertNotNull(response);
		assertTrue(response.toLowerCase().contains("blue"),
				"Response should recall 'blue' from memory. Got: " + response);
	}

	@Test
	public void testCodeInterpreterWithS3CsvAnalysis() throws Exception {
		// Upload a CSV to S3 via FileHandleManager
		String csvContent = String.join("\n",
				"name,age,score",
				"Alice,30,95.5",
				"Bob,25,87.3",
				"Charlie,35,92.1",
				"Diana,28,88.9",
				"Eve,32,91.0");

		S3FileHandle fileHandle = fileHandleManager.createFileFromByteArray(
				admin.getId().toString(), new Date(),
				csvContent.getBytes(StandardCharsets.UTF_8),
				"sample_data.csv", ContentTypeUtil.TEXT_PLAIN_UTF8, null);
		fileHandlesToDelete.add(fileHandle);

		// Copy the file to the staging bucket
		String stagingKey = fileHandle.getKey();
		s3ClientV2.copyObject(CopyObjectRequest.builder()
				.sourceBucket(fileHandle.getBucketName())
				.sourceKey(fileHandle.getKey())
				.destinationBucket(stagingBucket)
				.destinationKey(stagingKey)
				.build());
		stagingKeysToDelete.add(stagingKey);

		// Generate a pre-signed URL for the staging bucket copy
		String presignedUrl;

		presignedUrl = presigner.presignGetObject(
				GetObjectPresignRequest.builder().getObjectRequest(r -> r.bucket(stagingBucket).key(stagingKey))
						.signatureDuration(Duration.ofMinutes(15)).build())
				.url().toString();

		assertNotNull(presignedUrl, "Should generate a pre-signed URL for the staging bucket");

		// Start a code interpreter session and download the CSV via the pre-signed URL
		String sessionId = codeInterpreterClient.startSession("s3CsvTest" + System.nanoTime());
		try {
			String downloadCode = String.join("\n",
					"import urllib.request",
					"url = \"\"\"" + presignedUrl + "\"\"\"",
					"urllib.request.urlretrieve(url, 'sample_data.csv')",
					"print('CSV downloaded successfully')");

			// call under test
			CodeExecutionResult downloadResult = codeInterpreterClient.executeCode(sessionId, "python", downloadCode);
			assertFalse(downloadResult.isError(),
					"CSV download should not error. Output: " + downloadResult.textOutput());
			assertTrue(downloadResult.textOutput().contains("CSV downloaded successfully"));

			// Analyze the CSV and produce a summary output file
			String analyzeCode = String.join("\n",
					"import csv",
					"import json",
					"",
					"with open('sample_data.csv', 'r') as f:",
					"    reader = csv.DictReader(f)",
					"    rows = list(reader)",
					"",
					"ages = [int(r['age']) for r in rows]",
					"scores = [float(r['score']) for r in rows]",
					"",
					"summary = {",
					"    'row_count': len(rows),",
					"    'columns': ['name', 'age', 'score'],",
					"    'avg_age': sum(ages) / len(ages),",
					"    'avg_score': sum(scores) / len(scores),",
					"    'max_score_name': max(rows, key=lambda r: float(r['score']))['name']",
					"}",
					"",
					"with open('analysis_result.json', 'w') as f:",
					"    json.dump(summary, f, indent=2)",
					"",
					"print(json.dumps(summary))");

			// call under test
			CodeExecutionResult analysisResult = codeInterpreterClient.executeCode(sessionId, "python", analyzeCode);
			assertFalse(analysisResult.isError(),
					"Analysis should not error. Output: " + analysisResult.textOutput());
			System.out.println(analysisResult.textOutput());
			assertTrue(analysisResult.textOutput().contains("row_count"));
			assertTrue(analysisResult.textOutput().contains("Alice"));

			// Fetch the result file from the session
			// call under test
			List<GeneratedFile> files = codeInterpreterClient.readFiles(sessionId, List.of("analysis_result.json"));
			assertFalse(files.isEmpty(), "Should retrieve the analysis_result.json file");

			GeneratedFile resultFile = files.get(0);
			assertNotNull(resultFile.data());
			String jsonContent = new String(resultFile.data());
			assertTrue(jsonContent.contains("row_count"),
					"Result file should contain analysis. Got: " + jsonContent);
			assertTrue(jsonContent.contains("avg_score"),
					"Result file should contain avg_score. Got: " + jsonContent);
		} finally {
			codeInterpreterClient.stopSession(sessionId);
		}
	}

	@Test
	public void testGetFileFromSessionRoundTrip() throws Exception {
		String sessionId = codeInterpreterClient.startSession("toolsRoundTrip" + System.nanoTime());
		try {
			// Produce a file in the session.
			CodeExecutionResult createResult = codeInterpreterClient.executeCode(sessionId, "python", String.join("\n",
					"import json",
					"with open('density.json', 'w') as f:",
					"    json.dump([{'city': 'Seattle', 'density': 2030}], f)",
					"print('done')"));
			assertFalse(createResult.isError(), "Create should succeed. Output: " + createResult.textOutput());

			// call under test — export the session file into a new Synapse file handle
			String fileHandleId = codeInterpreterFileManager.getFileFromSession(admin, sessionId, "density.json",
					"application/json");
			assertNotNull(fileHandleId, "Should return a file handle ID");

			// Verify the created file handle
			S3FileHandle exportedHandle = (S3FileHandle) fileHandleDao.get(fileHandleId);
			fileHandlesToDelete.add(exportedHandle);
			assertEquals("density.json", exportedHandle.getFileName());
			assertEquals("application/json", exportedHandle.getContentType());
			assertEquals(admin.getId().toString(), exportedHandle.getCreatedBy());
			assertNotNull(exportedHandle.getContentMd5());
			assertTrue(exportedHandle.getContentSize() > 0);

			// Verify the file content in S3
			String content = new String(
					s3ClientV2.getObjectAsBytes(r -> r.bucket(exportedHandle.getBucketName()).key(exportedHandle.getKey())).asByteArray(),
					StandardCharsets.UTF_8);
			assertTrue(content.contains("Seattle"), "Should contain city data. Got: " + content);
			assertTrue(content.contains("density"), "Should contain density field. Got: " + content);
		} finally {
			codeInterpreterClient.stopSession(sessionId);
		}
	}

	@Test
	public void testRunPythonTool() throws Exception {
		String sessionId = codeInterpreterClient.startSession("runPythonTest" + System.nanoTime());
		try {
			ToolContext toolContext = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), admin,
					AgentToolContextKey.CODE_SESSION_ID.getKey(), sessionId));

			// call under test
			String result = codeInterpreterTools.runPython(
					new RunPythonRequest().setScript("print(sum(range(1, 101)))"), toolContext);

			assertNotNull(result);
			assertTrue(result.contains("5050"), "Should contain the sum 1..100. Got: " + result);
		} finally {
			codeInterpreterClient.stopSession(sessionId);
		}
	}

	@Test
	public void testSessionIsolationCannotSeeOtherSessionFiles() throws Exception {
		String sessionA = codeInterpreterClient.startSession("isolationA" + System.nanoTime());
		String sessionB = codeInterpreterClient.startSession("isolationB" + System.nanoTime());
		try {
			ToolContext contextA = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), admin,
					AgentToolContextKey.CODE_SESSION_ID.getKey(), sessionA));
			ToolContext contextB = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), admin,
					AgentToolContextKey.CODE_SESSION_ID.getKey(), sessionB));

			// Create a file in session A
			String createResult = codeInterpreterTools.runPython(new RunPythonRequest().setScript(String.join("\n",
					"with open('secret_a.txt', 'w') as f:",
					"    f.write('session A secret data')",
					"print('created')")), contextA);
			assertTrue(createResult.contains("created"), "File creation should succeed in session A. Got: " + createResult);

			// Verify session A can read its own file
			String readA = codeInterpreterTools.runPython(new RunPythonRequest().setScript(String.join("\n",
					"with open('secret_a.txt', 'r') as f:",
					"    print(f.read())")), contextA);
			assertTrue(readA.contains("session A secret data"), "Session A should read its own file. Got: " + readA);

			// Verify session B cannot see session A's file
			String readB = codeInterpreterTools.runPython(new RunPythonRequest().setScript(String.join("\n",
					"import os",
					"print(os.path.exists('secret_a.txt'))")), contextB);
			assertTrue(readB.contains("False"), "Session B should NOT see session A's file. Got: " + readB);
		} finally {
			codeInterpreterClient.stopSession(sessionA);
			codeInterpreterClient.stopSession(sessionB);
		}
	}

	/**
	 * Tool class demonstrating ToolContext-based UserInfo propagation.
	 */
	public static class ProfileTools {

		@Tool(description = "Get the current user's numeric ID")
		public String getUserId(ToolContext toolContext) {
			UserInfo userInfo = (UserInfo) toolContext.getContext().get("userInfo");
			if (userInfo == null) {
				return "Error: No user context available";
			}
			return "User ID: " + userInfo.getId();
		}

		@Tool(description = "Get the current user's access level")
		public String getAccessLevel(ToolContext toolContext) {
			AgentAccessLevel accessLevel = (AgentAccessLevel) toolContext.getContext().get("accessLevel");
			if (accessLevel == null) {
				return "Error: No access level in context";
			}
			return "Access Level: " + accessLevel.name();
		}
	}
}
