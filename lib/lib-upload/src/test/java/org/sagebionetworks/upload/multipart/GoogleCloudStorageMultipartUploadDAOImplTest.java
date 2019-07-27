package org.sagebionetworks.upload.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.model.dbo.file.DBOMultipartUploadComposerPartState;
import org.sagebionetworks.repo.model.dbo.file.MultipartUploadComposerDAO;
import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.HttpMethod;

@ExtendWith(MockitoExtension.class)
public class GoogleCloudStorageMultipartUploadDAOImplTest {


	@Mock
	private SynapseGoogleCloudStorageClient mockStorageClient;

	@Mock
	private MultipartUploadComposerDAO mockMultipartUploadComposerDAO;

	@InjectMocks
	private GoogleCloudStorageMultipartUploadDAOImpl googleMpuDAO;

	@Mock
	Blob mockBlob;

	@Mock
	Blob mockBlob2;

	private static final String UPLOAD_ID = "233";
	private static final String KEY_NAME = "testKeyName";
	private static final int PART_NUMBER = 4;
	private static final String PART_KEY_NAME = KEY_NAME + "/" + PART_NUMBER;
	private static final String BUCKET_NAME = "test-bucket-name";
	private static final String PART_MD5 = "abcdef0123456789abcdef0123456789";

	@Test
	public void initiateMultipartUpload() {
		// Will always be an empty string.
		assertTrue(googleMpuDAO
				.initiateMultipartUpload(BUCKET_NAME, KEY_NAME, new MultipartUploadRequest())
				.isEmpty());
	}

	@Test
	public void presignedPutURL() throws MalformedURLException {
		when(mockStorageClient.createSignedUrl(BUCKET_NAME, KEY_NAME, 15 * 1000 * 60, HttpMethod.PUT)).thenReturn(new URL("http://google.com/"));

		// Call under test
		assertNotNull(googleMpuDAO.createPreSignedPutUrl(BUCKET_NAME, KEY_NAME, null)); // contentType is not used
		verify(mockStorageClient).createSignedUrl(BUCKET_NAME, KEY_NAME, 15 * 1000 * 60, HttpMethod.PUT);
	}

	@Test
	public void addPart() throws Exception {
		long existingDboLowerBound = 1L;
		long existingDboUpperBound = 20L;
		long existingDboPartSize = 1L;

		AddPartRequest addPartRequest = new AddPartRequest(UPLOAD_ID,
				"", BUCKET_NAME, KEY_NAME, PART_KEY_NAME, PART_MD5, PART_NUMBER, 100);
		when(mockStorageClient.getObject(BUCKET_NAME, KEY_NAME + "/" + PART_NUMBER)).thenReturn(mockBlob);
		String md5AsBase64 = new String(Base64.encodeBase64(Hex.decodeHex(PART_MD5.toCharArray())), StandardCharsets.UTF_8);
		when(mockBlob.getMd5()).thenReturn(md5AsBase64);

		doNothing().when(mockMultipartUploadComposerDAO).addPartToUpload(UPLOAD_ID, (long) PART_NUMBER, (long) PART_NUMBER);
		when(mockMultipartUploadComposerDAO.getAddedPartRanges(anyLong(), anyLong(), anyLong()))
				.thenReturn(createDboList(Long.valueOf(UPLOAD_ID), existingDboLowerBound, existingDboUpperBound, existingDboPartSize));

		// Call under test
		googleMpuDAO.validateAndAddPart(addPartRequest);

		verify(mockStorageClient).getObject(BUCKET_NAME, KEY_NAME + "/" + PART_NUMBER);
		verify(mockMultipartUploadComposerDAO).addPartToUpload(UPLOAD_ID, (long) PART_NUMBER, (long) PART_NUMBER);
		verify(mockMultipartUploadComposerDAO).getAddedPartRanges(anyLong(), anyLong(), anyLong());
		// The merge should not execute, so no more interactions with the composer DAO
		verifyNoMoreInteractions(mockMultipartUploadComposerDAO);
	}

	@Test
	public void validatePart() throws Exception {
		AddPartRequest addPartRequest = new AddPartRequest(UPLOAD_ID,
				"", BUCKET_NAME, KEY_NAME, PART_KEY_NAME, PART_MD5, PART_NUMBER, 100);
		when(mockStorageClient.getObject(BUCKET_NAME, KEY_NAME + "/" + PART_NUMBER)).thenReturn(mockBlob);
		String md5AsBase64 = new String(Base64.encodeBase64(Hex.decodeHex(PART_MD5.toCharArray())), StandardCharsets.UTF_8);
		when(mockBlob.getMd5()).thenReturn(md5AsBase64);

		// Call under test
		googleMpuDAO.validatePartMd5(addPartRequest);

		verify(mockStorageClient).getObject(BUCKET_NAME, KEY_NAME + "/" + PART_NUMBER);
	}

	@Test
	public void validatePartNullBlob() {
		AddPartRequest addPartRequest = new AddPartRequest(UPLOAD_ID,
				"", BUCKET_NAME, KEY_NAME, PART_KEY_NAME, PART_MD5, PART_NUMBER, 100);
		when(mockStorageClient.getObject(BUCKET_NAME, KEY_NAME + "/" + PART_NUMBER)).thenReturn(null);

		// Call under test
		assertThrows(IllegalArgumentException.class, () -> googleMpuDAO.validatePartMd5(addPartRequest));
	}

	@Test
	public void validatePartMismatchedMd5() {
		AddPartRequest addPartRequest = new AddPartRequest(UPLOAD_ID,
				"", BUCKET_NAME, KEY_NAME, PART_KEY_NAME, PART_MD5, PART_NUMBER, 100);
		when(mockStorageClient.getObject(BUCKET_NAME, KEY_NAME + "/" + PART_NUMBER)).thenReturn(mockBlob);
		when(mockBlob.getMd5()).thenReturn("not a matching md5");

		// Call under test
		assertThrows(IllegalArgumentException.class, () -> googleMpuDAO.validatePartMd5(addPartRequest));
	}

	@Test
	public void addPartNoMd5Check() {
		long existingDboLowerBound = 1L;
		long existingDboUpperBound = 20L;
		long existingDboPartSize = 1L;

		doNothing().when(mockMultipartUploadComposerDAO).addPartToUpload(UPLOAD_ID, (long) PART_NUMBER, (long) PART_NUMBER);
		when(mockMultipartUploadComposerDAO.getAddedPartRanges(anyLong(), anyLong(), anyLong()))
				.thenReturn(createDboList(Long.valueOf(UPLOAD_ID), existingDboLowerBound, existingDboUpperBound, existingDboPartSize));

		// Call under test
		googleMpuDAO.addPart(UPLOAD_ID, BUCKET_NAME, KEY_NAME, (long) PART_NUMBER, (long) PART_NUMBER, 100L);

		verify(mockMultipartUploadComposerDAO).addPartToUpload(UPLOAD_ID, (long) PART_NUMBER, (long) PART_NUMBER);
		verify(mockMultipartUploadComposerDAO).getAddedPartRanges(anyLong(), anyLong(), anyLong());
		// The merge should not execute, so no more interactions with the composer DAO
		verifyNoMoreInteractions(mockMultipartUploadComposerDAO);
	}

	@Test
	public void attemptMergeNoStitchTargets() {
		// parts 21-32 would be missing in this case, so the merge shouldn't happen
		long lowerBoundOfPart = 5L;
		long upperBoundOfPart = 5L;
		long numberOfPartsInEntireUpload = 100L;
		long existingDboLowerBound = 1L;
		long existingDboUpperBound = 20L;
		long existingDboPartSize = 1L;
		when(mockMultipartUploadComposerDAO.getAddedPartRanges(anyLong(), anyLong(), anyLong()))
				.thenReturn(createDboList(Long.valueOf(UPLOAD_ID), existingDboLowerBound, existingDboUpperBound, existingDboPartSize));

		// Call under test
		googleMpuDAO.addPart(UPLOAD_ID, BUCKET_NAME, KEY_NAME, lowerBoundOfPart, upperBoundOfPart, numberOfPartsInEntireUpload);

		verifyZeroInteractions(mockStorageClient);
		verify(mockMultipartUploadComposerDAO).addPartToUpload(UPLOAD_ID, lowerBoundOfPart, upperBoundOfPart);
		verify(mockMultipartUploadComposerDAO).getAddedPartRanges(anyLong(), anyLong(), anyLong());
		verifyNoMoreInteractions(mockMultipartUploadComposerDAO);
	}

	@Test
	public void attemptMergeWithStitchTargetsSingleton() {
		long lowerBoundOfPart = 5L;
		long upperBoundOfPart = 5L;
		long numberOfPartsInEntireUpload = 100L;
		long existingDboLowerBound = 1L;
		long existingDboUpperBound = 32L;
		long existingDboPartSize = 1L;
		when(mockMultipartUploadComposerDAO.getAddedPartRanges(anyLong(), anyLong(), anyLong()))
				.thenReturn(createDboList(Long.valueOf(UPLOAD_ID), existingDboLowerBound,
						existingDboUpperBound, existingDboPartSize));

		// Call under test
		googleMpuDAO.addPart(UPLOAD_ID, BUCKET_NAME, KEY_NAME, lowerBoundOfPart, upperBoundOfPart, numberOfPartsInEntireUpload);

		verify(mockStorageClient).composeObjects(eq(BUCKET_NAME), anyString(), anyList());
		verify(mockMultipartUploadComposerDAO).deletePartsInRange(UPLOAD_ID, existingDboLowerBound, existingDboUpperBound);
		verify(mockMultipartUploadComposerDAO).addPartToUpload(UPLOAD_ID, existingDboLowerBound, existingDboUpperBound);
	}

	@Test
	public void attemptMergeWithStitchTargetsRange() {
		long lowerBoundOfPart = 65L;
		long upperBoundOfPart = 96L;
		long numberOfPartsInEntireUpload = 1024L;
		long existingDboLowerBound = 1L;
		long existingDboUpperBound = 1024L;
		long existingDboPartSize = 32L;
		when(mockMultipartUploadComposerDAO.getAddedPartRanges(anyLong(), anyLong(), anyLong()))
				.thenReturn(createDboList(Long.valueOf(UPLOAD_ID), existingDboLowerBound, existingDboUpperBound, existingDboPartSize));

		// Call under test
		googleMpuDAO.addPart(UPLOAD_ID, BUCKET_NAME, KEY_NAME, lowerBoundOfPart, upperBoundOfPart, numberOfPartsInEntireUpload);

		verify(mockStorageClient).composeObjects(eq(BUCKET_NAME), anyString(), anyList());
		verify(mockMultipartUploadComposerDAO).deletePartsInRange(UPLOAD_ID, existingDboLowerBound, existingDboUpperBound);
		verify(mockMultipartUploadComposerDAO).addPartToUpload(UPLOAD_ID, existingDboLowerBound, existingDboUpperBound);
	}

	@Test
	public void attemptMergeWithStitchTargetsHitUpperBound() {
		long lowerBoundOfPart = 1L;
		long upperBoundOfPart = 32L;
		long numberOfPartsInEntireUpload = 100L;
		long existingDboLowerBound = 1L;
		long existingDboUpperBound = 100L;
		long existingDboPartSize = 32L;
		when(mockMultipartUploadComposerDAO.getAddedPartRanges(anyLong(), anyLong(), anyLong()))
				.thenReturn(createDboList(Long.valueOf(UPLOAD_ID), existingDboLowerBound, existingDboUpperBound, existingDboPartSize));

		// Call under test
		googleMpuDAO.addPart(UPLOAD_ID, BUCKET_NAME, KEY_NAME, lowerBoundOfPart, upperBoundOfPart, numberOfPartsInEntireUpload);

		verify(mockStorageClient).composeObjects(eq(BUCKET_NAME), anyString(), anyList());
		verify(mockMultipartUploadComposerDAO).deletePartsInRange(UPLOAD_ID, existingDboLowerBound, existingDboUpperBound);
		verify(mockMultipartUploadComposerDAO).addPartToUpload(UPLOAD_ID, existingDboLowerBound, existingDboUpperBound);
	}

	@Test
	public void attemptMergeWithStitchTargetsSingletonLastPartEdgeCase() {
		long lowerBoundOfPart = 33L;
		long upperBoundOfPart = 33L;
		long numberOfPartsInEntireUpload = 33L;
		long existingDboLowerBound = 1L;
		long existingDboUpperBound = 33L;
		long existingDboPartSize = 32L;
		when(mockMultipartUploadComposerDAO.getAddedPartRanges(anyLong(), anyLong(), anyLong()))
				.thenReturn(createDboList(Long.valueOf(UPLOAD_ID), existingDboLowerBound, existingDboUpperBound, existingDboPartSize));

		// Call under test
		googleMpuDAO.addPart(UPLOAD_ID, BUCKET_NAME, KEY_NAME, lowerBoundOfPart, upperBoundOfPart, numberOfPartsInEntireUpload);

		verify(mockStorageClient).composeObjects(eq(BUCKET_NAME), anyString(), anyList());
		verify(mockMultipartUploadComposerDAO).deletePartsInRange(UPLOAD_ID, existingDboLowerBound, existingDboUpperBound);
		verify(mockMultipartUploadComposerDAO).addPartToUpload(UPLOAD_ID, existingDboLowerBound, existingDboUpperBound);
	}

	@Test
	public void attemptMergeWithStitchTargetsRangeLastPartEdgeCase() {
		long lowerBoundOfPart = 1025L;
		long upperBoundOfPart = 1026L;
		long numberOfPartsInEntireUpload = 1026L;
		long existingDboLowerBound = 1L;
		long existingDboUpperBound = 1026L;
		long existingDboPartSize = 1024L;
		when(mockMultipartUploadComposerDAO.getAddedPartRanges(anyLong(), anyLong(), anyLong()))
				.thenReturn(createDboList(Long.valueOf(UPLOAD_ID), existingDboLowerBound, existingDboUpperBound, existingDboPartSize));

		// Call under test
		googleMpuDAO.addPart(UPLOAD_ID, BUCKET_NAME, KEY_NAME, lowerBoundOfPart, upperBoundOfPart, numberOfPartsInEntireUpload);

		verify(mockStorageClient).composeObjects(eq(BUCKET_NAME), anyString(), anyList());
		verify(mockMultipartUploadComposerDAO).deletePartsInRange(UPLOAD_ID, existingDboLowerBound, existingDboUpperBound);
		verify(mockMultipartUploadComposerDAO).addPartToUpload(UPLOAD_ID, existingDboLowerBound, existingDboUpperBound);
	}


	private List<DBOMultipartUploadComposerPartState> createDboList(long uploadId,
																	long lowerBound,
																	long upperBound,
																	long partSize) {
		List<DBOMultipartUploadComposerPartState> list = new LinkedList<>();
		for (long i = lowerBound; i <= upperBound; i += partSize) {
			DBOMultipartUploadComposerPartState dbo = new DBOMultipartUploadComposerPartState();
			dbo.setUploadId(uploadId);
			dbo.setPartRangeLowerBound(i);
			dbo.setPartRangeUpperBound(Math.min(i + partSize - 1, upperBound));
			list.add(dbo);
		}
		return list;
	}

	@Test
	public void testCompleteMultipartUploadNotReady() {
		long numberOfParts = 100L;

		CompleteMultipartRequest completeMultipartRequest = new CompleteMultipartRequest();
		completeMultipartRequest.setUploadId(Long.valueOf(UPLOAD_ID));
		completeMultipartRequest.setNumberOfParts(numberOfParts);
		completeMultipartRequest.setBucket(BUCKET_NAME);
		completeMultipartRequest.setKey(KEY_NAME);

		DBOMultipartUploadComposerPartState entireFilePart = new DBOMultipartUploadComposerPartState();
		entireFilePart.setUploadId(Long.valueOf(UPLOAD_ID));
		entireFilePart.setPartRangeLowerBound(1L);
		entireFilePart.setPartRangeUpperBound(numberOfParts - 1); // The last part is not stitched.

		when(mockMultipartUploadComposerDAO.getAddedParts(Long.valueOf(UPLOAD_ID)))
				.thenReturn(Collections.singletonList(entireFilePart));

		// Call under test
		assertThrows(IllegalArgumentException.class,  () ->
				googleMpuDAO.completeMultipartUpload(completeMultipartRequest));

		verify(mockMultipartUploadComposerDAO).getAddedParts(Long.valueOf(UPLOAD_ID));
		verifyNoMoreInteractions(mockMultipartUploadComposerDAO);
		verifyZeroInteractions(mockStorageClient);
	}


	@Test
	public void testCompleteMultipartUpload() {
		long numberOfParts = 100L;

		CompleteMultipartRequest completeMultipartRequest = new CompleteMultipartRequest();
		completeMultipartRequest.setUploadId(Long.valueOf(UPLOAD_ID));
		completeMultipartRequest.setNumberOfParts(numberOfParts);
		completeMultipartRequest.setBucket(BUCKET_NAME);
		completeMultipartRequest.setKey(KEY_NAME);

		DBOMultipartUploadComposerPartState entireFilePart = new DBOMultipartUploadComposerPartState();
		entireFilePart.setUploadId(Long.valueOf(UPLOAD_ID));
		entireFilePart.setPartRangeLowerBound(1L);
		entireFilePart.setPartRangeUpperBound(numberOfParts);

		when(mockMultipartUploadComposerDAO.getAddedParts(Long.valueOf(UPLOAD_ID)))
				.thenReturn(Collections.singletonList(entireFilePart));
		doNothing().when(mockMultipartUploadComposerDAO).deletePartsInRange(UPLOAD_ID, -1, Long.MAX_VALUE);
		doNothing().when(mockStorageClient).rename(eq(BUCKET_NAME), any(String.class), eq(KEY_NAME));
		when(mockStorageClient.getObjects(BUCKET_NAME, KEY_NAME)).thenReturn(Arrays.asList(mockBlob, mockBlob2));
		when(mockBlob.getName()).thenReturn(KEY_NAME);
		when(mockBlob2.getName()).thenReturn(KEY_NAME + "/1-5");
		when(mockStorageClient.getObject(BUCKET_NAME, KEY_NAME)).thenReturn(mockBlob);
		when(mockBlob.getSize()).thenReturn(1234L);

		// Call under test
		assertEquals(1234L, googleMpuDAO.completeMultipartUpload(completeMultipartRequest));

		verify(mockMultipartUploadComposerDAO).getAddedParts(Long.valueOf(UPLOAD_ID));
		verify(mockMultipartUploadComposerDAO).deletePartsInRange(UPLOAD_ID, -1, Long.MAX_VALUE);
		verify(mockStorageClient).rename(eq(BUCKET_NAME), any(String.class), eq(KEY_NAME));
		verify(mockStorageClient).getObject(BUCKET_NAME, KEY_NAME);
		verify(mockBlob2).delete();
		verify(mockBlob, never()).delete();
		verify(mockBlob).getSize();
	}
}
