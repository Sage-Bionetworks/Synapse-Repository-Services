package org.sagebionetworks.file.worker;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.repo.manager.AuthorizationManagerUtil.AUTHORIZED;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationAuthorizationStatus;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.FileDownloadCode;
import org.sagebionetworks.repo.model.file.FileDownloadStatus;
import org.sagebionetworks.repo.model.file.FileDownloadSummary;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.sqs.model.Message;
import com.google.common.collect.Lists;

public class BulkFileDownloadWorkerTest {

	AsynchJobStatusManager mockAsynchJobStatusManager;
	UserManager mockUserManger;
	BulkDownloadManager mockBulkDownloadManager;

	ProgressCallback<Message> mockProgress;

	BulkFileDownloadWorker worker;

	ZipOutputStream mockZipOut;
	FileHandleAssociation fha1;
	FileHandleAssociation fha2;

	UserInfo user;
	AsynchronousJobStatus jobStatus;
	BulkFileDownloadRequest requestBody;
	Message message;
	S3FileHandle fileHandle1;
	S3FileHandle fileHandle2;

	List<File> mockTempFilesCreated;
	List<ZipOutputStream> mockZipOutCreated;
	List<File> mockDownloadedFiles;

	S3FileHandle resultHandle;

	@Before
	public void before() throws Exception {
		// dependencies
		mockAsynchJobStatusManager = Mockito.mock(AsynchJobStatusManager.class);
		mockUserManger = Mockito.mock(UserManager.class);
		mockBulkDownloadManager = Mockito.mock(BulkDownloadManager.class);
		mockProgress = Mockito.mock(ProgressCallback.class);

		worker = new BulkFileDownloadWorker();
		ReflectionTestUtils.setField(worker, "asynchJobStatusManager",
				mockAsynchJobStatusManager);
		ReflectionTestUtils.setField(worker, "userManger", mockUserManger);
		ReflectionTestUtils.setField(worker, "bulkDownloadManager",
				mockBulkDownloadManager);

		// test objects
		fha1 = new FileHandleAssociation();
		fha1.setFileHandleId("1");
		fha1.setAssociateObjectId("123");
		fha1.setAssociateObjectType(FileHandleAssociateType.TableEntity);

		fha2 = new FileHandleAssociation();
		fha2.setFileHandleId("2");
		fha2.setAssociateObjectId("123");
		fha2.setAssociateObjectType(FileHandleAssociateType.TableEntity);

		requestBody = new BulkFileDownloadRequest();
		requestBody.setRequestedFiles(Arrays.asList(fha1));

		user = new UserInfo(false, 777L);

		jobStatus = new AsynchronousJobStatus();
		jobStatus.setJobId("9999");
		jobStatus.setRequestBody(requestBody);
		jobStatus.setStartedByUserId(user.getId());
		message = MessageUtils.buildMessage(jobStatus);

		fileHandle1 = new S3FileHandle();
		fileHandle1.setId(fha1.getFileHandleId());
		fileHandle1.setFileName("foo.txt");
		fileHandle1.setContentSize(10L);

		fileHandle2 = new S3FileHandle();
		fileHandle2.setId(fha2.getFileHandleId());
		fileHandle2.setFileName("bar.txt");
		fileHandle2.setContentSize(10L);

		// mock test objects
		mockZipOut = Mockito.mock(ZipOutputStream.class);
		when(mockUserManger.getUserInfo(user.getId())).thenReturn(user);
		// User can download the file.
		when(mockBulkDownloadManager.canDownLoadFile(user, Arrays.asList(fha1)))
				.thenReturn(
						Arrays.asList(new FileHandleAssociationAuthorizationStatus(
								fha1, AUTHORIZED)));
		when(mockBulkDownloadManager.getS3FileHandle(fha1.getFileHandleId()))
				.thenReturn(fileHandle1);
		when(mockBulkDownloadManager.getS3FileHandle(fha2.getFileHandleId()))
				.thenReturn(fileHandle2);

		// Create and track a mock file for each temp requested.
		mockTempFilesCreated = Lists.newLinkedList();
		doAnswer(new Answer<File>() {
			@Override
			public File answer(InvocationOnMock invocation) throws Throwable {
				File mockFile = Mockito.mock(File.class);
				mockTempFilesCreated.add(mockFile);
				return mockFile;
			}
		}).when(mockBulkDownloadManager).createTempFile(anyString(), anyString());

		// Create and track a mock downloaded files.
		mockDownloadedFiles = Lists.newLinkedList();
		doAnswer(new Answer<File>() {
			@Override
			public File answer(InvocationOnMock invocation) throws Throwable {
				File mockFile = Mockito.mock(File.class);
				mockDownloadedFiles.add(mockFile);
				return mockFile;
			}
		}).when(mockBulkDownloadManager)
				.downloadToTempFile(any(S3FileHandle.class));

		// create and track the ZipOutputStreams
		mockZipOutCreated = Lists.newLinkedList();
		doAnswer(new Answer<ZipOutputStream>() {
			@Override
			public ZipOutputStream answer(InvocationOnMock invocation)
					throws Throwable {
				ZipOutputStream out = Mockito.mock(ZipOutputStream.class);
				mockZipOutCreated.add(out);
				return out;
			}
		}).when(mockBulkDownloadManager).createZipOutputStream(any(File.class));

		// setup the result handle
		resultHandle = new S3FileHandle();
		resultHandle.setId("1111");
		when(
				mockBulkDownloadManager.multipartUploadLocalFile(
						any(UserInfo.class), any(File.class), anyString(),
						any(ProgressListener.class))).thenReturn(resultHandle);
	}

	@Test
	public void testCreateZipEntryName() {
		assertEquals("321/321321/foo.txt",
				BulkFileDownloadWorker.createZipEntryName("foo.txt", 321321));
	}

	@Test
	public void testCreateZipEntrySmall() {
		assertEquals("1/1/foo.txt",
				BulkFileDownloadWorker.createZipEntryName("foo.txt", 1L));
	}

	@Test
	public void testRunHappy() throws Exception {
		worker.run(mockProgress, message);
		verify(mockProgress, times(1)).progressMade(message);
		verify(mockAsynchJobStatusManager, times(1)).updateJobProgress(
				anyString(), anyLong(), anyLong(), anyString());

		// temp file for the zip should be created
		assertEquals(1, mockTempFilesCreated.size());
		// ZipOutputStream should be created for the zip.
		assertEquals(1, mockZipOutCreated.size());
		// one file should have been downloaded
		assertEquals(1, mockDownloadedFiles.size());
		verifyAllStreamsClosedAndFilesDeleted();
		
		// The zip should get uploaded
		verify(mockBulkDownloadManager, times(1)).multipartUploadLocalFile(
				any(UserInfo.class), any(File.class), anyString(),
				any(ProgressListener.class));
		
		ArgumentCaptor<String> entryCapture = ArgumentCaptor
				.forClass(String.class);
		verify(mockBulkDownloadManager).addFileToZip(any(ZipOutputStream.class),
				any(File.class), entryCapture.capture());
		assertEquals("1/1/foo.txt", entryCapture.getValue());

		// expect the job to be completed with the response body.
		FileDownloadSummary summary = new FileDownloadSummary();
		summary.setFileHandleId(fha1.getFileHandleId());
		summary.setAssociateObjectId(fha1.getAssociateObjectId());
		summary.setAssociateObjectType(fha1.getAssociateObjectType());
		summary.setStatus(FileDownloadStatus.SUCCESS);
		summary.setZipEntryName("1/1/foo.txt");
		BulkFileDownloadResponse expectedResponse = new BulkFileDownloadResponse();
		expectedResponse.setResultZipFileHandleId(resultHandle.getId());
		expectedResponse.setFileSummary(Arrays.asList(summary));
		verify(mockAsynchJobStatusManager).setComplete(jobStatus.getJobId(),
				expectedResponse);
	}

	/**
	 * If no files are added to the zip then the zip should not get uploaded and
	 * the result FileHandle.id should be null.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRunNoFilesAdded() throws Exception {
		String deniedReason = "because";
		// for this case the user cannot download the one file
		when(mockBulkDownloadManager.canDownLoadFile(user, Arrays.asList(fha1)))
				.thenReturn(
						Arrays.asList(new FileHandleAssociationAuthorizationStatus(
								fha1, AuthorizationManagerUtil
										.accessDenied(deniedReason))));
		// call under test.
		worker.run(mockProgress, message);
		
		verifyAllStreamsClosedAndFilesDeleted();
		// The zip should not get uploaded
		verify(mockBulkDownloadManager, never()).multipartUploadLocalFile(
				any(UserInfo.class), any(File.class), anyString(),
				any(ProgressListener.class));
		// expect the job to be completed with the response body.
		FileDownloadSummary summary = new FileDownloadSummary();
		summary.setFileHandleId(fha1.getFileHandleId());
		summary.setAssociateObjectId(fha1.getAssociateObjectId());
		summary.setAssociateObjectType(fha1.getAssociateObjectType());
		summary.setStatus(FileDownloadStatus.FAILURE);
		summary.setFailureCode(FileDownloadCode.UNAUTHORIZED);
		summary.setFailureMessage(deniedReason);
		BulkFileDownloadResponse expectedResponse = new BulkFileDownloadResponse();
		expectedResponse.setResultZipFileHandleId(null);
		expectedResponse.setFileSummary(Arrays.asList(summary));
		verify(mockAsynchJobStatusManager).setComplete(jobStatus.getJobId(),
				expectedResponse);
	}

	/**
	 * For case where there is mixed success, a result zip should be created
	 * with the success files included.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRunNoMixedSuccess() throws Exception {
		// setup a 2 file request
		requestBody.setRequestedFiles(Arrays.asList(fha1, fha2));
		message = MessageUtils.buildMessage(jobStatus);
		
		String deniedReason = "because";
		// for this case 1 is denied and 2 is authorized.
		when(
				mockBulkDownloadManager.canDownLoadFile(user,
						Arrays.asList(fha1, fha2))).thenReturn(
				Arrays.asList(
						new FileHandleAssociationAuthorizationStatus(fha1,
								AuthorizationManagerUtil
										.accessDenied(deniedReason)),
						new FileHandleAssociationAuthorizationStatus(fha2,
								AUTHORIZED)));
		// call under test.
		worker.run(mockProgress, message);
		// progress should be made for each file.
		verify(mockProgress, times(2)).progressMade(message);
		verify(mockAsynchJobStatusManager, times(2)).updateJobProgress(
				anyString(), anyLong(), anyLong(), anyString());
		
		verifyAllStreamsClosedAndFilesDeleted();
		// The zip should get uploaded
		verify(mockBulkDownloadManager, times(1)).multipartUploadLocalFile(
				any(UserInfo.class), any(File.class), anyString(),
				any(ProgressListener.class));
		// expect the job to be completed with the response body.
		// 1
		FileDownloadSummary summary1 = new FileDownloadSummary();
		summary1.setFileHandleId(fha1.getFileHandleId());
		summary1.setAssociateObjectId(fha1.getAssociateObjectId());
		summary1.setAssociateObjectType(fha1.getAssociateObjectType());
		summary1.setStatus(FileDownloadStatus.FAILURE);
		summary1.setFailureCode(FileDownloadCode.UNAUTHORIZED);
		summary1.setFailureMessage(deniedReason);
		// 2
		FileDownloadSummary summary2 = new FileDownloadSummary();
		summary2.setFileHandleId(fha2.getFileHandleId());
		summary2.setAssociateObjectId(fha2.getAssociateObjectId());
		summary2.setAssociateObjectType(fha2.getAssociateObjectType());
		summary2.setStatus(FileDownloadStatus.SUCCESS);
		summary2.setZipEntryName("2/2/bar.txt");
		// response
		BulkFileDownloadResponse expectedResponse = new BulkFileDownloadResponse();
		expectedResponse.setResultZipFileHandleId(resultHandle.getId());
		expectedResponse.setFileSummary(Arrays.asList(summary1, summary2));
		verify(mockAsynchJobStatusManager).setComplete(jobStatus.getJobId(),
				expectedResponse);
	}
	
	/**
	 * Case where file handle does not exist.
	 * @throws Exception
	 */
	@Test
	public void testRunNotFoundException() throws Exception {
		String error = "does not exist";
		when(mockBulkDownloadManager.getS3FileHandle(fha1.getFileHandleId())).thenThrow(new NotFoundException(error));

		// call under test
		worker.run(mockProgress, message);

		verifyAllStreamsClosedAndFilesDeleted();

		// expect the job to be completed with the response body.
		FileDownloadSummary summary = new FileDownloadSummary();
		summary.setFileHandleId(fha1.getFileHandleId());
		summary.setAssociateObjectId(fha1.getAssociateObjectId());
		summary.setAssociateObjectType(fha1.getAssociateObjectType());
		summary.setStatus(FileDownloadStatus.FAILURE);
		summary.setFailureCode(FileDownloadCode.NOT_FOUND);
		summary.setFailureMessage(error);
		// response
		BulkFileDownloadResponse expectedResponse = new BulkFileDownloadResponse();
		expectedResponse.setResultZipFileHandleId(null);
		expectedResponse.setFileSummary(Arrays.asList(summary));
		verify(mockAsynchJobStatusManager).setComplete(jobStatus.getJobId(),
				expectedResponse);
	}
	
	/**
	 * Unexpected exception thrown while processing a file.
	 * @throws Exception
	 */
	@Test
	public void testRunUnknownException() throws Exception {
		String error = "does not exist";
		when(mockBulkDownloadManager.getS3FileHandle(fha1.getFileHandleId())).thenThrow(new RuntimeException(error));

		// call under test
		worker.run(mockProgress, message);

		verifyAllStreamsClosedAndFilesDeleted();

		// expect the job to be completed with the response body.
		FileDownloadSummary summary = new FileDownloadSummary();
		summary.setFileHandleId(fha1.getFileHandleId());
		summary.setAssociateObjectId(fha1.getAssociateObjectId());
		summary.setAssociateObjectType(fha1.getAssociateObjectType());
		summary.setStatus(FileDownloadStatus.FAILURE);
		summary.setFailureCode(FileDownloadCode.UNKNOWN_ERROR);
		summary.setFailureMessage(error);
		// response
		BulkFileDownloadResponse expectedResponse = new BulkFileDownloadResponse();
		expectedResponse.setResultZipFileHandleId(null);
		expectedResponse.setFileSummary(Arrays.asList(summary));
		verify(mockAsynchJobStatusManager).setComplete(jobStatus.getJobId(),
				expectedResponse);
	}
	
	/**
	 * A single S3File exceeds the max file size.
	 * @throws Exception
	 */
	@Test
	public void testRunFileTooLarge() throws Exception {
		fileHandle1.setContentSize(BulkFileDownloadWorker.MAX_TOTAL_FILE_SIZE_BYTES+1);
		// call under test
		worker.run(mockProgress, message);

		verifyAllStreamsClosedAndFilesDeleted();

		// expect the job to be completed with the response body.
		FileDownloadSummary summary = new FileDownloadSummary();
		summary.setFileHandleId(fha1.getFileHandleId());
		summary.setAssociateObjectId(fha1.getAssociateObjectId());
		summary.setAssociateObjectType(fha1.getAssociateObjectType());
		summary.setStatus(FileDownloadStatus.FAILURE);
		summary.setFailureCode(FileDownloadCode.EXCEEDS_SIZE_LIMIT);
		summary.setFailureMessage(BulkFileDownloadWorker.FILE_EXCEEDS_THE_MAXIMUM_SIZE_LIMIT);
		// response
		BulkFileDownloadResponse expectedResponse = new BulkFileDownloadResponse();
		expectedResponse.setResultZipFileHandleId(null);
		expectedResponse.setFileSummary(Arrays.asList(summary));
		verify(mockAsynchJobStatusManager).setComplete(jobStatus.getJobId(),
				expectedResponse);
	}
	
	/**
	 * The zip has grown past the maximum size.
	 * @throws Exception
	 */
	@Test
	public void testRunZipFull() throws Exception {
		// temp file for the zip should be created
		File mockZip = Mockito.mock(File.class);
		when(mockBulkDownloadManager.createTempFile(anyString(), anyString())).thenReturn(mockZip);
		when(mockZip.length()).thenReturn(BulkFileDownloadWorker.MAX_TOTAL_FILE_SIZE_BYTES+1);
		fileHandle1.setContentSize(1L);
		// call under test
		worker.run(mockProgress, message);
		// expect the job to be completed with the response body.
		FileDownloadSummary summary = new FileDownloadSummary();
		summary.setFileHandleId(fha1.getFileHandleId());
		summary.setAssociateObjectId(fha1.getAssociateObjectId());
		summary.setAssociateObjectType(fha1.getAssociateObjectType());
		summary.setStatus(FileDownloadStatus.FAILURE);
		summary.setFailureCode(FileDownloadCode.EXCEEDS_SIZE_LIMIT);
		summary.setFailureMessage(BulkFileDownloadWorker.RESULT_FILE_HAS_REACHED_THE_MAXIMUM_SIZE);
		// response
		BulkFileDownloadResponse expectedResponse = new BulkFileDownloadResponse();
		expectedResponse.setResultZipFileHandleId(null);
		expectedResponse.setFileSummary(Arrays.asList(summary));
		verify(mockAsynchJobStatusManager).setComplete(jobStatus.getJobId(),
				expectedResponse);
	}
	
	/**
	 * The case where two files are requested with the same FileHandl.id.
	 * The file should only be added to the zip once.
	 * @throws Exception
	 */
	@Test
	public void testRunDuplicateFileHandleId() throws Exception {
		// both files have the same FileHandleId
		fha2.setFileHandleId(fha1.getFileHandleId());
		requestBody.setRequestedFiles(Arrays.asList(fha1, fha2));
		message = MessageUtils.buildMessage(jobStatus);
	
		// for this case 1 is denied and 2 is authorized.
		when(
				mockBulkDownloadManager.canDownLoadFile(user,
						Arrays.asList(fha1, fha2))).thenReturn(
				Arrays.asList(
						new FileHandleAssociationAuthorizationStatus(fha1,
								AUTHORIZED),
						new FileHandleAssociationAuthorizationStatus(fha2,
								AUTHORIZED)));
		// call under test.
		worker.run(mockProgress, message);

		verifyAllStreamsClosedAndFilesDeleted();
		// The zip should get uploaded
		verify(mockBulkDownloadManager, times(1)).multipartUploadLocalFile(
				any(UserInfo.class), any(File.class), anyString(),
				any(ProgressListener.class));
		// expect the job to be completed with the response body.
		// 1
		FileDownloadSummary summary1 = new FileDownloadSummary();
		summary1.setFileHandleId(fha1.getFileHandleId());
		summary1.setAssociateObjectId(fha1.getAssociateObjectId());
		summary1.setAssociateObjectType(fha1.getAssociateObjectType());
		summary1.setStatus(FileDownloadStatus.SUCCESS);
		summary1.setZipEntryName("1/1/foo.txt");
		// 2
		FileDownloadSummary summary2 = new FileDownloadSummary();
		summary2.setFileHandleId(fha2.getFileHandleId());
		summary2.setAssociateObjectId(fha2.getAssociateObjectId());
		summary2.setAssociateObjectType(fha2.getAssociateObjectType());
		summary2.setStatus(FileDownloadStatus.FAILURE);
		summary2.setFailureCode(FileDownloadCode.DUPLICATE);
		summary2.setFailureMessage(BulkFileDownloadWorker.FILE_ALREADY_ADDED);
		// response
		BulkFileDownloadResponse expectedResponse = new BulkFileDownloadResponse();
		expectedResponse.setResultZipFileHandleId(resultHandle.getId());
		expectedResponse.setFileSummary(Arrays.asList(summary1, summary2));
		verify(mockAsynchJobStatusManager).setComplete(jobStatus.getJobId(),
				expectedResponse);
	}

	/**
	 * Unknown failure occurs before or after each file is processed
	 * should cause the job to fail.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRunUnknownError() throws Exception {
		String error = "something bad";
		RuntimeException exception = new RuntimeException(error);
		when(mockUserManger.getUserInfo(anyLong())).thenThrow(exception);
		// call under test.
		worker.run(mockProgress, message);

		verifyAllStreamsClosedAndFilesDeleted();
		
		verify(mockAsynchJobStatusManager).setJobFailed(jobStatus.getJobId(),
				exception);
	}
	
	/**
	 * Helper to verify that all streams get closed and all files are deleted.
	 * @throws IOException
	 */
	private void verifyAllStreamsClosedAndFilesDeleted() throws IOException {
		// All created temp files should have been deleted.
		for (File mockFile : mockTempFilesCreated) {
			verify(mockFile).delete();
		}
		// All created output stream should have been closed.
		for (ZipOutputStream mockOut : mockZipOutCreated) {
			verify(mockOut, atLeast(1)).close();
		}
		// All created temp files should have been deleted.
		for (File mockFile : mockDownloadedFiles) {
			verify(mockFile).delete();
		}
	}
}
