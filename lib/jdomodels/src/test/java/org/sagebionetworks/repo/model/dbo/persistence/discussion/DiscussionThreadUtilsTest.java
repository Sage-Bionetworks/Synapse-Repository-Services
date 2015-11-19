package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;

public class DiscussionThreadUtilsTest {

	@Test
	public void testValidCreateThread() {
		CreateDiscussionThread createThread = DiscussionThreadTestUtil.createValidCreateDiscussionThread();
		DiscussionThreadUtils.validateCreateThreadAndThrowException(createThread);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testInvalidCreateThreadWithNullForumId() {
		CreateDiscussionThread createThread = DiscussionThreadTestUtil.createValidCreateDiscussionThread();
		createThread.setForumId(null);
		DiscussionThreadUtils.validateCreateThreadAndThrowException(createThread);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testInvalidCreateThreadWithNullTitle() {
		CreateDiscussionThread createThread = DiscussionThreadTestUtil.createValidCreateDiscussionThread();
		createThread.setTitle(null);
		DiscussionThreadUtils.validateCreateThreadAndThrowException(createThread);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testInvalidCreateThreadWithNullMessage() {
		CreateDiscussionThread createThread = DiscussionThreadTestUtil.createValidCreateDiscussionThread();
		createThread.setMessageMarkdown(null);
		DiscussionThreadUtils.validateCreateThreadAndThrowException(createThread);
	}

	@Test
	public void testCreateDBO() throws InterruptedException {
		String forumId = "1";
		String title = "title";
		String messageUrl = "messageUrl";
		Long userId = 2L;
		String id = "3";
		String etag = "etag";
		Long timestamp = new Date().getTime();
		Thread.sleep(100);
		DBODiscussionThread dbo = DiscussionThreadUtils.createDBO(forumId, title,
				messageUrl, userId, id, etag);
		assertNotNull(dbo);
		assertEquals(dbo.getId().toString(), id);
		assertEquals(dbo.getForumId().toString(), forumId);
		assertEquals(new String(dbo.getTitle(), DiscussionThreadUtils.UTF8), title);
		assertEquals(dbo.getMessageUrl(), messageUrl);
		assertEquals(dbo.getCreatedBy(), userId);
		//assertTrue(new Date(timestamp).before(dbo.getCreatedOn()));
		//assertTrue(new Date(timestamp).before(dbo.getModifiedOn()));
		assertEquals(dbo.getEtag(), etag);
		assertFalse(dbo.getIsEdited());
		assertFalse(dbo.getIsDeleted());

		try {
			DiscussionThreadUtils.createDBO(null, title, messageUrl, userId, id, etag);
			fail("Must throw exception for null forumId");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			DiscussionThreadUtils.createDBO(forumId, null, messageUrl, userId, id, etag);
			fail("Must throw exception for null title");
		} catch (NullPointerException e) {
			// as expected
		}
		try {
			DiscussionThreadUtils.createDBO(forumId, title, null, userId, id, etag);
			fail("Must throw exception for null messageUrl");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			DiscussionThreadUtils.createDBO(forumId, title, messageUrl, null, id, etag);
			fail("Must throw exception for null userId");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			DiscussionThreadUtils.createDBO(forumId, title, messageUrl, userId, null, etag);
			fail("Must throw exception for null id");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			DiscussionThreadUtils.createDBO(forumId, title, messageUrl, userId, id, null);
			fail("Must throw exception for null etag");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testCompressAndDecompressUTF8() {
		String string = "This is a title";
		byte[] bytes = DiscussionThreadUtils.compressUTF8(string);
		assertEquals(string, DiscussionThreadUtils.decompressUTF8(bytes));
	}

	@Test
	public void testCreateList() {
		List<Long> longList = Arrays.asList(1L, 2L, 3L, 4L, 5L);
		List<String> stringList = Arrays.asList("1", "2", "3", "4", "5");
		byte[] bytes = DiscussionThreadUtils.compressUTF8(longList.toString());
		String decompress = DiscussionThreadUtils.decompressUTF8(bytes);
		assertEquals(stringList, DiscussionThreadUtils.createList(decompress));
	}
}
