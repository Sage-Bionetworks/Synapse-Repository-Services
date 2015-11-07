package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.discussion.Forum;

public class ForumUtilsTest {
	private static final Long ID = 123L;
	private static final Long PROJECT_ID = 987L;

	@Test
	public void testDBOToDTOAndBack() {
		DBOForum dbo = new DBOForum();
		dbo.setId(ID);
		dbo.setProjectId(PROJECT_ID);
		// From DBO to DTO
		Forum dto = ForumUtils.createDTOFromDBO(dbo);
		assertEquals(dto.getId(), ID);
		assertEquals(dto.getProjectId(), PROJECT_ID);
		// From DTO to DBO
		DBOForum dbo2 = ForumUtils.createDBOFromDTO(dto);
		assertEquals(dbo, dbo2);
	}

}
