package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class DiscussionReplyUtilsTest {

	private String threadId = "1";
	private String messageKey = "messageKey";
	private Long userId = 234L;
	private Long id = 567L;
	private String etag = "etag";

	@Test
	public void testCreateDBO() {
		DBODiscussionReply dbo = DiscussionReplyUtils.createDBO(threadId, messageKey, userId, id, etag);
		assertNotNull(dbo);
		assertEquals(threadId, dbo.getThreadId().toString());
		assertEquals(messageKey, dbo.getMessageKey());
		assertEquals(userId, dbo.getCreatedBy());
		assertEquals(id, dbo.getId());
		assertEquals(etag, dbo.getEtag());
		assertFalse(dbo.getIsEdited());
		assertFalse(dbo.getIsDeleted());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateDBOWithNullThreadId() {
		DiscussionReplyUtils.createDBO(null, messageKey, userId, id, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateDBOWithNullMessageKey() {
		DiscussionReplyUtils.createDBO(threadId, null, userId, id, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateDBOWithNullUserId() {
		DiscussionReplyUtils.createDBO(threadId, messageKey, null, id, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateDBOWithNullId() {
		DiscussionReplyUtils.createDBO(threadId, messageKey, userId, null, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateDBOWithNullEtag() {
		DiscussionReplyUtils.createDBO(threadId, messageKey, userId, id, null);
	}
}
