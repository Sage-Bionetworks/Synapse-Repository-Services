package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
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
		DBODiscussionThread dbo = DiscussionThreadUtils.createDBO(forumId, title,
				messageUrl, userId, id, etag);
		assertNotNull(dbo);
		assertEquals(dbo.getId().toString(), id);
		assertEquals(dbo.getForumId().toString(), forumId);
		assertEquals(new String(dbo.getTitle(), DiscussionThreadUtils.UTF8), title);
		assertEquals(dbo.getMessageUrl(), messageUrl);
		assertEquals(dbo.getCreatedBy(), userId);
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
	public void testConvertListToStringAndBackWithEmptyList() {
		List<String> list = new ArrayList<String>();
		String empty = DiscussionThreadUtils.toString(list);
		assertEquals(empty, "");
		List<String> result = DiscussionThreadUtils.toList(empty);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testConvertListToStringAndBackWithOneElmList() {
		List<String> list = new ArrayList<String>();
		list.add("a");
		String string = DiscussionThreadUtils.toString(list);
		assertEquals(string, "a");
		List<String> result = DiscussionThreadUtils.toList(string);
		assertEquals(list, result);
	}

	@Test
	public void testConvertListToStringAndBackWithMoreThanOneElmList() {
		List<String> list = new ArrayList<String>();
		list.addAll(Arrays.asList("a", "b", "c"));
		String string = DiscussionThreadUtils.toString(list);
		assertEquals(string, "a,b,c");
		List<String> result = DiscussionThreadUtils.toList(string);
		assertEquals(list, result);
	}
}
