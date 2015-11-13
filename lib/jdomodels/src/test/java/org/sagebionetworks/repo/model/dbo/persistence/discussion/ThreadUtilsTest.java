package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.repo.model.discussion.DiscussionThread;

public class ThreadUtilsTest {

	@Test
	public void testDBOToDTOAndBack() {
		DBOThread dbo = ThreadTestUtil.createValidatedThread();
		dbo.setEtag(null);

		DiscussionThread dto = ThreadUtils.createDTOFromDBO(dbo);
		assertNotNull(dto);
		assertEquals(dbo.getId().toString(), dto.getId());
		assertEquals(dbo.getForumId().toString(), dto.getForumId());
		assertEquals(dbo.getCreatedOn(), dto.getCreatedOn());
		assertEquals(dbo.getModifiedOn(), dto.getModifiedOn());
		assertEquals(dbo.getCreatedBy().toString(), dto.getCreatedBy());
		assertEquals(dbo.getMessageUrl(), dto.getMessageUrl());
		assertEquals(dbo.getIsEdited(), dto.getIsEdited());
		assertEquals(dbo.getIsDeleted(), dto.getIsDeleted());

		assertEquals(ThreadUtils.createDTOFromDBO(dbo), dto);
	}

	@Test
	public void testValidateThread() {
		DBOThread dbo = ThreadTestUtil.createValidatedThread();
		ThreadUtils.validateDBOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullId() {
		DBOThread dbo = ThreadTestUtil.createValidatedThread();
		dbo.setId(null);
		ThreadUtils.validateDBOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullForumId() {
		DBOThread dbo = ThreadTestUtil.createValidatedThread();
		dbo.setForumId(null);
		ThreadUtils.validateDBOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullTitle() {
		DBOThread dbo = ThreadTestUtil.createValidatedThread();
		dbo.setTitle(null);
		ThreadUtils.validateDBOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullCreatedBy() {
		DBOThread dbo = ThreadTestUtil.createValidatedThread();
		dbo.setCreatedBy(null);
		ThreadUtils.validateDBOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullCreatedOn() {
		DBOThread dbo = ThreadTestUtil.createValidatedThread();
		dbo.setCreatedOn(null);
		ThreadUtils.validateDBOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullModifiedOn() {
		DBOThread dbo = ThreadTestUtil.createValidatedThread();
		dbo.setModifiedOn(null);
		ThreadUtils.validateDBOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullMessageUrl() {
		DBOThread dbo = ThreadTestUtil.createValidatedThread();
		dbo.setMessageUrl(null);
		ThreadUtils.validateDBOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullIsEdited() {
		DBOThread dbo = ThreadTestUtil.createValidatedThread();
		dbo.setIsEdited(null);
		ThreadUtils.validateDBOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullIsDeleted() {
		DBOThread dbo = ThreadTestUtil.createValidatedThread();
		dbo.setIsDeleted(null);
		ThreadUtils.validateDBOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullEtag() {
		DBOThread dbo = ThreadTestUtil.createValidatedThread();
		dbo.setEtag(null);
		ThreadUtils.validateDBOAndThrowException(dbo);
	}
}
