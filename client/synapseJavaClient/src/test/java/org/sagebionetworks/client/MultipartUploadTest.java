package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.client.MultipartUpload.calculateMD5Hex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadState;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PartPresignedUrl;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public class MultipartUploadTest {

	@Mock
	SynapseClient mockClient;

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

	MultipartUploadStatus startStatus;

	MultipartUploadStatus completeStatus;

	@Before
	public void before() throws SynapseException, DecoderException {
		MockitoAnnotations.initMocks(this);

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
						any(MultipartUploadRequest.class), any(Boolean.class)))
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
		String fileMD5Hex = MultipartUpload.createParts(input, fileSizeBytes,
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
		long partSizeBytes = fileSizeBytes + 1;
		long numberOfParts = 1;
		partDataList = new LinkedList<PartData>();
		String expectedFileMD5Hex = calculateMD5Hex(fileBytes, 0, 10);
		String[] expectedPartMD5Hex = new String[] { calculateMD5Hex(fileBytes,
				0, 10), };
		// call under test
		String fileMD5Hex = MultipartUpload.createParts(input, fileSizeBytes,
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
	public void testUploadMissingPartsOnePart() throws SynapseException {
		MultipartUploadStatus status = new MultipartUploadStatus();
		status.setPartsState("0");
		status.setUploadId("uploadId");

		List<PartData> partDataList = createTestPartList(1);
		// call under test.
		MultipartUpload.uploadMissingParts(mockClient, status, partDataList);

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
		MultipartUpload.uploadMissingParts(mockClient, status, partDataList);

		verify(mockClient, times(2)).getMultipartPresignedUrlBatch(
				any(BatchPresignedUploadUrlRequest.class));
		verify(mockClient, times(2)).putFileToURL(any(URL.class),
				any(File.class), any(String.class));
		verify(mockClient, times(2)).addPartToMultipartUpload(anyString(),
				anyInt(), anyString());
	}

	@Test
	public void testUploadFileHappy() throws SynapseException {
		MultipartUpload upload = new MultipartUpload(mockClient, input,
				fileSizeBytes, fileName, contentType, storageLocationId,
				generatePreview, forceRestart);
		S3FileHandle result = upload.uploadFile();
		assertEquals(fileHandle, result);

		verify(mockClient, times(1)).startMultipartUpload(
				any(MultipartUploadRequest.class), any(Boolean.class));
		verify(mockClient, times(1)).getMultipartPresignedUrlBatch(
				any(BatchPresignedUploadUrlRequest.class));
		verify(mockClient, times(1)).putFileToURL(any(URL.class),
				any(File.class), any(String.class));
		verify(mockClient, times(1)).addPartToMultipartUpload(anyString(),
				anyInt(), anyString());
		verify(mockClient, times(1)).completeMultipartUpload(startStatus.getUploadId());
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
