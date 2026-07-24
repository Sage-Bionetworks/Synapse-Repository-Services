package org.sagebionetworks.repo.manager.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager.PushFileRequest;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager.PushFileResult;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.SessionFileMetadata;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.BatchFileResult;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileResult;
import org.sagebionetworks.repo.model.file.FileResultFailureCode;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
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
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private FileHandleDao mockFileHandleDao;
	@Mock
	private IdGenerator mockIdGenerator;
	@Mock
	private StorageLocationDAO mockStorageLocationDAO;

	@TempDir
	Path tempDir;

	private CodeInterpreterFileManager fileManager;

	@BeforeEach
	public void setup() {
		when(mockStackConfig.getStack()).thenReturn("dev");
		when(mockStackConfig.getS3Bucket()).thenReturn("devdata.sagebase.org");
		fileManager = new CodeInterpreterFileManager(mockS3Client, mockS3Presigner, mockCodeInterpreterClient,
				mockFileHandleManager, mockFileHandleDao, mockIdGenerator, mockStorageLocationDAO, mockStackConfig);
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

	@Test
	public void testPushFileHandlesToSessionWithAuthorizedS3File() throws Exception {
		UserInfo user = new UserInfo(false, 101L);
		String sessionId = "session-1";

		FileHandleAssociation association = new FileHandleAssociation().setFileHandleId("222")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn123");
		PushFileRequest request = new PushFileRequest(association, "entity_metadata_specialist/data.csv");

		S3FileHandle s3Handle = new S3FileHandle();
		s3Handle.setId("222");
		s3Handle.setBucketName("source-bucket");
		s3Handle.setKey("path/to/file.csv");
		s3Handle.setFileName("file.csv");
		FileResult fileResult = new FileResult().setFileHandleId("222").setFileHandle(s3Handle);

		when(mockFileHandleManager.getFileHandleAndUrlBatch(eq(user), any(BatchFileRequest.class)))
				.thenReturn(new BatchFileResult().setRequestedFiles(List.of(fileResult)));
		when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());
		when(mockS3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresignedGetRequest);
		when(mockPresignedGetRequest.url()).thenReturn(new URL("https://staging.s3.amazonaws.com/path/to/file.csv?presigned=true"));
		when(mockCodeInterpreterClient.executeCode(eq(sessionId), eq("python"), any(String.class))).thenReturn(mockCodeResult);
		when(mockCodeResult.isError()).thenReturn(false);

		// call under test
		List<PushFileResult> results = fileManager.pushFileHandlesToSession(user, List.of(request), sessionId);

		assertEquals(1, results.size());
		assertFalse(results.get(0).isError());
		assertEquals(request, results.get(0).request());
		assertEquals(mockCodeResult, results.get(0).execution());

		ArgumentCaptor<BatchFileRequest> batchCaptor = ArgumentCaptor.forClass(BatchFileRequest.class);
		verify(mockFileHandleManager).getFileHandleAndUrlBatch(eq(user), batchCaptor.capture());
		BatchFileRequest batchRequest = batchCaptor.getValue();
		assertTrue(batchRequest.getIncludeFileHandles());
		assertFalse(batchRequest.getIncludePreSignedURLs());
		assertFalse(batchRequest.getIncludePreviewPreSignedURLs());
		assertEquals(List.of(association), batchRequest.getRequestedFiles());

		verify(mockS3Client).copyObject(any(CopyObjectRequest.class));
		verify(mockCodeInterpreterClient).executeCode(eq(sessionId), eq("python"), any(String.class));
	}

	@Test
	public void testPushFileHandlesToSessionWithUnauthorizedFile() {
		UserInfo user = new UserInfo(false, 101L);
		String sessionId = "session-1";

		FileHandleAssociation association = new FileHandleAssociation().setFileHandleId("222")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn123");
		PushFileRequest request = new PushFileRequest(association, "data.csv");

		FileResult fileResult = new FileResult().setFileHandleId("222").setFailureCode(FileResultFailureCode.UNAUTHORIZED);

		when(mockFileHandleManager.getFileHandleAndUrlBatch(eq(user), any(BatchFileRequest.class)))
				.thenReturn(new BatchFileResult().setRequestedFiles(List.of(fileResult)));

		// call under test
		List<PushFileResult> results = fileManager.pushFileHandlesToSession(user, List.of(request), sessionId);

		assertEquals(1, results.size());
		assertTrue(results.get(0).isError());
		assertTrue(results.get(0).error().contains("do not have permission to download"), "Got: " + results.get(0).error());

		// Unauthorized files never touch S3 or the session.
		verifyNoInteractions(mockS3Client);
		verifyNoInteractions(mockS3Presigner);
		verifyNoInteractions(mockCodeInterpreterClient);
	}

	@Test
	public void testPushFileHandlesToSessionWithNonS3File() {
		UserInfo user = new UserInfo(false, 101L);
		String sessionId = "session-1";

		FileHandleAssociation association = new FileHandleAssociation().setFileHandleId("222")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn123");
		PushFileRequest request = new PushFileRequest(association, "data.csv");

		// A resolved-but-non-S3 file handle: the file handle is left null to represent an unsupported type.
		FileResult fileResult = new FileResult().setFileHandleId("222");

		when(mockFileHandleManager.getFileHandleAndUrlBatch(eq(user), any(BatchFileRequest.class)))
				.thenReturn(new BatchFileResult().setRequestedFiles(List.of(fileResult)));

		// call under test
		List<PushFileResult> results = fileManager.pushFileHandlesToSession(user, List.of(request), sessionId);

		assertEquals(1, results.size());
		assertTrue(results.get(0).isError());
		assertTrue(results.get(0).error().contains("not an S3-backed file"));

		verifyNoInteractions(mockS3Client);
		verifyNoInteractions(mockCodeInterpreterClient);
	}

	@Test
	public void testPushFileHandlesToSessionWithUnsupportedType() {
		UserInfo user = new UserInfo(false, 101L);
		String sessionId = "session-1";

		FileHandleAssociation association = new FileHandleAssociation().setFileHandleId("222")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn123");
		PushFileRequest request = new PushFileRequest(association, "data.bin");

		// An image is neither an allowed content type nor an allowed extension.
		S3FileHandle s3Handle = new S3FileHandle();
		s3Handle.setId("222");
		s3Handle.setBucketName("source-bucket");
		s3Handle.setKey("path/to/file.png");
		s3Handle.setFileName("file.png");
		s3Handle.setContentType("image/png");
		FileResult fileResult = new FileResult().setFileHandleId("222").setFileHandle(s3Handle);

		when(mockFileHandleManager.getFileHandleAndUrlBatch(eq(user), any(BatchFileRequest.class)))
				.thenReturn(new BatchFileResult().setRequestedFiles(List.of(fileResult)));

		// call under test
		List<PushFileResult> results = fileManager.pushFileHandlesToSession(user, List.of(request), sessionId);

		assertEquals(1, results.size());
		assertTrue(results.get(0).isError());
		assertTrue(results.get(0).error().contains("type is not supported"), "Got: " + results.get(0).error());

		// Unsupported files never touch S3 or the session.
		verifyNoInteractions(mockS3Client);
		verifyNoInteractions(mockCodeInterpreterClient);
	}

	@Test
	public void testPushFileHandlesToSessionWithTooLargeFile() {
		UserInfo user = new UserInfo(false, 101L);
		String sessionId = "session-1";

		FileHandleAssociation association = new FileHandleAssociation().setFileHandleId("222")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn123");
		PushFileRequest request = new PushFileRequest(association, "data.csv");

		// An allowed type, but one byte over the size limit.
		S3FileHandle s3Handle = new S3FileHandle();
		s3Handle.setId("222");
		s3Handle.setBucketName("source-bucket");
		s3Handle.setKey("path/to/big.csv");
		s3Handle.setFileName("big.csv");
		s3Handle.setContentType("text/csv");
		s3Handle.setContentSize(CodeInterpreterFileManager.MAX_FILE_SIZE_BYTES + 1);
		FileResult fileResult = new FileResult().setFileHandleId("222").setFileHandle(s3Handle);

		when(mockFileHandleManager.getFileHandleAndUrlBatch(eq(user), any(BatchFileRequest.class)))
				.thenReturn(new BatchFileResult().setRequestedFiles(List.of(fileResult)));

		// call under test
		List<PushFileResult> results = fileManager.pushFileHandlesToSession(user, List.of(request), sessionId);

		assertEquals(1, results.size());
		assertTrue(results.get(0).isError());
		assertTrue(results.get(0).error().contains("exceeds the maximum"), "Got: " + results.get(0).error());

		verifyNoInteractions(mockS3Client);
		verifyNoInteractions(mockCodeInterpreterClient);
	}

	@Test
	public void testGetFileMetadataBatchWithMixedResults() {
		UserInfo user = new UserInfo(false, 101L);

		FileHandleAssociation eligible = new FileHandleAssociation().setFileHandleId("1")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn1");
		FileHandleAssociation unauthorized = new FileHandleAssociation().setFileHandleId("2")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn2");
		FileHandleAssociation tooLarge = new FileHandleAssociation().setFileHandleId("3")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn3");

		S3FileHandle okHandle = new S3FileHandle();
		okHandle.setContentType("text/csv");
		okHandle.setFileName("ok.csv");
		okHandle.setContentSize(1024L);

		S3FileHandle bigHandle = new S3FileHandle();
		bigHandle.setContentType("application/pdf");
		bigHandle.setFileName("big.pdf");
		bigHandle.setContentSize(CodeInterpreterFileManager.MAX_FILE_SIZE_BYTES + 1);

		when(mockFileHandleManager.getFileHandleAndUrlBatch(eq(user), any(BatchFileRequest.class)))
				.thenReturn(new BatchFileResult().setRequestedFiles(List.of(
						new FileResult().setFileHandleId("1").setFileHandle(okHandle),
						new FileResult().setFileHandleId("2").setFailureCode(FileResultFailureCode.UNAUTHORIZED),
						new FileResult().setFileHandleId("3").setFileHandle(bigHandle))));

		// call under test
		List<SessionFileMetadata> results = fileManager.getFileMetadataBatch(user,
				List.of(eligible, unauthorized, tooLarge));

		assertEquals(3, results.size());

		// Eligible: downloadable, supported type, within size limit; content metadata populated.
		SessionFileMetadata ok = results.get(0);
		assertTrue(ok.getCanDownload());
		assertTrue(ok.getIsSupportedType());
		assertTrue(ok.getIsWithinSizeLimit());
		assertTrue(ok.getCanAddToSession());
		assertEquals("text/csv", ok.getContentType());
		assertEquals(1024L, ok.getContentSizeBytes());
		assertEquals(eligible, ok.getFileHandleAssociation());

		// Unauthorized: not downloadable; content metadata omitted; the association is still reported.
		SessionFileMetadata denied = results.get(1);
		assertFalse(denied.getCanDownload());
		assertFalse(denied.getCanAddToSession());
		assertNull(denied.getContentType());
		assertNull(denied.getContentSizeBytes());
		assertEquals(unauthorized, denied.getFileHandleAssociation());

		// Too large: downloadable and supported type, but over the size limit.
		SessionFileMetadata big = results.get(2);
		assertTrue(big.getCanDownload());
		assertTrue(big.getIsSupportedType());
		assertFalse(big.getIsWithinSizeLimit());
		assertFalse(big.getCanAddToSession());
		assertTrue(big.getReason().contains("exceeds the maximum"), "Got: " + big.getReason());

		// Reporting metadata never stages files.
		verifyNoInteractions(mockS3Client);
		verifyNoInteractions(mockCodeInterpreterClient);
	}

	@Test
	public void testGetFileFromSessionWithValidFile() {
		UserInfo user = new UserInfo(false, 123L);
		String filePath = "analysis_result.csv";
		String contentType = "text/csv";
		String sessionId = "testSession123";

		CodeInterpreterFileManager.PullResult pullResult = new CodeInterpreterFileManager.PullResult(
				"dev.code-interpreter.staging.sagebase.org", "123/some-uuid/analysis_result.csv",
				"abc123def456abc123def456abc12345", 1024L);
		when(mockS3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedPutRequest);
		try {
			when(mockPresignedPutRequest.url()).thenReturn(new URL("https://example.com/presigned"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		when(mockCodeInterpreterClient.executeCode(eq(sessionId), eq("python"), any(String.class))).thenReturn(mockCodeResult);
		when(mockCodeResult.isError()).thenReturn(false);
		when(mockCodeResult.textOutput()).thenReturn(pullResult.md5() + ":" + pullResult.contentSize() + "\n");

		StorageLocationSetting storageLocation = new S3StorageLocationSetting();
		when(mockStorageLocationDAO.get(StorageLocationDAO.DEFAULT_STORAGE_LOCATION_ID)).thenReturn(storageLocation);
		when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());
		when(mockIdGenerator.generateNewId(IdType.FILE_IDS)).thenReturn(789L);

		S3FileHandle createdHandle = new S3FileHandle();
		createdHandle.setId("789");
		when(mockFileHandleDao.createFile(any(S3FileHandle.class))).thenReturn(createdHandle);

		// call under test
		String result = fileManager.getFileFromSession(user, sessionId, filePath, contentType);

		assertEquals("789", result);

		ArgumentCaptor<CopyObjectRequest> copyCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
		verify(mockS3Client).copyObject(copyCaptor.capture());
		assertEquals("dev.code-interpreter.staging.sagebase.org", copyCaptor.getValue().sourceBucket());
		assertEquals("devdata.sagebase.org", copyCaptor.getValue().destinationBucket());

		ArgumentCaptor<S3FileHandle> handleCaptor = ArgumentCaptor.forClass(S3FileHandle.class);
		verify(mockFileHandleDao).createFile(handleCaptor.capture());
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
		UserInfo user = new UserInfo(false, 123L);
		String filePath = "output/subdir/report.json";
		String contentType = "application/json";
		String sessionId = "testSession123";

		when(mockS3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedPutRequest);
		when(mockPresignedPutRequest.url()).thenReturn(new URL("https://example.com/presigned"));
		when(mockCodeInterpreterClient.executeCode(eq(sessionId), eq("python"), any(String.class))).thenReturn(mockCodeResult);
		when(mockCodeResult.isError()).thenReturn(false);
		when(mockCodeResult.textOutput()).thenReturn("aabbccdd11223344aabbccdd11223344:512\n");

		StorageLocationSetting storageLocation = new S3StorageLocationSetting();
		when(mockStorageLocationDAO.get(StorageLocationDAO.DEFAULT_STORAGE_LOCATION_ID)).thenReturn(storageLocation);
		when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());
		when(mockIdGenerator.generateNewId(IdType.FILE_IDS)).thenReturn(999L);

		S3FileHandle createdHandle = new S3FileHandle();
		createdHandle.setId("999");
		when(mockFileHandleDao.createFile(any(S3FileHandle.class))).thenReturn(createdHandle);

		// call under test — only the last path segment becomes the file name
		String result = fileManager.getFileFromSession(user, sessionId, filePath, contentType);

		assertEquals("999", result);

		ArgumentCaptor<S3FileHandle> handleCaptor = ArgumentCaptor.forClass(S3FileHandle.class);
		verify(mockFileHandleDao).createFile(handleCaptor.capture());
		assertEquals("report.json", handleCaptor.getValue().getFileName());
	}

	@Test
	public void testGetFileFromSessionWithPullError() {
		UserInfo user = new UserInfo(false, 123L);
		String filePath = "result.csv";
		String contentType = "text/csv";
		String sessionId = "testSession123";

		when(mockS3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedPutRequest);
		try {
			when(mockPresignedPutRequest.url()).thenReturn(new URL("https://example.com/presigned"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		when(mockCodeInterpreterClient.executeCode(eq(sessionId), eq("python"), any(String.class))).thenReturn(mockCodeResult);
		when(mockCodeResult.isError()).thenReturn(true);
		when(mockCodeResult.textOutput()).thenReturn("FileNotFoundError: result.csv");

		// call under test — a failed pull propagates and no file handle is created
		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> fileManager.getFileFromSession(user, sessionId, filePath, contentType));
		assertTrue(ex.getMessage().contains("FileNotFoundError"));

		verifyNoInteractions(mockFileHandleDao);
	}
}
