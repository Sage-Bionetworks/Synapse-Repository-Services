package org.sagebionetworks.repo.manager.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
public class CodeInterpreterFileManagerTest {

	@Mock
	private S3Client mockS3Client;
	@Mock
	private S3Presigner mockS3Presigner;
	@Mock
	private AgentCoreCodeInterpreterClient mockCodeInterpreterClient;
	@Mock
	private StackConfiguration mockStackConfig;
	@Mock
	private PresignedGetObjectRequest mockPresignedGetRequest;
	@Mock
	private PresignedPutObjectRequest mockPresignedPutRequest;
	@Mock
	private CodeExecutionResult mockCodeResult;

	@TempDir
	Path tempDir;

	private CodeInterpreterFileManager fileManager;

	@BeforeEach
	public void setup() {
		when(mockStackConfig.getStack()).thenReturn("dev");
		fileManager = new CodeInterpreterFileManager(mockS3Client, mockS3Presigner, mockCodeInterpreterClient, mockStackConfig);
	}

	@Test
	public void testGetStagingBucket() {
		assertEquals("dev.code-interpreter.staging.sagebase.org", fileManager.getStagingBucket());
	}

	@Test
	public void testPushS3FileToSession() throws Exception {
		String sessionId = "session-1";
		String sourceBucket = "source-bucket";
		String sourceKey = "path/to/file.csv";
		String sessionPath = "data.csv";

		when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());
		when(mockS3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresignedGetRequest);
		when(mockPresignedGetRequest.url()).thenReturn(new URL("https://staging.s3.amazonaws.com/path/to/file.csv?presigned=true"));
		when(mockCodeInterpreterClient.executeCode(eq(sessionId), eq("python"), any(String.class))).thenReturn(mockCodeResult);

		// call under test
		CodeExecutionResult result = fileManager.pushS3FileToSession(sessionId, sourceBucket, sourceKey, sessionPath);

		assertEquals(mockCodeResult, result);

		ArgumentCaptor<CopyObjectRequest> copyCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
		verify(mockS3Client).copyObject(copyCaptor.capture());
		assertEquals("source-bucket", copyCaptor.getValue().sourceBucket());
		assertEquals("path/to/file.csv", copyCaptor.getValue().sourceKey());
		assertEquals("dev.code-interpreter.staging.sagebase.org", copyCaptor.getValue().destinationBucket());
		assertEquals("path/to/file.csv", copyCaptor.getValue().destinationKey());

		verify(mockCodeInterpreterClient).executeCode(eq(sessionId), eq("python"), any(String.class));
	}

	@Test
	public void testPushLocalFileToSession() throws Exception {
		String sessionId = "session-1";
		File localFile = tempDir.resolve("test.csv").toFile();
		try (FileWriter writer = new FileWriter(localFile)) {
			writer.write("name,age\nAlice,30\n");
		}
		String sessionPath = "query_specialist/results.csv";

		when(mockS3Client.putObject(any(PutObjectRequest.class), any(Path.class))).thenReturn(PutObjectResponse.builder().build());
		when(mockS3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresignedGetRequest);
		when(mockPresignedGetRequest.url()).thenReturn(new URL("https://staging.s3.amazonaws.com/uuid/test.csv?presigned=true"));
		when(mockCodeInterpreterClient.executeCode(eq(sessionId), eq("python"), any(String.class))).thenReturn(mockCodeResult);

		// call under test
		CodeExecutionResult result = fileManager.pushLocalFileToSession(sessionId, localFile, "text/csv", sessionPath);

		assertEquals(mockCodeResult, result);

		ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(mockS3Client).putObject(putCaptor.capture(), eq(localFile.toPath()));
		assertEquals("dev.code-interpreter.staging.sagebase.org", putCaptor.getValue().bucket());
		assertEquals("text/csv", putCaptor.getValue().contentType());

		verify(mockCodeInterpreterClient).executeCode(eq(sessionId), eq("python"), any(String.class));
	}

	@Test
	public void testPullFileFromSessionWithSuccess() throws Exception {
		String sessionId = "session-1";
		String sessionPath = "output/report.csv";
		String contentType = "text/csv";
		String userId = "123";

		when(mockS3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedPutRequest);
		when(mockPresignedPutRequest.url()).thenReturn(new URL("https://staging.s3.amazonaws.com/123/uuid/report.csv?presigned=true"));
		when(mockCodeInterpreterClient.executeCode(eq(sessionId), eq("python"), any(String.class))).thenReturn(mockCodeResult);
		when(mockCodeResult.isError()).thenReturn(false);
		when(mockCodeResult.textOutput()).thenReturn("abc123def456abc123def456abc12345:2048\n");

		// call under test
		CodeInterpreterFileManager.PullResult result = fileManager.pullFileFromSession(sessionId, sessionPath, contentType, userId);

		assertEquals("dev.code-interpreter.staging.sagebase.org", result.bucket());
		assertEquals("abc123def456abc123def456abc12345", result.md5());
		assertEquals(2048L, result.contentSize());
		// Key starts with userId
		assertTrue(result.key().startsWith("123/"));
		assertTrue(result.key().endsWith("/report.csv"));

		verify(mockCodeInterpreterClient).executeCode(eq(sessionId), eq("python"), any(String.class));
	}

	@Test
	public void testPullFileFromSessionWithError() throws Exception {
		String sessionId = "session-1";
		String sessionPath = "missing.csv";
		String contentType = "text/csv";
		String userId = "123";

		when(mockS3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedPutRequest);
		when(mockPresignedPutRequest.url()).thenReturn(new URL("https://example.com/presigned"));
		when(mockCodeInterpreterClient.executeCode(eq(sessionId), eq("python"), any(String.class))).thenReturn(mockCodeResult);
		when(mockCodeResult.isError()).thenReturn(true);
		when(mockCodeResult.textOutput()).thenReturn("FileNotFoundError: missing.csv");

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class, () ->
				fileManager.pullFileFromSession(sessionId, sessionPath, contentType, userId));

		assertTrue(ex.getMessage().contains("FileNotFoundError"));
	}

	@Test
	public void testPullFileFromSessionWithUnexpectedOutput() throws Exception {
		String sessionId = "session-1";
		String sessionPath = "data.csv";
		String contentType = "text/csv";
		String userId = "123";

		when(mockS3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedPutRequest);
		when(mockPresignedPutRequest.url()).thenReturn(new URL("https://example.com/presigned"));
		when(mockCodeInterpreterClient.executeCode(eq(sessionId), eq("python"), any(String.class))).thenReturn(mockCodeResult);
		when(mockCodeResult.isError()).thenReturn(false);
		when(mockCodeResult.textOutput()).thenReturn("unexpected output");

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class, () ->
				fileManager.pullFileFromSession(sessionId, sessionPath, contentType, userId));

		assertTrue(ex.getMessage().contains("Unexpected output from upload script"));
	}

	@Test
	public void testPullFileFromSessionWithSimpleFileName() throws Exception {
		String sessionId = "session-1";
		String sessionPath = "report.csv";
		String contentType = "text/csv";
		String userId = "456";

		when(mockS3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedPutRequest);
		when(mockPresignedPutRequest.url()).thenReturn(new URL("https://example.com/presigned"));
		when(mockCodeInterpreterClient.executeCode(eq(sessionId), eq("python"), any(String.class))).thenReturn(mockCodeResult);
		when(mockCodeResult.isError()).thenReturn(false);
		when(mockCodeResult.textOutput()).thenReturn("md5hash12345678901234567890abcde:512\n");

		// call under test
		CodeInterpreterFileManager.PullResult result = fileManager.pullFileFromSession(sessionId, sessionPath, contentType, userId);

		assertTrue(result.key().startsWith("456/"));
		assertTrue(result.key().endsWith("/report.csv"));
		assertEquals("md5hash12345678901234567890abcde", result.md5());
		assertEquals(512L, result.contentSize());
	}
}
