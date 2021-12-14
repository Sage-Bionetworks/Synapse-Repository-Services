package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public class ForumUtilsTest {
	private static final Long ID = 123L;
	private static final Long PROJECT_ID = 987L;

	@Test
	public void testDBOToDTOAndBack() {
		DBOForum dbo = new DBOForum();
		dbo.setId(ID);
		dbo.setProjectId(PROJECT_ID);
		dbo.setEtag(UUID.randomUUID().toString());
		// From DBO to DTO
		Forum dto = ForumUtils.createDTOFromDBO(dbo);
		assertEquals(dto.getId(), ID.toString());
		assertEquals(KeyFactory.stringToKey(dto.getProjectId()), PROJECT_ID);
		assertEquals(dto.getEtag(), dbo.getEtag());
		// From DTO to DBO
		DBOForum dbo2 = ForumUtils.createDBOFromDTO(dto);
		assertEquals(dbo, dbo2);
	}

}
