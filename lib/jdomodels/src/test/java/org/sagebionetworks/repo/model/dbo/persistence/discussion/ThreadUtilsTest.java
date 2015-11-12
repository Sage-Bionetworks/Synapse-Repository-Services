package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.discussion.Thread;

public class ThreadUtilsTest {

	@Test
	public void testDTOToDBOAndBack() {
		Thread dto = new Thread();
		dto.setId("1");
		dto.setForumId("2");
		dto.setTitle("title");
		dto.setCreatedOn(new Date());
		dto.setCreatedBy("3");
		dto.setModifiedOn(new Date());
		dto.setMessageKey("messageKey");
		dto.setIsEdited(false);
		dto.setIsDeleted(true);

		DBOThread dbo = ThreadUtils.createDBOFromDTO(dto);
		assertNotNull(dbo);
		assertEquals(dbo.getId().toString(), dto.getId());
		assertEquals(dbo.getForumId().toString(), dto.getForumId());
		assertEquals(dbo.getCreatedOn(), dto.getCreatedOn());
		assertEquals(dbo.getModifiedOn(), dto.getModifiedOn());
		assertEquals(dbo.getCreatedBy().toString(), dto.getCreatedBy());
		assertEquals(dbo.getMessageKey(), dto.getMessageKey());
		assertEquals(dbo.isEdited(), dto.getIsEdited());
		assertEquals(dbo.isDeleted(), dto.getIsDeleted());

		assertEquals(ThreadUtils.createDTOFromDBO(dbo), dto);
	}

}
