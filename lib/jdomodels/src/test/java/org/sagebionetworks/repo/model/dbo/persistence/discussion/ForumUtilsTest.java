package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.UUID;

import org.junit.Test;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.ForumObjectType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public class ForumUtilsTest {
	private static final Long ID = 123L;
	private static final Long OBJECT_ID = 987L;

	@Test
	public void testDBOToDTOAndBackForEntity() {
		DBOForum dbo = new DBOForum();
		dbo.setId(ID);
		dbo.setObjectId(OBJECT_ID);
		dbo.setObjectType(ForumObjectType.ENTITY.name());
		dbo.setEtag(UUID.randomUUID().toString());
		// From DBO to DTO
		Forum dto = ForumUtils.createDTOFromDBO(dbo);
		assertEquals(ID.toString(), dto.getId());
		assertEquals(KeyFactory.keyToString(OBJECT_ID), dto.getObjectId());
		assertEquals(ForumObjectType.ENTITY, dto.getObjectType());
		// projectId is populated for backward compat on entity forums
		assertEquals(KeyFactory.keyToString(OBJECT_ID), dto.getProjectId());
		assertEquals(dbo.getEtag(), dto.getEtag());
		dto.setProjectId(null);
		// From DTO to DBO
		DBOForum dbo2 = ForumUtils.createDBOFromDTO(dto);
		assertEquals(dbo, dbo2);
	}

	@Test
	public void testDBOToDTOAndBackForAccessRequirement() {
		DBOForum dbo = new DBOForum();
		dbo.setId(ID);
		dbo.setObjectId(456L);
		dbo.setObjectType(ForumObjectType.ACCESS_REQUIREMENT.name());
		dbo.setEtag(UUID.randomUUID().toString());
		// From DBO to DTO
		Forum dto = ForumUtils.createDTOFromDBO(dbo);
		assertEquals(ID.toString(), dto.getId());
		assertEquals(KeyFactory.keyToString(456L), dto.getObjectId());
		assertEquals(ForumObjectType.ACCESS_REQUIREMENT, dto.getObjectType());
		// projectId is NOT populated for AR forums
		assertNull(dto.getProjectId());
		assertEquals(dbo.getEtag(), dto.getEtag());
		// From DTO to DBO
		DBOForum dbo2 = ForumUtils.createDBOFromDTO(dto);
		assertEquals(dbo, dbo2);
	}

	@Test
	public void testDBOToDTOWithNullObjectType() {
		DBOForum dbo = new DBOForum();
		dbo.setId(ID);
		dbo.setObjectId(OBJECT_ID);
		dbo.setObjectType(null);
		dbo.setEtag(UUID.randomUUID().toString());
		// From DBO to DTO - null objectType defaults to ENTITY
		Forum dto = ForumUtils.createDTOFromDBO(dbo);
		assertEquals(ForumObjectType.ENTITY, dto.getObjectType());
		assertEquals(KeyFactory.keyToString(OBJECT_ID), dto.getProjectId());
	}

	@Test
	public void testDTOTODBOWithNullObjectId() {
		assertThrows(IllegalArgumentException.class, () -> ForumUtils.createDBOFromDTO(
				new Forum().setId("123").setObjectId(null)));
	}

	@Test
	public void testDTOTODBOWithNullObjectType() {
		assertThrows(IllegalArgumentException.class, () -> ForumUtils.createDBOFromDTO(
				new Forum().setId("123").setObjectId("567").setObjectType(null)));
	}

	@Test
	public void testDTOTODBOWithNonNullProjectId() {
		assertThrows(IllegalArgumentException.class, () -> ForumUtils.createDBOFromDTO(
				new Forum().setId("123").setObjectId("567").setObjectType(ForumObjectType.ENTITY).setProjectId("567")));
	}
}
