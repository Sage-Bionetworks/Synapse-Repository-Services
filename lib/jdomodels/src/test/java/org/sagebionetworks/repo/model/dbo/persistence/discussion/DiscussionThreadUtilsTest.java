package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.discussion.CreateThread;

public class DiscussionThreadUtilsTest {

	@Test
	public void testValidCreateThread() {
		CreateThread createThread = DiscussionThreadTestUtil.createValidCreateThread();
		DiscussionThreadUtils.validateCreateThreadAndThrowException(createThread);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testInvalidCreateThreadWithNullForumId() {
		CreateThread createThread = DiscussionThreadTestUtil.createValidCreateThread();
		createThread.setForumId(null);
		DiscussionThreadUtils.validateCreateThreadAndThrowException(createThread);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testInvalidCreateThreadWithNullTitle() {
		CreateThread createThread = DiscussionThreadTestUtil.createValidCreateThread();
		createThread.setTitle(null);
		DiscussionThreadUtils.validateCreateThreadAndThrowException(createThread);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testInvalidCreateThreadWithNullMessage() {
		CreateThread createThread = DiscussionThreadTestUtil.createValidCreateThread();
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
		assertTrue(timestamp < dbo.getCreatedOn());
		assertTrue(timestamp < dbo.getModifiedOn());
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
}
