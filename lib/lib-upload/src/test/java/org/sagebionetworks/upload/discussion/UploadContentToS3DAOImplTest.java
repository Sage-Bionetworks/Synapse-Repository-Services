package org.sagebionetworks.upload.discussion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.SynapseS3Client;

import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

@ExtendWith(MockitoExtension.class)
public class UploadContentToS3DAOImplTest {

	@Mock
	private SynapseS3Client mockS3Client;
	@Mock
	private S3Object mockS3Object;
	@Mock
	private S3ObjectInputStream mockInputStream;
	@Captor
	private ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor;
	@InjectMocks
	private UploadContentToS3DAOImpl dao;

	private String bucketName = "bucket";
	private URL url;

	@BeforeEach
	public void before() throws MalformedURLException {
		dao.setBucketName(bucketName);
		url = new URL("https://www.synapse.org/");
	}

	@Test
	public void testInitialize() {
		// call under test
		dao.initialize();
		verify(mockS3Client).setBucketCrossOriginConfiguration(eq(bucketName), any(BucketCrossOriginConfiguration.class));
	}

	@Test
	public void testUploadThreadMessage() throws Exception {
		String content = "this is a message";
		String forumId = "1";
		String threadId = "2";
		// call under test
		String key = dao.uploadThreadMessage(content, forumId, threadId);
		assertNotNull(key);
		verifyPutObjectRequest(key);
		String[] parts = key.split("/");
		assertEquals(3, parts.length);
		assertEquals(forumId, parts[0]);
		assertEquals(threadId, parts[1]);
	}

	@Test
	public void testUploadReplyMessage() throws Exception {
		String content = "this is a message";
		String forumId = "1";
		String threadId = "2";
		String replyId = "3";
		// call under test
		String key = dao.uploadReplyMessage(content, forumId, threadId, replyId);
		assertNotNull(key);
		verifyPutObjectRequest(key);
		String[] parts = key.split("/");
		assertEquals(4, parts.length);
		assertEquals(forumId, parts[0]);
		assertEquals(threadId, parts[1]);
		assertEquals(replyId, parts[2]);
	}

	private void verifyPutObjectRequest(String expectedKey) {
		verify(mockS3Client).putObject(putObjectRequestCaptor.capture());
		PutObjectRequest request = putObjectRequestCaptor.getValue();
		assertEquals(bucketName, request.getBucketName());
		assertEquals(expectedKey, request.getKey());
		// Messages are served through pre-signed URLs, so objects must not be publicly readable
		assertNull(request.getCannedAcl());
		assertNull(request.getAccessControlList());
		ObjectMetadata metadata = request.getMetadata();
		assertEquals("text/plain; charset=utf-8", metadata.getContentType());
		assertEquals("gzip", metadata.getContentEncoding());
	}

	@Test
	public void testGetThreadUrlWithNullKey() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.getThreadUrl(null);
		});
		verifyNoInteractions(mockS3Client);
	}

	@Test
	public void testGetThreadUrlWithBadKey() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.getThreadUrl("1/2/3/key");
		});
		verifyNoInteractions(mockS3Client);
	}

	@Test
	public void testGetThreadUrl() {
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(url);
		// call under test
		String messageUrl = dao.getThreadUrl("1/2/key").getMessageUrl();
		assertEquals(url.toString(), messageUrl);
	}

	@Test
	public void testGetReplyUrlWithNullKey() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.getReplyUrl(null);
		});
		verifyNoInteractions(mockS3Client);
	}

	@Test
	public void testGetReplyUrlWithBadKey() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.getReplyUrl("1/2/key");
		});
		verifyNoInteractions(mockS3Client);
	}

	@Test
	public void testGetReplyUrl() {
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(url);
		// call under test
		String messageUrl = dao.getReplyUrl("1/2/3/key").getMessageUrl();
		assertEquals(url.toString(), messageUrl);
	}

	@Test
	public void testGetMessage() throws IOException {
		byte[] compressedBytes = UploadContentToS3DAOImpl.compress("message");
		S3ObjectInputStream s3ObjectInputStream = new S3ObjectInputStream(new ByteArrayInputStream(compressedBytes), null);
		when(mockS3Client.getObject(anyString(), anyString())).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(s3ObjectInputStream);
		String key = "key";
		// call under test
		String message = dao.getMessage(key);
		assertEquals("message", message);
		verify(mockS3Client).getObject(bucketName, key);
	}

	@Test
	public void testGetMessageWithNullInputStream() {
		when(mockS3Client.getObject(anyString(), anyString())).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(null);
		String key = "key";
		assertThrows(NullPointerException.class, () -> {
			// call under test
			dao.getMessage(key);
		});
		verify(mockS3Client).getObject(bucketName, key);
		verify(mockS3Object).getObjectContent();
	}

	@Test
	public void testGetMessageCloseInputStream() throws IOException {
		when(mockS3Client.getObject(anyString(), anyString())).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(mockInputStream);
		String key = "key";
		// reading from the mock stream yields no gzip header, so the read fails
		assertThrows(RuntimeException.class, () -> {
			// call under test
			dao.getMessage(key);
		});
		verify(mockS3Client).getObject(bucketName, key);
		verify(mockS3Object).getObjectContent();
		verify(mockInputStream).close();
	}
}
