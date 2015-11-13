package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.discussion.DiscussionThread;

public class ThreadUtilsTest {

	@Test
	public void testDBOToDTOAndBack() {
		DiscussionThread dto = ThreadTestUtil.createValidThread();

		DBOThread dbo = ThreadUtils.createDBOFromDTO(dto);
		assertNotNull(dto);
		assertEquals(dbo.getId().toString(), dto.getId());
		assertEquals(dbo.getForumId().toString(), dto.getForumId());
		assertEquals(new Date(dbo.getCreatedOn()), dto.getCreatedOn());
		assertEquals(new Date(dbo.getModifiedOn()), dto.getModifiedOn());
		assertEquals(dbo.getCreatedBy().toString(), dto.getCreatedBy());
		assertEquals(dbo.getMessageUrl(), dto.getMessageUrl());
		assertEquals(dbo.getIsEdited(), dto.getIsEdited());
		assertEquals(dbo.getIsDeleted(), dto.getIsDeleted());

		assertEquals(ThreadUtils.createDTOFromDBO(dbo), dto);
	}

	@Test
	public void testValidateThread() {
		DiscussionThread dbo = ThreadTestUtil.createValidThread();
		ThreadUtils.validateCreateDTOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullForumId() {
		DiscussionThread dbo = ThreadTestUtil.createValidThread();
		dbo.setForumId(null);
		ThreadUtils.validateCreateDTOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullTitle() {
		DiscussionThread dbo = ThreadTestUtil.createValidThread();
		dbo.setTitle(null);
		ThreadUtils.validateCreateDTOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullCreatedBy() {
		DiscussionThread dbo = ThreadTestUtil.createValidThread();
		dbo.setCreatedBy(null);
		ThreadUtils.validateCreateDTOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullCreatedOn() {
		DiscussionThread dbo = ThreadTestUtil.createValidThread();
		dbo.setCreatedOn(null);
		ThreadUtils.validateCreateDTOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullModifiedOn() {
		DiscussionThread dbo = ThreadTestUtil.createValidThread();
		dbo.setModifiedOn(null);
		ThreadUtils.validateCreateDTOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullMessageUrl() {
		DiscussionThread dbo = ThreadTestUtil.createValidThread();
		dbo.setMessageUrl(null);
		ThreadUtils.validateCreateDTOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullIsEdited() {
		DiscussionThread dbo = ThreadTestUtil.createValidThread();
		dbo.setIsEdited(null);
		ThreadUtils.validateCreateDTOAndThrowException(dbo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateThreadWithNullIsDeleted() {
		DiscussionThread dbo = ThreadTestUtil.createValidThread();
		dbo.setIsDeleted(null);
		ThreadUtils.validateCreateDTOAndThrowException(dbo);
	}
}
