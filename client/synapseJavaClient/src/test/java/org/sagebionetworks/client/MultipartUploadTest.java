package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.client.MultipartUpload.calculateMD5Hex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.MultipartUploadState;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PartPresignedUrl;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.FileProviderImpl;

public class MultipartUploadTest {

	@Mock
	SynapseClient mockClient;
	@Mock
	FileProvider mockFileProvider;

	List<PartData> partDataList;

	long fileSizeBytes;
	String fileName;
	String contentType;
	Long storageLocationId;
	Boolean generatePreview;
	Boolean forceRestart;
	byte[] fileBytes;
	ByteArrayInputStream input;
	S3FileHandle fileHandle;
	
	List<File> mockFiles;
	List<OutputStream> mockOutStreams;

	MultipartUploadStatus startStatus;

	MultipartUploadStatus completeStatus;

	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		// capture all mock files created.
		mockFiles = new LinkedList<File>();
		doAnswer(new Answer<File>(){
			@Override
			public File answer(InvocationOnMock invocation) throws Throwable {
				// create a mock file
				File mockFile = Mockito.mock(File.class);
				mockFiles.add(mockFile);
				return mockFile;
			}}).when(mockFileProvider).createTempFile(anyString(), anyString());
		
		mockOutStreams = new LinkedList<OutputStream>();
		doAnswer(new Answer<OutputStream>(){
			@Override
			public OutputStream answer(InvocationOnMock invocation) throws Throwable {
				// create a mock file
				OutputStream mockStream = Mockito.mock(FileOutputStream.class);
				mockOutStreams.add(mockStream);
				return mockStream;
			}}).when(mockFileProvider).createFileOutputStream(any(File.class));

		// The file's data is the following byte [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
		fileBytes = Hex.decodeHex("00010203040506070809".toCharArray());
		input = new ByteArrayInputStream(fileBytes);
		fileName = "foo.txt";
		contentType = "plain/text";
		storageLocationId = null;
		fileSizeBytes = fileBytes.length;

		partDataList = new LinkedList<PartData>();

		startStatus = new MultipartUploadStatus();
		startStatus.setPartsState("0");
		startStatus.setResultFileHandleId(null);
		startStatus.setUploadId("uploadId");

		when(
				mockClient.startMultipartUpload(
						any(), any()))
				.thenReturn(startStatus);

		completeStatus = new MultipartUploadStatus();
		completeStatus.setPartsState("1");
		completeStatus.setResultFileHandleId("1235");
		completeStatus.setState(MultipartUploadState.COMPLETED);
		completeStatus.setUploadId(startStatus.getUploadId());

		when(mockClient.completeMultipartUpload(anyString())).thenReturn(
				completeStatus);

		fileHandle = new S3FileHandle();
		fileHandle.setId(completeStatus.getResultFileHandleId());
		when(mockClient.getRawFileHandle(anyString())).thenReturn(fileHandle);

		BatchPresignedUploadUrlResponse batchResponse = new BatchPresignedUploadUrlResponse();
		batchResponse.setPartPresignedUrls(new LinkedList<PartPresignedUrl>());
		PartPresignedUrl partUrl = new PartPresignedUrl();
		partUrl.setPartNumber(new Long(1));
		partUrl.setUploadPresignedUrl("http://amazon.com/bucket/key/1");
		batchResponse.getPartPresignedUrls().add(partUrl);

		when(
				mockClient
						.getMultipartPresignedUrlBatch(any(BatchPresignedUploadUrlRequest.class)))
				.thenReturn(batchResponse);
	}

	@After
	public void after() {
		MultipartUpload.deleteTempFiles(partDataList);
	}

	@Test
	public void testCreatePartsHappy() throws Exception {
		long fileSizeBytes = fileBytes.length;
		long partSizeBytes = 4;
		long numberOfParts = 3;
		partDataList = new LinkedList<PartData>();
		String expectedFileMD5Hex = calculateMD5Hex(fileBytes, 0, 10);
		String[] expectedPartMD5Hex = new String[] {
				calculateMD5Hex(fileBytes, 0, 4),
				calculateMD5Hex(fileBytes, 4, 4),
				calculateMD5Hex(fileBytes, 8, 2), };
		// call under test
		String fileMD5Hex = MultipartUpload.createParts(new FileProviderImpl(), input, fileSizeBytes,
				partSizeBytes, numberOfParts, partDataList);
		assertEquals(expectedFileMD5Hex, fileMD5Hex);
		assertEquals(3, partDataList.size());
		// Check the bytes of each files
		// one
		int index = 0;
		PartData partData = partDataList.get(index);
		assertEquals(expectedPartMD5Hex[index], partData.getPartMD5Hex());
		assertEquals(4, partData.getPartFile().length());
		assertEquals(1, partData.getPartNumber());
		byte[] partBytes = FileUtils
				.readFileToByteArray(partData.getPartFile());
		assertEquals("00010203", new String(Hex.encodeHex(partBytes)));
		// two
		index = 1;
		partData = partDataList.get(index);
		assertEquals(expectedPartMD5Hex[index], partData.getPartMD5Hex());
		assertEquals(4, partData.getPartFile().length());
		assertEquals(2, partData.getPartNumber());
		partBytes = FileUtils.readFileToByteArray(partData.getPartFile());
		assertEquals("04050607", new String(Hex.encodeHex(partBytes)));

		// three
		index = 2;
		partData = partDataList.get(index);
		assertEquals(expectedPartMD5Hex[index], partData.getPartMD5Hex());
		assertEquals(2, partData.getPartFile().length());
		assertEquals(3, partData.getPartNumber());
		partBytes = FileUtils.readFileToByteArray(partData.getPartFile());
		assertEquals("0809", new String(Hex.encodeHex(partBytes)));
	}

	@Test
	public void testCreatePartsPartSizeLargerThanFile() throws Exception {
		long fileSizeBytes = fileBytes.length;
		long partSizeBytes = 5L*1024L*1024L;
		long numberOfParts = 1;
		partDataList = new LinkedList<PartData>();
		String expectedFileMD5Hex = calculateMD5Hex(fileBytes, 0, 10);
		String[] expectedPartMD5Hex = new String[] {
				calculateMD5Hex(fileBytes, 0, 10),
		};
		// call under test
		String fileMD5Hex = MultipartUpload.createParts(new FileProviderImpl(),input, fileSizeBytes,
				partSizeBytes, numberOfParts, partDataList);
		assertEquals(expectedFileMD5Hex, fileMD5Hex);
		assertEquals(1, partDataList.size());
		// the single part
		int index = 0;
		PartData partData = partDataList.get(index);
		assertEquals(expectedPartMD5Hex[index], partData.getPartMD5Hex());
		assertEquals(fileSizeBytes, partData.getPartFile().length());
		assertEquals(1, partData.getPartNumber());
		byte[] partBytes = FileUtils
				.readFileToByteArray(partData.getPartFile());
		assertEquals("00010203040506070809",
				new String(Hex.encodeHex(partBytes)));
		
	}
	
	@Test
	public void testCreatePartsPartSizeEvenPartSize() throws Exception {
		long fileSizeBytes = fileBytes.length;
		long partSizeBytes = 5L;
		long numberOfParts = 2;
		partDataList = new LinkedList<PartData>();
		String expectedFileMD5Hex = calculateMD5Hex(fileBytes, 0, 10);
		String[] expectedPartMD5Hex = new String[] { 
				calculateMD5Hex(fileBytes, 0, 5),
				calculateMD5Hex(fileBytes, 5, 5),
		};
		// call under test
		String fileMD5Hex = MultipartUpload.createParts(new FileProviderImpl(),input, fileSizeBytes,
				partSizeBytes, numberOfParts, partDataList);
		assertEquals(expectedFileMD5Hex, fileMD5Hex);
		assertEquals(2, partDataList.size());
		// one
		int index = 0;
		PartData partData = partDataList.get(index);
		assertEquals(expectedPartMD5Hex[index], partData.getPartMD5Hex());
		assertEquals(5, partData.getPartFile().length());
		assertEquals(1, partData.getPartNumber());
		byte[] partBytes = FileUtils
				.readFileToByteArray(partData.getPartFile());
		assertEquals("0001020304",
				new String(Hex.encodeHex(partBytes)));
		
		// two
		index = 1;
		partData = partDataList.get(index);
		assertEquals(expectedPartMD5Hex[index], partData.getPartMD5Hex());
		assertEquals(5, partData.getPartFile().length());
		assertEquals(2, partData.getPartNumber());
		partBytes = FileUtils.readFileToByteArray(partData.getPartFile());
		assertEquals("0506070809", new String(Hex.encodeHex(partBytes)));
	}

	@Test
	public void testUploadMissingPartsOnePart() throws SynapseException {
		MultipartUploadStatus status = new MultipartUploadStatus();
		status.setPartsState("0");
		status.setUploadId("uploadId");

		List<PartData> partDataList = createTestPartList(1);
		// call under test.
		MultipartUpload.uploadMissingParts(mockClient, status, partDataList, contentType);

		ArgumentCaptor<BatchPresignedUploadUrlRequest> captureBatch = ArgumentCaptor
				.forClass(BatchPresignedUploadUrlRequest.class);
		verify(mockClient, times(1)).getMultipartPresignedUrlBatch(
				captureBatch.capture());
		assertEquals(status.getUploadId(), captureBatch.getValue()
				.getUploadId());
		assertNotNull(captureBatch.getValue().getPartNumbers());
		assertEquals(1, captureBatch.getValue().getPartNumbers().size());
		assertEquals(new Long(1),
				captureBatch.getValue().getPartNumbers().get(0));

		// should be put to the file.
		verify(mockClient, times(1)).putFileToURL(any(URL.class),
				any(File.class), any(String.class));

		verify(mockClient, times(1)).addPartToMultipartUpload(
				status.getUploadId(), 1, partDataList.get(0).getPartMD5Hex());
	}

	@Test
	public void testUploadMissingPartsMultipleParts() throws SynapseException {
		MultipartUploadStatus status = new MultipartUploadStatus();
		status.setPartsState("0101");
		status.setUploadId("uploadId");

		List<PartData> partDataList = createTestPartList(4);
		// call under test.
		MultipartUpload.uploadMissingParts(mockClient, status, partDataList, contentType);

		verify(mockClient, times(2)).getMultipartPresignedUrlBatch(
				any(BatchPresignedUploadUrlRequest.class));
		verify(mockClient, times(2)).putFileToURL(any(URL.class),
				any(File.class), any(String.class));
		verify(mockClient, times(2)).addPartToMultipartUpload(anyString(),
				anyInt(), anyString());
	}

	@Test
	public void testUploadFileHappy() throws Exception {
		MultipartUpload upload = new MultipartUpload(mockClient, input,
				fileSizeBytes, fileName, contentType, storageLocationId,
				generatePreview, forceRestart, mockFileProvider);
		CloudProviderFileHandleInterface result = upload.uploadFile();
		assertEquals(fileHandle, result);

		verify(mockClient, times(1)).startMultipartUpload(
				any(), any());
		verify(mockClient, times(1)).getMultipartPresignedUrlBatch(
				any(BatchPresignedUploadUrlRequest.class));
		verify(mockClient, times(1)).putFileToURL(any(URL.class),
				any(File.class), any(String.class));
		verify(mockClient, times(1)).addPartToMultipartUpload(anyString(),
				anyInt(), anyString());
		verify(mockClient, times(1)).completeMultipartUpload(startStatus.getUploadId());
		
		verifyMockFilesDeleted(1);
		verifyMockStreamsClosed(1);
	}
	@Test
	public void testUploadFileComplete() throws Exception {
		// Setup a status of complete from the start.
		startStatus.setPartsState("1");
		startStatus.setResultFileHandleId(fileHandle.getId());
		startStatus.setState(MultipartUploadState.COMPLETED);
		
		MultipartUpload upload = new MultipartUpload(mockClient, input,
				fileSizeBytes, fileName, contentType, storageLocationId,
				generatePreview, forceRestart, mockFileProvider);
		// since it is complete it should just return the file handle.
		CloudProviderFileHandleInterface result = upload.uploadFile();
		assertEquals(fileHandle, result);

		verify(mockClient, times(1)).startMultipartUpload(
				any(), any());
		verify(mockClient, never()).getMultipartPresignedUrlBatch(
				any(BatchPresignedUploadUrlRequest.class));
		verify(mockClient, never()).putFileToURL(any(URL.class),
				any(File.class), any(String.class));
		verify(mockClient, never()).addPartToMultipartUpload(anyString(),
				anyInt(), anyString());
		verify(mockClient, never()).completeMultipartUpload(startStatus.getUploadId());
	}
	
	/**
	 * Verify the expected number of mock files were created and deleted.
	 * @param expectedCount
	 */
	public void verifyMockFilesDeleted(int expectedCount){
		assertEquals(expectedCount, mockFiles.size());
		for(File mockFile: mockFiles){
			verify(mockFile).delete();
		}
	}
	
	/**
	 * Verify the expected number of mock OutputStreams were created and closed.
	 * @param expectedCount
	 * @throws IOException
	 */
	public void verifyMockStreamsClosed(int expectedCount) throws IOException{
		assertEquals(expectedCount, mockOutStreams.size());
		for(OutputStream mockOut: mockOutStreams){
			verify(mockOut).close();
		}
	}

	/**
	 * Test helper
	 * 
	 * @param count
	 * @return
	 */
	private List<PartData> createTestPartList(int count) {
		List<PartData> list = new LinkedList<PartData>();
		for (int i = 0; i < count; i++) {
			PartData pd = new PartData();
			pd.setPartFile(Mockito.mock(File.class));
			pd.setPartMD5Hex("partMD5Hex" + i);
			pd.setPartNumber(i + 1);
			list.add(pd);
		}
		return list;
	}

}
