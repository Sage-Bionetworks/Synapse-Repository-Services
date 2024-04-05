package org.sagebionetworks.upload.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.model.dbo.file.part.AsyncMultipartUploadComposerDAO;
import org.sagebionetworks.repo.model.dbo.file.part.Compose;
import org.sagebionetworks.repo.model.dbo.file.part.OrderBy;
import org.sagebionetworks.repo.model.dbo.file.part.PartRange;
import org.sagebionetworks.repo.model.file.AbortMultipartRequest;
import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.HttpMethod;

@ExtendWith(MockitoExtension.class)
public class AsyncGoogleMultipartUploadDAOTest {

	@Mock
	private SynapseGoogleCloudStorageClient mockGoogleCloudStorageClient;
	@Mock
	private AsyncMultipartUploadComposerDAO mockAsyncComposerDAO;
	@Mock
	private Blob mockBlob;

	@Spy
	@InjectMocks
	private AsyncGoogleMultipartUploadDAO dao;

	private String bucket;
	private String key;
	private String contentType;
	private String uploadId;
	private PartRange left;
	private PartRange right;
	private Compose toCompose;
	private PartRange rangeOneToTwo;
	private PartRange rangeThreeToFour;
	private PartRange rangeFiveToSix;
	private PartRange rangeSevenToEight;

	private String md5 = "md5";
	private String md5Base64;
	private String md5Hex;
	private AddPartRequest addPartRequest;
	private CompleteMultipartRequest completeRequest;
	private AbortMultipartRequest abortRequest;

	@BeforeEach
	public void before() throws UnsupportedEncodingException {
		bucket = "bucket";
		key = "key";
		contentType = "contentType";
		uploadId = "123";
		left = new PartRange().setLowerBound(1L).setUpperBound(4L);
		right = new PartRange().setLowerBound(5L).setUpperBound(12L);
		toCompose = new Compose().setLeft(left).setRight(right);

		rangeOneToTwo = new PartRange().setLowerBound(1L).setUpperBound(2L);
		rangeThreeToFour = new PartRange().setLowerBound(3L).setUpperBound(4L);
		rangeFiveToSix = new PartRange().setLowerBound(5L).setUpperBound(6L);
		rangeSevenToEight = new PartRange().setLowerBound(7L).setUpperBound(8L);

		md5 = "some.md5";
		md5Base64 = new String(Base64.encodeBase64(md5.getBytes("UTF-8")));
		md5Hex = Hex.encodeHexString(md5.getBytes("UTF-8"));

		addPartRequest = new AddPartRequest().setBucket(bucket).setKey(key).setPartKey(key + "/8-8")
				.setPartMD5Hex(md5Hex).setPartNumber(8L).setTotalNumberOfParts(111L).setUploadId(uploadId)
				.setUploadToken("token");
		completeRequest = new CompleteMultipartRequest().setBucket(bucket).setKey(key).setNumberOfParts(111L)
				.setUploadId(Long.parseLong(uploadId)).setUploadToken("token");

		abortRequest = new AbortMultipartRequest().setBucket(bucket).setKey(key).setUploadId(uploadId);
	}

	@Test
	public void testInitiateMultipartUpload() {
		MultipartUploadRequest request = new MultipartUploadRequest();
		// call under test
		assertEquals("", dao.initiateMultipartUpload(bucket, key, request));
	}

	@Test
	public void testInitiateMultipartUploadCopy() {

		assertThrows(UnsupportedOperationException.class, () -> {
			// call under test
			dao.initiateMultipartUploadCopy(null, null, null, null);
		});
	}

	@Test
	public void testCreatePartUploadCopyPresignedUrl() {
		assertThrows(UnsupportedOperationException.class, () -> {
			// call under test
			dao.createPartUploadCopyPresignedUrl(null, 0, null);
		});
	}

	@Test
	public void testValidatePartCopy() {
		assertThrows(UnsupportedOperationException.class, () -> {
			// call under test
			dao.validatePartCopy(null, 0, null);
		});
	}

	@Test
	public void testGetObjectEtag() {
		assertThrows(UnsupportedOperationException.class, () -> {
			// call under test
			dao.getObjectEtag(null, null);
		});
	}

	@Test
	public void testDoesObjectExist() {
		when(mockGoogleCloudStorageClient.doesObjectExist(any(), any())).thenReturn(true);
		// call under test
		assertTrue(dao.doesObjectExist(bucket, key));

		verify(mockGoogleCloudStorageClient).doesObjectExist(bucket, key);
	}

	@Test
	public void testDoesObjectExistWithNullBucket() {
		bucket = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			assertTrue(dao.doesObjectExist(bucket, key));
		}).getMessage();
		assertEquals("bucketName is required.", message);
		verifyZeroInteractions(mockGoogleCloudStorageClient);
	}

	@Test
	public void testDoesObjectExistWithNullKey() {
		key = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			assertTrue(dao.doesObjectExist(bucket, key));
		}).getMessage();
		assertEquals("objectKey is required.", message);
		verifyZeroInteractions(mockGoogleCloudStorageClient);
	}

	@Test
	public void testCreatePartUploadPreSignedUrl() throws MalformedURLException {
		URL resultUrl = new URL("https://google.com/something");
		when(mockGoogleCloudStorageClient.createSignedUrl(any(), any(), anyLong(), any())).thenReturn(resultUrl);
		// call under test
		assertEquals(new PresignedUrl().withUrl(resultUrl), dao.createPartUploadPreSignedUrl(bucket, key, contentType));
		verify(mockGoogleCloudStorageClient).createSignedUrl(bucket, key, GoogleUtils.PRE_SIGNED_URL_EXPIRATION_MS,
				HttpMethod.PUT);
	}

	@Test
	public void testCreatePartUploadPreSignedUrlWithNullBucket() {
		String bucket = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.createPartUploadPreSignedUrl(bucket, key, contentType);
		}).getMessage();
		assertEquals("bucket is required.", message);
		verifyZeroInteractions(mockGoogleCloudStorageClient);
	}

	@Test
	public void testCreatePartUploadPreSignedUrlWithNullKey() {
		String key = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.createPartUploadPreSignedUrl(bucket, key, contentType);
		}).getMessage();
		assertEquals("key is required.", message);
		verifyZeroInteractions(mockGoogleCloudStorageClient);
	}

	@Test
	public void testComposeLockedParts() {

		// call under test
		dao.composeLockedParts(uploadId, bucket, key, toCompose);

		verify(mockGoogleCloudStorageClient).composeObjects(bucket, "key/1-12", List.of("key/1-4", "key/5-12"));
		verify(mockAsyncComposerDAO).addPart(uploadId, new PartRange().setLowerBound(1L).setUpperBound(12L));
		verify(mockAsyncComposerDAO, times(2)).removePart(any(), any());
		verify(mockAsyncComposerDAO).removePart(uploadId, left);
		verify(mockAsyncComposerDAO).removePart(uploadId, right);
		verify(mockGoogleCloudStorageClient, times(2)).deleteObject(any(), any());
		verify(mockGoogleCloudStorageClient).deleteObject(bucket, "key/1-4");
		verify(mockGoogleCloudStorageClient).deleteObject(bucket, "key/5-12");

	}

	@Test
	public void testValidateCompose() {
		// call under test
		dao.validateCompose(toCompose);
		verify(dao).validatePartRange(left);
		verify(dao).validatePartRange(right);
	}

	@Test
	public void testValidateComposeWithNull() {
		toCompose = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.validateCompose(toCompose);
		}).getMessage();
		assertEquals("compose is required.", message);
	}

	@Test
	public void testValidatePartRange() {
		// call under test
		dao.validatePartRange(left);
	}

	@Test
	public void testValidatePartRangeWithNullPart() {
		left = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.validatePartRange(left);
		}).getMessage();
		assertEquals("range is required.", message);
	}

	@Test
	public void testValidatePartRangeWithNullLower() {
		left.setLowerBound(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.validatePartRange(left);
		}).getMessage();
		assertEquals("range.lowerBound is required.", message);
	}

	@Test
	public void testValidatePartRangeWithNullUpper() {
		left.setUpperBound(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.validatePartRange(left);
		}).getMessage();
		assertEquals("range.upperBound is required.", message);
	}

	@Test
	public void testValidateAndAddPart() {
		
		when(mockGoogleCloudStorageClient.getObject(any(), any())).thenReturn(mockBlob);
		when(mockBlob.getMd5()).thenReturn(md5Base64);
		when(mockAsyncComposerDAO.findContiguousParts(any(), any(), anyInt())).thenReturn(
				List.of(new Compose().setLeft(rangeFiveToSix).setRight(rangeSevenToEight),
						new Compose().setLeft(rangeOneToTwo).setRight(rangeThreeToFour)));
		boolean locksAcquired = true;
		setupAttemptToLockParts(locksAcquired);
		doNothing().when(dao).composeLockedParts(any(), any(), any(), any());

		// call under test
		dao.validateAndAddPart(addPartRequest);

		verify(mockAsyncComposerDAO).addPart(uploadId, new PartRange().setLowerBound(8L).setUpperBound(8L));
		verify(mockGoogleCloudStorageClient).getObject(bucket, key+"/8-8");
		verify(mockAsyncComposerDAO).findContiguousParts(uploadId, OrderBy.random, 4);
		verify(mockAsyncComposerDAO, times(2)).attemptToLockParts(any(), any(), any());
		verify(mockAsyncComposerDAO).attemptToLockParts(eq(uploadId), any(), eq(rangeFiveToSix), eq(rangeSevenToEight));
		verify(mockAsyncComposerDAO).attemptToLockParts(eq(uploadId), any(), eq(rangeOneToTwo), eq(rangeThreeToFour));
		
		verify(dao, times(2)).composeLockedParts(any(), any(), any(), any());
		verify(dao).composeLockedParts(uploadId, bucket, key, new Compose().setLeft(rangeFiveToSix).setRight(rangeSevenToEight));
		verify(dao).composeLockedParts(uploadId, bucket, key, new Compose().setLeft(rangeOneToTwo).setRight(rangeThreeToFour));
		verifyNoMoreInteractions(mockAsyncComposerDAO,mockGoogleCloudStorageClient);
	}

	@Test
	public void testValidateAndAddPartWithNoContiguousParts() {
		
		when(mockGoogleCloudStorageClient.getObject(any(), any())).thenReturn(mockBlob);
		when(mockBlob.getMd5()).thenReturn(md5Base64);
		when(mockAsyncComposerDAO.findContiguousParts(any(), any(), anyInt())).thenReturn(Collections.emptyList());

		// call under test
		dao.validateAndAddPart(addPartRequest);

		verify(mockAsyncComposerDAO).addPart(uploadId, new PartRange().setLowerBound(8L).setUpperBound(8L));
		verify(mockGoogleCloudStorageClient).getObject(bucket, key+"/8-8");
		verify(mockAsyncComposerDAO).findContiguousParts(uploadId, OrderBy.random, 4);
		verifyNoMoreInteractions(mockAsyncComposerDAO);
		verify(dao, never()).composeLockedParts(any(), any(), any(), any());
	}

	@Test
	public void testValidateAndAddPartWithNotLocked() {
		
		when(mockGoogleCloudStorageClient.getObject(any(), any())).thenReturn(mockBlob);
		when(mockBlob.getMd5()).thenReturn(md5Base64);
		when(mockAsyncComposerDAO.findContiguousParts(any(), any(), anyInt())).thenReturn(
				List.of(new Compose().setLeft(rangeFiveToSix).setRight(rangeSevenToEight),
						new Compose().setLeft(rangeOneToTwo).setRight(rangeThreeToFour)));
		boolean locksAcquired = false;
		setupAttemptToLockParts(locksAcquired);

		// call under test
		dao.validateAndAddPart(addPartRequest);

		verify(mockAsyncComposerDAO).addPart(uploadId, new PartRange().setLowerBound(8L).setUpperBound(8L));
		verify(mockGoogleCloudStorageClient).getObject(bucket, key+"/8-8");
		verify(mockAsyncComposerDAO).findContiguousParts(uploadId, OrderBy.random, 4);
		verify(mockAsyncComposerDAO, times(2)).attemptToLockParts(any(), any(), any());
		verify(mockAsyncComposerDAO).attemptToLockParts(eq(uploadId), any(), eq(rangeFiveToSix), eq(rangeSevenToEight));
		verify(mockAsyncComposerDAO).attemptToLockParts(eq(uploadId), any(), eq(rangeOneToTwo), eq(rangeThreeToFour));
		
		verify(dao, never()).composeLockedParts(any(), any(), any(), any());
	}

	@Test
	public void testValidateAndAddPartWithWrongMD5() {
		when(mockGoogleCloudStorageClient.getObject(any(), any())).thenReturn(mockBlob);
		when(mockBlob.getMd5()).thenReturn("wrong");
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			dao.validateAndAddPart(addPartRequest);
		}).getMessage();
		assertEquals("The provided MD5 does not match the MD5 of the uploaded part.  Please re-upload the part.", message);
		verifyZeroInteractions(mockAsyncComposerDAO);
		verify(mockGoogleCloudStorageClient).getObject(bucket, key+"/8-8");
		verifyNoMoreInteractions(mockGoogleCloudStorageClient);
	}

	@Test
	public void testValidateAndAddPartWithNullRequest() {
		addPartRequest = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.validateAndAddPart(addPartRequest);
		}).getMessage();
		assertEquals("request is required.", message);
		verifyZeroInteractions(mockAsyncComposerDAO, mockGoogleCloudStorageClient);
	}

	@Test
	public void testValidateAndAddPartWithNullBucket() {
		addPartRequest.setBucket(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.validateAndAddPart(addPartRequest);
		}).getMessage();
		assertEquals("request.bucket is required.", message);
		verifyZeroInteractions(mockAsyncComposerDAO, mockGoogleCloudStorageClient);
	}

	@Test
	public void testValidateAndAddPartWithNullKey() {
		addPartRequest.setKey(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.validateAndAddPart(addPartRequest);
		}).getMessage();
		assertEquals("request.key is required.", message);
		verifyZeroInteractions(mockAsyncComposerDAO, mockGoogleCloudStorageClient);
	}

	@Test
	public void testValidateAndAddPartWithNullPartKey() {
		addPartRequest.setPartKey(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.validateAndAddPart(addPartRequest);
		}).getMessage();
		assertEquals("request.partKey is required.", message);
		verifyZeroInteractions(mockAsyncComposerDAO, mockGoogleCloudStorageClient);
	}

	@Test
	public void testValidateAndAddPartWithNullPartMD5Hex() {
		addPartRequest.setPartMD5Hex(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.validateAndAddPart(addPartRequest);
		}).getMessage();
		assertEquals("request.partMD5Hex is required.", message);
		verifyZeroInteractions(mockAsyncComposerDAO, mockGoogleCloudStorageClient);
	}

	@Test
	public void testCompleteMultipartUpload() {
		
		when(mockAsyncComposerDAO.findContiguousParts(any(), any(), anyInt())).thenReturn(
				List.of(new Compose().setLeft(rangeFiveToSix).setRight(rangeSevenToEight)), List.of(
						new Compose().setLeft(rangeOneToTwo).setRight(rangeThreeToFour)), Collections.emptyList());
		boolean locksAcquired = true;
		setupAttemptToLockParts(locksAcquired);
		doNothing().when(dao).composeLockedParts(any(), any(), any(), any());
		when(mockGoogleCloudStorageClient.getObject(any(), any())).thenReturn(mockBlob);
		when(mockBlob.getSize()).thenReturn(987L);

		// call under test
		assertEquals(987L, dao.completeMultipartUpload(completeRequest));

		verify(mockAsyncComposerDAO, times(3)).findContiguousParts(uploadId, OrderBy.asc, 1);
		verify(mockAsyncComposerDAO, times(2)).attemptToLockParts(any(), any(), any());
		verify(mockAsyncComposerDAO).attemptToLockParts(eq(uploadId), any(), eq(rangeFiveToSix), eq(rangeSevenToEight));
		verify(mockAsyncComposerDAO).attemptToLockParts(eq(uploadId), any(), eq(rangeOneToTwo), eq(rangeThreeToFour));
		
		verify(dao, times(2)).composeLockedParts(any(), any(), any(), any());
		verify(dao).composeLockedParts(uploadId, bucket, key, new Compose().setLeft(rangeFiveToSix).setRight(rangeSevenToEight));
		verify(dao).composeLockedParts(uploadId, bucket, key, new Compose().setLeft(rangeOneToTwo).setRight(rangeThreeToFour));
		
		verify(mockGoogleCloudStorageClient).rename(bucket, "key/1-111", key);
		verify(mockGoogleCloudStorageClient).getObject(bucket, key);
		
		verifyNoMoreInteractions(mockAsyncComposerDAO,mockGoogleCloudStorageClient);
	}

	@Test
	public void testCompleteMultipartUploadWithNothingToCompose() {
		
		when(mockAsyncComposerDAO.findContiguousParts(any(), any(), anyInt())).thenReturn(Collections.emptyList());
		when(mockGoogleCloudStorageClient.getObject(any(), any())).thenReturn(mockBlob);
		when(mockBlob.getSize()).thenReturn(987L);

		// call under test
		assertEquals(987L, dao.completeMultipartUpload(completeRequest));

		verify(mockAsyncComposerDAO, times(1)).findContiguousParts(uploadId, OrderBy.asc, 1);
		
		verify(mockGoogleCloudStorageClient).rename(bucket, "key/1-111", key);
		verify(mockGoogleCloudStorageClient).getObject(bucket, key);
		
		verifyNoMoreInteractions(mockAsyncComposerDAO,mockGoogleCloudStorageClient);
	}

	@Test
	public void testCompleteMultipartUploadWithCannotAcquireLock() {
		
		when(mockAsyncComposerDAO.findContiguousParts(any(), any(), anyInt())).thenReturn(
				List.of(new Compose().setLeft(rangeFiveToSix).setRight(rangeSevenToEight)),  Collections.emptyList());
		boolean locksAcquired = false;
		setupAttemptToLockParts(locksAcquired);
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			dao.completeMultipartUpload(completeRequest);
		}).getMessage();
		assertEquals("Cannot perform this action while parts are still being added to this multipart upload.", message);

		verify(mockAsyncComposerDAO, times(1)).findContiguousParts(uploadId, OrderBy.asc, 1);
		verify(mockAsyncComposerDAO, times(1)).attemptToLockParts(any(), any(), any());
		verify(mockAsyncComposerDAO).attemptToLockParts(eq(uploadId), any(), eq(rangeFiveToSix), eq(rangeSevenToEight));	
		verifyNoMoreInteractions(mockAsyncComposerDAO,mockGoogleCloudStorageClient);
	}

	@Test
	public void testCompleteMultipartUploadWithNullRequest() {
		completeRequest = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.completeMultipartUpload(completeRequest);
		}).getMessage();
		assertEquals("request is required.", message);
		verifyZeroInteractions(mockAsyncComposerDAO, mockGoogleCloudStorageClient);
	}

	@Test
	public void testCompleteMultipartUploadWithNullBucket() {
		completeRequest.setBucket(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.completeMultipartUpload(completeRequest);
		}).getMessage();
		assertEquals("request.bucket is required.", message);
		verifyZeroInteractions(mockAsyncComposerDAO, mockGoogleCloudStorageClient);
	}

	@Test
	public void testCompleteMultipartUploadWithNullKey() {
		completeRequest.setKey(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.completeMultipartUpload(completeRequest);
		}).getMessage();
		assertEquals("request.key is required.", message);
		verifyZeroInteractions(mockAsyncComposerDAO, mockGoogleCloudStorageClient);
	}

	@Test
	public void testCompleteMultipartUploadWithNullUploadId() {
		completeRequest.setUploadId(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.completeMultipartUpload(completeRequest);
		}).getMessage();
		assertEquals("request.uploadId is required.", message);
		verifyZeroInteractions(mockAsyncComposerDAO, mockGoogleCloudStorageClient);
	}
	
	@Test
	public void testCompleteMultipartUploadWithNullNumberOfParts() {
		completeRequest.setNumberOfParts(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.completeMultipartUpload(completeRequest);
		}).getMessage();
		assertEquals("request.numberOfParts is required.", message);
		verifyZeroInteractions(mockAsyncComposerDAO, mockGoogleCloudStorageClient);
	}

	@Test
	public void testTryAbortMultipartRequest() {
		
		when(mockAsyncComposerDAO.listAllPartsForUploadId(any())).thenReturn(List.of(rangeOneToTwo,rangeFiveToSix));
		when(mockGoogleCloudStorageClient.getObjects(any(), any())).thenReturn(List.of(mockBlob));
		when(mockGoogleCloudStorageClient.doesObjectExist(any(), any())).thenReturn(true);
		boolean locksAcquired = true;
		setupAttemptToLockParts(locksAcquired);
		
		// call under test
		dao.tryAbortMultipartRequest(abortRequest);
		
		verify(mockAsyncComposerDAO).listAllPartsForUploadId(uploadId);
		verify(mockAsyncComposerDAO, times(2)).attemptToLockParts(any(), any(), any());
		verify(mockAsyncComposerDAO).attemptToLockParts(eq(uploadId), any(), eq(rangeOneToTwo));
		verify(mockAsyncComposerDAO).attemptToLockParts(eq(uploadId), any(), eq(rangeFiveToSix));
		verify(mockAsyncComposerDAO).removePart(uploadId, rangeOneToTwo);
		verify(mockAsyncComposerDAO).removePart(uploadId, rangeFiveToSix);
		verify(mockGoogleCloudStorageClient).getObjects(bucket, "key/");
		verify(mockBlob).delete();
		verify(mockGoogleCloudStorageClient).doesObjectExist(bucket, key);
		verify(mockGoogleCloudStorageClient).deleteObject(bucket, key);
		verifyNoMoreInteractions(mockAsyncComposerDAO,mockGoogleCloudStorageClient);
	}

	@Test
	public void testTryAbortMultipartRequestWithFinalObjectDoesNotExist() {
		
		when(mockAsyncComposerDAO.listAllPartsForUploadId(any())).thenReturn(List.of(rangeOneToTwo,rangeFiveToSix));
		when(mockGoogleCloudStorageClient.getObjects(any(), any())).thenReturn(List.of(mockBlob));
		when(mockGoogleCloudStorageClient.doesObjectExist(any(), any())).thenReturn(false);
		boolean locksAcquired = true;
		setupAttemptToLockParts(locksAcquired);
		
		// call under test
		dao.tryAbortMultipartRequest(abortRequest);
		
		verify(mockAsyncComposerDAO).listAllPartsForUploadId(uploadId);
		verify(mockAsyncComposerDAO, times(2)).attemptToLockParts(any(), any(), any());
		verify(mockAsyncComposerDAO).attemptToLockParts(eq(uploadId), any(), eq(rangeOneToTwo));
		verify(mockAsyncComposerDAO).attemptToLockParts(eq(uploadId), any(), eq(rangeFiveToSix));
		verify(mockAsyncComposerDAO).removePart(uploadId, rangeOneToTwo);
		verify(mockAsyncComposerDAO).removePart(uploadId, rangeFiveToSix);
		verify(mockGoogleCloudStorageClient).getObjects(bucket, "key/");
		verify(mockBlob).delete();
		verify(mockGoogleCloudStorageClient).doesObjectExist(bucket, key);
		verify(mockGoogleCloudStorageClient, never()).deleteObject(any(), any());
		verifyNoMoreInteractions(mockAsyncComposerDAO,mockGoogleCloudStorageClient);
	}

	@Test
	public void testTryAbortMultipartRequestWithUnableToAcquireLock() {
		
		when(mockAsyncComposerDAO.listAllPartsForUploadId(any())).thenReturn(List.of(rangeOneToTwo,rangeFiveToSix));
		boolean locksAcquired = false;
		setupAttemptToLockParts(locksAcquired);
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			dao.tryAbortMultipartRequest(abortRequest);
		}).getMessage();
		assertEquals("Cannot perform this action while parts are still being added to this multipart upload.", message);
			
		verify(mockAsyncComposerDAO).listAllPartsForUploadId(uploadId);
		verify(mockAsyncComposerDAO, times(1)).attemptToLockParts(any(), any(), any());
		verify(mockAsyncComposerDAO).attemptToLockParts(eq(uploadId), any(), eq(rangeOneToTwo));
		verify(mockAsyncComposerDAO, never()).removePart(any(), any());
		verifyZeroInteractions(mockGoogleCloudStorageClient);
		verifyNoMoreInteractions(mockAsyncComposerDAO);
	}
	
	@Test
	public void testTryAbortMultipartRequestWithNullRequest() {
		abortRequest = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			dao.tryAbortMultipartRequest(abortRequest);
		}).getMessage();
		assertEquals("request is required.", message);
		verifyZeroInteractions(mockAsyncComposerDAO, mockGoogleCloudStorageClient);
	}
	
	@Test
	public void testTryAbortMultipartRequestWithNullBucket() {
		abortRequest.setBucket(null);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			dao.tryAbortMultipartRequest(abortRequest);
		}).getMessage();
		assertEquals("request.bucket is required.", message);
		verifyZeroInteractions(mockAsyncComposerDAO, mockGoogleCloudStorageClient);
	}
	
	@Test
	public void testTryAbortMultipartRequestWithNullKey() {
		abortRequest.setKey(null);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			dao.tryAbortMultipartRequest(abortRequest);
		}).getMessage();
		assertEquals("request.key is required.", message);
		verifyZeroInteractions(mockAsyncComposerDAO, mockGoogleCloudStorageClient);
	}
	
	@Test
	public void testTryAbortMultipartRequestWithNullUploadId() {
		abortRequest.setUploadId(null);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			dao.tryAbortMultipartRequest(abortRequest);
		}).getMessage();
		assertEquals("request.uploadId is required.", message);
		verifyZeroInteractions(mockAsyncComposerDAO, mockGoogleCloudStorageClient);
	}

	/**
	 * Helper to set a call to attemptToLockParts().
	 * 
	 * @param success When true the locks will be acquired and the consumer will be
	 *                called. When false the locks will not be acquired and the
	 *                consumer will not be called.
	 */
	void setupAttemptToLockParts(boolean success) {
		doAnswer(new Answer<Boolean>() {

			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				if (success) {
					Consumer<List<PartRange>> consumer = invocation.getArgument(1);
					List<PartRange> parts = new ArrayList<>();
					for (int i = 2; i < invocation.getArguments().length; i++) {
						parts.add(invocation.getArgument(i));
					}
					consumer.accept(parts);
				}
				return success;
			}
		}).when(mockAsyncComposerDAO).attemptToLockParts(any(), any(), any());
	}
}
