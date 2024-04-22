package org.sagebionetworks.client.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.util.RandomTempFileUtil;
import org.sagebionetworks.utils.MD5ChecksumHelper;

@ExtendWith(MockitoExtension.class)
public class MultithreadMultipartUploadTest {

	@Mock
	private SynapseClient mockSynapseClient;

	@Mock
	private PartCallableFactory mockPartCallableFactory;

	@Mock
	private ExecutorService mockThreadPool;

	@Mock
	private Future<AddPartResponse> mockFuture;

	@Mock
	private Callable<AddPartResponse> mockCallable;

	@Mock
	private File mockFile;

	@Mock
	private CloseableHttpClient mockHttpClient;

	@Test
	public void testDoUpload() throws FileNotFoundException, SynapseException, IOException {
		int fileSizeBytes = (int) (MultithreadMultipartUpload.MIN_PART_SIZE * 3) + 10;
		RandomTempFileUtil.consumeRandomTempFile(fileSizeBytes, "foo", ".txt", (temp) -> {
			try {

				String fileMD5Hex = MD5ChecksumHelper.getMD5Checksum(temp);

				String uploadId = "111";
				String fileHandleId = "222";
				when(mockSynapseClient.startMultipartUpload(any(), anyBoolean()))
						.thenReturn(new MultipartUploadStatus().setUploadId(uploadId).setPartsState("0000"));
				when(mockThreadPool.submit(any(Callable.class))).thenReturn(mockFuture);
				when(mockPartCallableFactory.createCallable(any())).thenReturn(mockCallable);
				when(mockSynapseClient.completeMultipartUpload(any())).thenReturn(
						new MultipartUploadStatus().setUploadId(uploadId).setResultFileHandleId(fileHandleId));
				when(mockSynapseClient.getRawFileHandle(any()))
						.thenReturn(new GoogleCloudFileHandle().setId(fileHandleId));

				boolean forceRestart = false;
				// call under test
				CloudProviderFileHandleInterface result = MultithreadMultipartUpload.doUpload(mockPartCallableFactory,
						mockHttpClient, mockThreadPool, mockSynapseClient, temp, new MultipartUploadRequest(),
						forceRestart);

				assertEquals(new GoogleCloudFileHandle().setId(fileHandleId), result);

				verify(mockSynapseClient).startMultipartUpload(new MultipartUploadRequest().setContentMD5Hex(fileMD5Hex)
						.setFileName(temp.getName()).setFileSizeBytes((long) fileSizeBytes)
						.setPartSizeBytes(MultithreadMultipartUpload.MIN_PART_SIZE).setContentType("text/plain"),
						forceRestart);

				verify(mockPartCallableFactory, times(4)).createCallable(any());
				// 1
				verify(mockPartCallableFactory).createCallable(new FilePartRequest().setSynapseClient(mockSynapseClient)
						.setUploadId(uploadId).setFile(temp).setHttpClient(mockHttpClient)
						.setPartLength(MultithreadMultipartUpload.MIN_PART_SIZE).setPartNumber(1L).setPartOffset(0L));
				// 2
				verify(mockPartCallableFactory).createCallable(
						new FilePartRequest().setSynapseClient(mockSynapseClient).setUploadId(uploadId).setFile(temp)
								.setHttpClient(mockHttpClient).setPartLength(MultithreadMultipartUpload.MIN_PART_SIZE)
								.setPartNumber(2L).setPartOffset(MultithreadMultipartUpload.MIN_PART_SIZE));
				// 3
				verify(mockPartCallableFactory).createCallable(
						new FilePartRequest().setSynapseClient(mockSynapseClient).setUploadId(uploadId).setFile(temp)
								.setHttpClient(mockHttpClient).setPartLength(MultithreadMultipartUpload.MIN_PART_SIZE)
								.setPartNumber(3L).setPartOffset(MultithreadMultipartUpload.MIN_PART_SIZE * 2));
				// 4
				verify(mockPartCallableFactory).createCallable(new FilePartRequest().setSynapseClient(mockSynapseClient)
						.setUploadId(uploadId).setFile(temp).setHttpClient(mockHttpClient).setPartLength(10L)
						.setPartNumber(4L).setPartOffset(MultithreadMultipartUpload.MIN_PART_SIZE * 3));

				verify(mockSynapseClient).completeMultipartUpload(uploadId);
				verify(mockSynapseClient).getRawFileHandle(fileHandleId);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

	}

	@Test
	public void testDoUploadWithPartsAlreadyUploaded() throws FileNotFoundException, SynapseException, IOException {
		int fileSizeBytes = (int) (MultithreadMultipartUpload.MIN_PART_SIZE * 3) + 10;
		RandomTempFileUtil.consumeRandomTempFile(fileSizeBytes, "foo", ".txt", (temp) -> {
			try {

				String fileMD5Hex = MD5ChecksumHelper.getMD5Checksum(temp);

				String uploadId = "111";
				String fileHandleId = "222";
				when(mockSynapseClient.startMultipartUpload(any(), anyBoolean()))
						.thenReturn(new MultipartUploadStatus().setUploadId(uploadId).setPartsState("0110"));
				when(mockThreadPool.submit(any(Callable.class))).thenReturn(mockFuture);
				when(mockPartCallableFactory.createCallable(any())).thenReturn(mockCallable);
				when(mockSynapseClient.completeMultipartUpload(any())).thenReturn(
						new MultipartUploadStatus().setUploadId(uploadId).setResultFileHandleId(fileHandleId));
				when(mockSynapseClient.getRawFileHandle(any()))
						.thenReturn(new GoogleCloudFileHandle().setId(fileHandleId));

				boolean forceRestart = false;
				// request can be null
				MultipartUploadRequest request = null;
				// call under test
				CloudProviderFileHandleInterface result = MultithreadMultipartUpload.doUpload(mockPartCallableFactory,
						mockHttpClient, mockThreadPool, mockSynapseClient, temp, request, forceRestart);

				assertEquals(new GoogleCloudFileHandle().setId(fileHandleId), result);

				verify(mockSynapseClient).startMultipartUpload(new MultipartUploadRequest().setContentMD5Hex(fileMD5Hex)
						.setFileName(temp.getName()).setFileSizeBytes((long) fileSizeBytes)
						.setPartSizeBytes(MultithreadMultipartUpload.MIN_PART_SIZE).setContentType("text/plain"),
						forceRestart);

				verify(mockPartCallableFactory, times(2)).createCallable(any());
				// 1
				verify(mockPartCallableFactory).createCallable(new FilePartRequest().setSynapseClient(mockSynapseClient)
						.setUploadId(uploadId).setFile(temp).setHttpClient(mockHttpClient)
						.setPartLength(MultithreadMultipartUpload.MIN_PART_SIZE).setPartNumber(1L).setPartOffset(0L));
				// parts 2 and 3 have already been uploaded.
				// 4
				verify(mockPartCallableFactory).createCallable(new FilePartRequest().setSynapseClient(mockSynapseClient)
						.setUploadId(uploadId).setFile(temp).setHttpClient(mockHttpClient).setPartLength(10L)
						.setPartNumber(4L).setPartOffset(MultithreadMultipartUpload.MIN_PART_SIZE * 3));

				verify(mockSynapseClient).completeMultipartUpload(uploadId);
				verify(mockSynapseClient).getRawFileHandle(fileHandleId);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	public void testDoUploadWithSmallFileAndForceRestart() throws FileNotFoundException, SynapseException, IOException {
		int fileSizeBytes = 10;
		RandomTempFileUtil.consumeRandomTempFile(fileSizeBytes, "foo", ".txt", (temp) -> {
			try {

				String fileMD5Hex = MD5ChecksumHelper.getMD5Checksum(temp);

				String uploadId = "111";
				String fileHandleId = "222";
				when(mockSynapseClient.startMultipartUpload(any(), anyBoolean()))
						.thenReturn(new MultipartUploadStatus().setUploadId(uploadId).setPartsState("0"));
				when(mockThreadPool.submit(any(Callable.class))).thenReturn(mockFuture);
				when(mockPartCallableFactory.createCallable(any())).thenReturn(mockCallable);
				when(mockSynapseClient.completeMultipartUpload(any())).thenReturn(
						new MultipartUploadStatus().setUploadId(uploadId).setResultFileHandleId(fileHandleId));
				when(mockSynapseClient.getRawFileHandle(any()))
						.thenReturn(new GoogleCloudFileHandle().setId(fileHandleId));

				boolean forceRestart = true;
				// call under test
				CloudProviderFileHandleInterface result = MultithreadMultipartUpload.doUpload(mockPartCallableFactory,
						mockHttpClient, mockThreadPool, mockSynapseClient, temp, new MultipartUploadRequest(),
						forceRestart);

				assertEquals(new GoogleCloudFileHandle().setId(fileHandleId), result);

				verify(mockSynapseClient).startMultipartUpload(new MultipartUploadRequest().setContentMD5Hex(fileMD5Hex)
						.setFileName(temp.getName()).setFileSizeBytes((long) fileSizeBytes)
						.setPartSizeBytes(MultithreadMultipartUpload.MIN_PART_SIZE).setContentType("text/plain"),
						forceRestart);

				verify(mockPartCallableFactory, times(1)).createCallable(any());
				// 1
				verify(mockPartCallableFactory).createCallable(
						new FilePartRequest().setSynapseClient(mockSynapseClient).setUploadId(uploadId).setFile(temp)
								.setHttpClient(mockHttpClient).setPartLength(10L).setPartNumber(1L).setPartOffset(0L));

				verify(mockSynapseClient).completeMultipartUpload(uploadId);
				verify(mockSynapseClient).getRawFileHandle(fileHandleId);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

	}

	@Test
	public void testDoUploadWithOverrideFileNameAndContentType()
			throws FileNotFoundException, SynapseException, IOException {
		int fileSizeBytes = 10;
		RandomTempFileUtil.consumeRandomTempFile(fileSizeBytes, "foo", ".txt", (temp) -> {
			try {

				String fileMD5Hex = MD5ChecksumHelper.getMD5Checksum(temp);

				String uploadId = "111";
				String fileHandleId = "222";
				when(mockSynapseClient.startMultipartUpload(any(), anyBoolean()))
						.thenReturn(new MultipartUploadStatus().setUploadId(uploadId).setPartsState("0"));
				when(mockThreadPool.submit(any(Callable.class))).thenReturn(mockFuture);
				when(mockPartCallableFactory.createCallable(any())).thenReturn(mockCallable);
				when(mockSynapseClient.completeMultipartUpload(any())).thenReturn(
						new MultipartUploadStatus().setUploadId(uploadId).setResultFileHandleId(fileHandleId));
				when(mockSynapseClient.getRawFileHandle(any()))
						.thenReturn(new GoogleCloudFileHandle().setId(fileHandleId));

				boolean forceRestart = true;
				String fileName = "override.json";
				String contentType = "appllication/json";
				MultipartUploadRequest request = new MultipartUploadRequest().setFileName(fileName)
						.setContentType(contentType);
				// call under test
				CloudProviderFileHandleInterface result = MultithreadMultipartUpload.doUpload(mockPartCallableFactory,
						mockHttpClient, mockThreadPool, mockSynapseClient, temp, request, forceRestart);

				assertEquals(new GoogleCloudFileHandle().setId(fileHandleId), result);

				verify(mockSynapseClient).startMultipartUpload(
						new MultipartUploadRequest().setContentMD5Hex(fileMD5Hex).setFileName(fileName)
								.setFileSizeBytes((long) fileSizeBytes)
								.setPartSizeBytes(MultithreadMultipartUpload.MIN_PART_SIZE).setContentType(contentType),
						forceRestart);

				verify(mockPartCallableFactory, times(1)).createCallable(any());
				// 1
				verify(mockPartCallableFactory).createCallable(
						new FilePartRequest().setSynapseClient(mockSynapseClient).setUploadId(uploadId).setFile(temp)
								.setHttpClient(mockHttpClient).setPartLength(10L).setPartNumber(1L).setPartOffset(0L));

				verify(mockSynapseClient).completeMultipartUpload(uploadId);
				verify(mockSynapseClient).getRawFileHandle(fileHandleId);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

	}

	@Test
	public void testDoUploadDoesNotExist() throws FileNotFoundException, SynapseException, IOException {
		File doesNotExist = new File("C:\\DoesNotExist.txt");
		boolean forceRestart = false;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			MultithreadMultipartUpload.doUpload(mockPartCallableFactory, mockHttpClient, mockThreadPool,
					mockSynapseClient, doesNotExist, new MultipartUploadRequest(), forceRestart);
		}).getMessage();
		assertEquals("The provided file does not exist: C:\\DoesNotExist.txt", message);
	}
	
	@Test
	public void testDoUploadWithNullFactory() throws FileNotFoundException, SynapseException, IOException {
		File file = new File("C:\\SomeFile.txt");
		mockPartCallableFactory = null;
		boolean forceRestart = false;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			MultithreadMultipartUpload.doUpload(mockPartCallableFactory, mockHttpClient, mockThreadPool,
					mockSynapseClient, file, new MultipartUploadRequest(), forceRestart);
		}).getMessage();
		assertEquals("callableFactory is required.", message);
	}
	
	@Test
	public void testDoUploadWithNullClient() throws FileNotFoundException, SynapseException, IOException {
		File file = new File("C:\\SomeFile.txt");
		mockHttpClient = null;
		boolean forceRestart = false;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			MultithreadMultipartUpload.doUpload(mockPartCallableFactory, mockHttpClient, mockThreadPool,
					mockSynapseClient, file, new MultipartUploadRequest(), forceRestart);
		}).getMessage();
		assertEquals("httpClient is required.", message);
	}
	
	@Test
	public void testDoUploadWithNullThreadPool() throws FileNotFoundException, SynapseException, IOException {
		File file = new File("C:\\SomeFile.txt");
		mockThreadPool = null;
		boolean forceRestart = false;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			MultithreadMultipartUpload.doUpload(mockPartCallableFactory, mockHttpClient, mockThreadPool,
					mockSynapseClient, file, new MultipartUploadRequest(), forceRestart);
		}).getMessage();
		assertEquals("threadPool is required.", message);
	}
	
	@Test
	public void testDoUploadWithNullSynapseClient() throws FileNotFoundException, SynapseException, IOException {
		File file = new File("C:\\SomeFile.txt");
		mockSynapseClient = null;
		boolean forceRestart = false;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			MultithreadMultipartUpload.doUpload(mockPartCallableFactory, mockHttpClient, mockThreadPool,
					mockSynapseClient, file, new MultipartUploadRequest(), forceRestart);
		}).getMessage();
		assertEquals("synapseClient is required.", message);
	}
	
	@Test
	public void testDoUploadWithNullFile() throws FileNotFoundException, SynapseException, IOException {
		File file = null;
		boolean forceRestart = false;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			MultithreadMultipartUpload.doUpload(mockPartCallableFactory, mockHttpClient, mockThreadPool,
					mockSynapseClient, file, new MultipartUploadRequest(), forceRestart);
		}).getMessage();
		assertEquals("file is required.", message);
	}

}
