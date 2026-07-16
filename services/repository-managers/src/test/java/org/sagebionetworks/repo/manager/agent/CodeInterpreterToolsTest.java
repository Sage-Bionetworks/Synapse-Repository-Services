package org.sagebionetworks.repo.manager.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;

@ExtendWith(MockitoExtension.class)
public class CodeInterpreterToolsTest {

	@Mock
	private FileHandleManager fileHandleManager;
	@Mock
	private S3Client s3Client;
	@Mock
	private AgentCoreCodeInterpreterClient codeInterpreterClient;
	@Mock
	private CodeInterpreterFileManager codeInterpreterFileManager;
	@Mock
	private StackConfiguration stackConfig;
	@Mock
	private FileHandleDao fileHandleDao;
	@Mock
	private IdGenerator idGenerator;
	@Mock
	private StorageLocationDAO storageLocationDAO;
	@Mock
	private CodeExecutionResult codeExecutionResult;

	private CodeInterpreterTools tools;
	private UserInfo userInfo;

	@BeforeEach
	public void before() {
		when(stackConfig.getS3Bucket()).thenReturn("devdata.sagebase.org");
		tools = new CodeInterpreterTools(fileHandleManager, s3Client, codeInterpreterClient, codeInterpreterFileManager,
				stackConfig, fileHandleDao, idGenerator, storageLocationDAO);
		userInfo = new UserInfo(false);
		userInfo.setId(123L);
	}

	@Test
	public void testAddFileToSessionWithValidFileHandle() throws Exception {
		String fileHandleId = "456";
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(Map.of("userInfo", userInfo, "sessionId", sessionId));

		S3FileHandle s3Handle = new S3FileHandle();
		s3Handle.setId(fileHandleId);
		s3Handle.setBucketName("source-bucket");
		s3Handle.setKey("path/to/data.csv");
		s3Handle.setFileName("data.csv");

		when(fileHandleManager.getRawFileHandle(userInfo, fileHandleId)).thenReturn(s3Handle);
		when(codeInterpreterFileManager.pushS3FileToSession(sessionId, "source-bucket", "path/to/data.csv", "data.csv"))
				.thenReturn(codeExecutionResult);
		when(codeExecutionResult.isError()).thenReturn(false);

		// call under test
		String result = tools.addFileToSession(fileHandleId, toolContext);

		assertEquals("File 'data.csv' is now available at './data.csv'", result);
		verify(codeInterpreterFileManager).pushS3FileToSession(sessionId, "source-bucket", "path/to/data.csv", "data.csv");
	}

	@Test
	public void testAddFileToSessionWithUnauthorizedUser() {
		String fileHandleId = "456";
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(Map.of("userInfo", userInfo, "sessionId", sessionId));

		when(fileHandleManager.getRawFileHandle(userInfo, fileHandleId))
				.thenThrow(new UnauthorizedException("Access denied"));

		// call under test
		try {
			tools.addFileToSession(fileHandleId, toolContext);
		} catch (UnauthorizedException e) {
			// expected
		}

		verifyNoInteractions(s3Client);
		verifyNoInteractions(codeInterpreterFileManager);
	}

	@Test
	public void testAddFileToSessionWithNonS3FileHandle() {
		String fileHandleId = "456";
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(Map.of("userInfo", userInfo, "sessionId", sessionId));

		ExternalFileHandle externalHandle = new ExternalFileHandle();
		externalHandle.setId(fileHandleId);

		when(fileHandleManager.getRawFileHandle(userInfo, fileHandleId)).thenReturn(externalHandle);

		// call under test
		String result = tools.addFileToSession(fileHandleId, toolContext);

		assertEquals("Error: File handle '456' is not an S3-backed file", result);
		verifyNoInteractions(s3Client);
		verifyNoInteractions(codeInterpreterFileManager);
	}

	@Test
	public void testAddFileToSessionWithMissingSessionId() {
		String fileHandleId = "456";
		ToolContext toolContext = new ToolContext(Map.of("userInfo", userInfo));

		// call under test
		String result = tools.addFileToSession(fileHandleId, toolContext);

		assertEquals("Error: No code interpreter session ID available", result);
		verifyNoInteractions(fileHandleManager);
		verifyNoInteractions(s3Client);
		verifyNoInteractions(codeInterpreterFileManager);
	}

	@Test
	public void testAddFileToSessionWithMissingUserInfo() {
		String fileHandleId = "456";
		ToolContext toolContext = new ToolContext(Map.of("sessionId", "testSession123"));

		// call under test
		String result = tools.addFileToSession(fileHandleId, toolContext);

		assertEquals("Error: No user context available", result);
		verifyNoInteractions(fileHandleManager);
		verifyNoInteractions(s3Client);
		verifyNoInteractions(codeInterpreterFileManager);
	}

	@Test
	public void testAddFileToSessionWithExecutionError() throws Exception {
		String fileHandleId = "456";
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(Map.of("userInfo", userInfo, "sessionId", sessionId));

		S3FileHandle s3Handle = new S3FileHandle();
		s3Handle.setId(fileHandleId);
		s3Handle.setBucketName("source-bucket");
		s3Handle.setKey("path/to/data.csv");
		s3Handle.setFileName("data.csv");

		when(fileHandleManager.getRawFileHandle(userInfo, fileHandleId)).thenReturn(s3Handle);
		when(codeInterpreterFileManager.pushS3FileToSession(sessionId, "source-bucket", "path/to/data.csv", "data.csv"))
				.thenReturn(codeExecutionResult);
		when(codeExecutionResult.isError()).thenReturn(true);
		when(codeExecutionResult.textOutput()).thenReturn("Connection timed out");

		// call under test
		String result = tools.addFileToSession(fileHandleId, toolContext);

		assertTrue(result.contains("Error downloading file to session"));
		assertTrue(result.contains("Connection timed out"));
	}

	@Test
	public void testGetFileFromSessionWithValidFile() throws Exception {
		String filePath = "analysis_result.csv";
		String contentType = "text/csv";
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(Map.of("userInfo", userInfo, "sessionId", sessionId));

		CodeInterpreterFileManager.PullResult pullResult = new CodeInterpreterFileManager.PullResult(
				"dev.code-interpreter.staging.sagebase.org", "123/some-uuid/analysis_result.csv",
				"abc123def456abc123def456abc12345", 1024L);
		when(codeInterpreterFileManager.pullFileFromSession(sessionId, filePath, contentType, "123"))
				.thenReturn(pullResult);

		StorageLocationSetting storageLocation = new S3StorageLocationSetting();
		when(storageLocationDAO.get(StorageLocationDAO.DEFAULT_STORAGE_LOCATION_ID)).thenReturn(storageLocation);
		when(s3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());
		when(idGenerator.generateNewId(IdType.FILE_IDS)).thenReturn(789L);

		S3FileHandle createdHandle = new S3FileHandle();
		createdHandle.setId("789");
		when(fileHandleDao.createFile(any(S3FileHandle.class))).thenReturn(createdHandle);

		// call under test
		String result = tools.getFileFromSession(filePath, contentType, toolContext);

		assertEquals("789", result);

		ArgumentCaptor<CopyObjectRequest> copyCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
		verify(s3Client).copyObject(copyCaptor.capture());
		assertEquals("dev.code-interpreter.staging.sagebase.org", copyCaptor.getValue().sourceBucket());
		assertEquals("123/some-uuid/analysis_result.csv", copyCaptor.getValue().sourceKey());
		assertEquals("devdata.sagebase.org", copyCaptor.getValue().destinationBucket());

		ArgumentCaptor<S3FileHandle> handleCaptor = ArgumentCaptor.forClass(S3FileHandle.class);
		verify(fileHandleDao).createFile(handleCaptor.capture());
		S3FileHandle persistedHandle = handleCaptor.getValue();
		assertEquals("devdata.sagebase.org", persistedHandle.getBucketName());
		assertEquals("abc123def456abc123def456abc12345", persistedHandle.getContentMd5());
		assertEquals(1024L, persistedHandle.getContentSize());
		assertEquals("text/csv", persistedHandle.getContentType());
		assertEquals("analysis_result.csv", persistedHandle.getFileName());
		assertEquals("123", persistedHandle.getCreatedBy());
		assertEquals("789", persistedHandle.getId());
	}

	@Test
	public void testGetFileFromSessionWithNestedPath() throws Exception {
		String filePath = "output/subdir/report.json";
		String contentType = "application/json";
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(Map.of("userInfo", userInfo, "sessionId", sessionId));

		CodeInterpreterFileManager.PullResult pullResult = new CodeInterpreterFileManager.PullResult(
				"dev.code-interpreter.staging.sagebase.org", "123/some-uuid/report.json",
				"aabbccdd11223344aabbccdd11223344", 512L);
		when(codeInterpreterFileManager.pullFileFromSession(sessionId, filePath, contentType, "123"))
				.thenReturn(pullResult);

		StorageLocationSetting storageLocation = new S3StorageLocationSetting();
		when(storageLocationDAO.get(StorageLocationDAO.DEFAULT_STORAGE_LOCATION_ID)).thenReturn(storageLocation);
		when(s3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());
		when(idGenerator.generateNewId(IdType.FILE_IDS)).thenReturn(999L);

		S3FileHandle createdHandle = new S3FileHandle();
		createdHandle.setId("999");
		when(fileHandleDao.createFile(any(S3FileHandle.class))).thenReturn(createdHandle);

		// call under test
		String result = tools.getFileFromSession(filePath, contentType, toolContext);

		assertEquals("999", result);

		ArgumentCaptor<S3FileHandle> handleCaptor = ArgumentCaptor.forClass(S3FileHandle.class);
		verify(fileHandleDao).createFile(handleCaptor.capture());
		assertEquals("report.json", handleCaptor.getValue().getFileName());
	}

	@Test
	public void testGetFileFromSessionWithMissingUserInfo() {
		String filePath = "result.csv";
		String contentType = "text/csv";
		ToolContext toolContext = new ToolContext(Map.of("sessionId", "testSession123"));

		// call under test
		String result = tools.getFileFromSession(filePath, contentType, toolContext);

		assertEquals("Error: No user context available", result);
		verifyNoInteractions(codeInterpreterFileManager);
	}

	@Test
	public void testGetFileFromSessionWithMissingSessionId() {
		String filePath = "result.csv";
		String contentType = "text/csv";
		ToolContext toolContext = new ToolContext(Map.of("userInfo", userInfo));

		// call under test
		String result = tools.getFileFromSession(filePath, contentType, toolContext);

		assertEquals("Error: No code interpreter session ID available", result);
		verifyNoInteractions(codeInterpreterFileManager);
	}

	@Test
	public void testGetFileFromSessionWithPullError() {
		String filePath = "result.csv";
		String contentType = "text/csv";
		String sessionId = "testSession123";
		ToolContext toolContext = new ToolContext(Map.of("userInfo", userInfo, "sessionId", sessionId));

		when(codeInterpreterFileManager.pullFileFromSession(sessionId, filePath, contentType, "123"))
				.thenThrow(new RuntimeException("Error uploading file from session: FileNotFoundError: result.csv"));

		// call under test
		try {
			tools.getFileFromSession(filePath, contentType, toolContext);
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains("FileNotFoundError"));
		}

		verifyNoInteractions(fileHandleDao);
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
