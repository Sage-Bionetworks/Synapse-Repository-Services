package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DiscussionThreadUtilsTest {

	private String forumId;
	private String title;
	private String messageKey;
	private long userId;
	private String id;
	private String etag;

	@Before
	public void before() {
		forumId = "1";
		title = "title";
		messageKey = "messageKey";
		userId = 2L;
		id = "3";
		etag = "etag";
	}

	@Test
	public void testCreateDBO() throws InterruptedException {
		DBODiscussionThread dbo = DiscussionThreadUtils.createDBO(forumId, title,
				messageKey, userId, id, etag);
		assertNotNull(dbo);
		assertEquals(dbo.getId().toString(), id);
		assertEquals(dbo.getForumId().toString(), forumId);
		assertEquals(new String(dbo.getTitle(), DiscussionThreadUtils.UTF8), title);
		assertEquals(dbo.getMessageKey(), messageKey);
		assertEquals(dbo.getCreatedBy(), (Long) userId);
		assertEquals(dbo.getEtag(), etag);
		assertFalse(dbo.getIsEdited());
		assertFalse(dbo.getIsDeleted());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testInvalidDBOWithNullForumId() {
		DiscussionThreadUtils.createDBO(null, title, messageKey, userId, id, etag);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testInvalidDBOWithNullTitle(){
		DiscussionThreadUtils.createDBO(forumId, null, messageKey, userId, id, etag);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testInvalidDBOWithNullMessage(){
		DiscussionThreadUtils.createDBO(forumId, title, null, userId, id, etag);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testInvalidDBOWithNullUserId(){
		DiscussionThreadUtils.createDBO(forumId, title, messageKey, null, id, etag);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testInvalidDBOWithNullId(){
		DiscussionThreadUtils.createDBO(forumId, title, messageKey, userId, null, etag);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testInvalidDBOWithNullEtag(){
		DiscussionThreadUtils.createDBO(forumId, title, messageKey, userId, id, null);
	}

	@Test
	public void testConvertListToStringAndBackWithEmptyList() {
		List<String> list = new ArrayList<String>();
		String empty = DiscussionThreadUtils.toCsvString(list);
		assertEquals(empty, "");
		List<String> result = DiscussionThreadUtils.toList(empty);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testConvertListToStringAndBackWithOneElmList() {
		List<String> list = new ArrayList<String>();
		list.add("a");
		String string = DiscussionThreadUtils.toCsvString(list);
		assertEquals(string, "a");
		List<String> result = DiscussionThreadUtils.toList(string);
		assertEquals(list, result);
	}

	@Test
	public void testConvertListToStringAndBackWithMoreThanOneElmList() {
		List<String> list = new ArrayList<String>();
		list.addAll(Arrays.asList("a", "b", "c"));
		String string = DiscussionThreadUtils.toCsvString(list);
		assertEquals(string, "a,b,c");
		List<String> result = DiscussionThreadUtils.toList(string);
		assertEquals(list, result);
	}
}
