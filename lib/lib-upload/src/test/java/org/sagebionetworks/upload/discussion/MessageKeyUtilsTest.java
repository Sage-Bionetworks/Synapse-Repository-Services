package org.sagebionetworks.upload.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MessageKeyUtilsTest {

	@Test (expected=IllegalArgumentException.class)
	public void testValidateThreadKeyNullKey() {
		MessageKeyUtils.validateThreadKey(null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateThreadKeyBadKeyWithLessParts() {
		MessageKeyUtils.validateThreadKey("forum/thread");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateThreadKeyBadKeyWithMoreParts() {
		MessageKeyUtils.validateThreadKey("forum/thread/reply/UUID");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateThreadKeyBadKeyWithBadForumId() {
		MessageKeyUtils.validateThreadKey("forumId/1/UUID");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateThreadKeyBadKeyWithBadThreadId() {
		MessageKeyUtils.validateThreadKey("1/thread/UUID");
	}

	@Test
	public void testValidateThreadKeyWithValidKey() {
		MessageKeyUtils.validateThreadKey("1/1/UUID");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateReplyKeyNullKey() {
		MessageKeyUtils.validateReplyKey(null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateReplyKeyBadKeyWithLessParts() {
		MessageKeyUtils.validateReplyKey("forum/thread");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateReplyKeyBadKeyWithMoreParts() {
		MessageKeyUtils.validateReplyKey("project/forum/thread/reply/UUID");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateReplyKeyBadKeyWithBadForumId() {
		MessageKeyUtils.validateReplyKey("forumId/1/2/UUID");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateReplyKeyBadKeyWithBadThreadId() {
		MessageKeyUtils.validateReplyKey("1/thread/2/UUID");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateReplyKeyBadKeyWithBadReplyId() {
		MessageKeyUtils.validateReplyKey("1/2/reply/UUID");
	}

	@Test
	public void testValidateReplyKeyWithValidKey() {
		MessageKeyUtils.validateReplyKey("1/1/2/UUID");
	}

	@Test
	public void testGetThreadId() {
		assertEquals("2", MessageKeyUtils.getThreadId("1/2/UUID"));
	}

	@Test
	public void testGetReplyId() {
		assertEquals("3", MessageKeyUtils.getReplyId("1/2/3/UUID"));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGenerateThreadKeyNullForumId() {
		MessageKeyUtils.generateThreadKey(null, "2");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGenerateThreadKeyNullThreadId() {
		MessageKeyUtils.generateThreadKey("1", null);
	}

	@Test
	public void testGenerateThreadKey() {
		assertTrue(MessageKeyUtils.generateThreadKey("1", "2").startsWith("1/2/"));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGenerateReplyKeyNullForumId() {
		MessageKeyUtils.generateReplyKey(null, "2", "3");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGenerateReplyKeyNullThreadId() {
		MessageKeyUtils.generateReplyKey("1", null, "3");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGenerateReplyKeyNullReplyId() {
		MessageKeyUtils.generateReplyKey("1", "2", null);
	}

	@Test
	public void testGenerateReplyKey() {
		assertTrue(MessageKeyUtils.generateReplyKey("1", "2", "3").startsWith("1/2/3/"));
	}
}
