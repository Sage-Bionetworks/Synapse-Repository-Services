package org.sagebionetworks.upload.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

public class UploadContentToS3DAOImplTest {

	@Mock
	private AmazonS3 mockS3Client;
	@Mock
	private S3Object mockS3Object;
	@Mock
	private S3ObjectInputStream mockInputStream;
	private String bucketName = "bucket";
	private UploadContentToS3DAOImpl dao;
	private URL url;

	@Before
	public void before() throws MalformedURLException {
		MockitoAnnotations.initMocks(this);

		dao = new UploadContentToS3DAOImpl();
		ReflectionTestUtils.setField(dao, "s3Client", mockS3Client);
		ReflectionTestUtils.setField(dao, "bucketName", bucketName);
		url = new URL("https://www.synapse.org/");
	}

	@Test
	public void testUploadThreadMessage() throws Exception {
		dao.initialize();
		verify(mockS3Client).createBucket(Mockito.anyString());
		verify(mockS3Client).setBucketCrossOriginConfiguration(Mockito.anyString(), (BucketCrossOriginConfiguration) Mockito.any());

		String content = "this is a message";
		String forumId = "1";
		String threadId = "2";
		String key = dao.uploadThreadMessage(content, forumId, threadId);
		verify(mockS3Client).putObject((PutObjectRequest) Mockito.any());
		assertNotNull(key);
		String[] parts = key.split("/");
		assertTrue(parts.length == 3);
		assertEquals(parts[0], forumId);
		assertEquals(parts[1], threadId);
	}

	@Test
	public void testUploadReplyMessage() throws Exception {
		dao.initialize();
		verify(mockS3Client).createBucket(Mockito.anyString());
		verify(mockS3Client).setBucketCrossOriginConfiguration(Mockito.anyString(), (BucketCrossOriginConfiguration) Mockito.any());

		String content = "this is a message";
		String forumId = "1";
		String threadId = "2";
		String replyId = "3";
		String key = dao.uploadReplyMessage(content, forumId, threadId, replyId);
		verify(mockS3Client).putObject((PutObjectRequest) Mockito.any());
		assertNotNull(key);
		String[] parts = key.split("/");
		assertTrue(parts.length == 4);
		assertEquals(parts[0], forumId);
		assertEquals(parts[1], threadId);
		assertEquals(parts[2], replyId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetThreadUrlWithNullKey() {
		dao.getThreadUrl(null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetThreadUrlWithBadKey() {
		dao.getThreadUrl("1/2/3/key");
	}

	@Test
	public void testGetThreadUrl() {
		when(mockS3Client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class)))
				.thenReturn(url);
		String url = dao.getThreadUrl("1/2/key").getMessageUrl();
		assertNotNull(url);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepyUrlWithNullKey() {
		dao.getReplyUrl(null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetReplyUrlWithBadKey() {
		dao.getReplyUrl("1/2/key");
	}

	@Test
	public void testGetReplyUrl() {
		when(mockS3Client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class)))
				.thenReturn(url);
		String url = dao.getReplyUrl("1/2/3/key").getMessageUrl();
		assertNotNull(url);
	}

	@Test
	public void testGetMessage() throws IOException {
		byte[] compressedBytes = UploadContentToS3DAOImpl.compress("message");
		ByteArrayInputStream in = new ByteArrayInputStream(compressedBytes);
		S3ObjectInputStream s3ObjectInputStream = new S3ObjectInputStream(in, null);
		when(mockS3Client.getObject(anyString(), anyString())).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(s3ObjectInputStream);
		String key = "key";
		dao.getMessage(key);
		verify(mockS3Client).getObject(anyString(), eq(key));
	}

	@Test (expected = NullPointerException.class)
	public void testGetMessageWithNullInputStream() throws IOException {
		when(mockS3Client.getObject(anyString(), anyString())).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(null);
		String key = "key";
		dao.getMessage(key);
		verify(mockS3Client).getObject(anyString(), eq(key));
		verify(mockS3Object).getObjectContent();
	}

	@Test
	public void testGetMessageCloseInputStream() throws IOException {
		when(mockS3Client.getObject(anyString(), anyString())).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(mockInputStream);
		String key = "key";
		try {
			dao.getMessage(key);
		} catch (RuntimeException e) {
			// as expected
		}
		verify(mockS3Client).getObject(anyString(), eq(key));
		verify(mockS3Object).getObjectContent();
		verify(mockInputStream).close();
	}
}
