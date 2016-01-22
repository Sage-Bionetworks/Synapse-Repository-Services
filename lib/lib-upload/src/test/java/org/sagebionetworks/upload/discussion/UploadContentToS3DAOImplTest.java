package org.sagebionetworks.upload.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class UploadContentToS3DAOImplTest {

	@Mock
	private AmazonS3Client mockS3Client;
	private String bucketName = "bucket";
	private UploadContentToS3DAOImpl dao;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);

		dao = new UploadContentToS3DAOImpl();
		ReflectionTestUtils.setField(dao, "s3Client", mockS3Client);
		ReflectionTestUtils.setField(dao, "bucketName", bucketName);
	}

	@Test
	public void testUploadThreadMessage() throws Exception {
		dao.initialize();
		Mockito.verify(mockS3Client).createBucket(Mockito.anyString());
		Mockito.verify(mockS3Client).setBucketCrossOriginConfiguration(Mockito.anyString(), (BucketCrossOriginConfiguration) Mockito.any());

		String content = "this is a message";
		String forumId = "1";
		String threadId = "2";
		String key = dao.uploadThreadMessage(content, forumId, threadId);
		Mockito.verify(mockS3Client).putObject((PutObjectRequest) Mockito.any());
		assertNotNull(key);
		String[] parts = key.split("/");
		assertTrue(parts.length == 3);
		assertEquals(parts[0], forumId);
		assertEquals(parts[1], threadId);
	}

	@Test
	public void testUploadReplyMessage() throws Exception {
		dao.initialize();
		Mockito.verify(mockS3Client).createBucket(Mockito.anyString());
		Mockito.verify(mockS3Client).setBucketCrossOriginConfiguration(Mockito.anyString(), (BucketCrossOriginConfiguration) Mockito.any());

		String content = "this is a message";
		String forumId = "1";
		String threadId = "2";
		String replyId = "3";
		String key = dao.uploadReplyMessage(content, forumId, threadId, replyId);
		Mockito.verify(mockS3Client).putObject((PutObjectRequest) Mockito.any());
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
		String url = dao.getThreadUrl("1/2/key");
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
		String url = dao.getReplyUrl("1/2/3/key");
		assertNotNull(url);
	}
}
