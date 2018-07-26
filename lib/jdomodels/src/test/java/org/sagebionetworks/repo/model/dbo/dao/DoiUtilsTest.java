package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoi;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public class DoiUtilsTest {

	private static final Long createdBy = 1L;
	private static final Timestamp createdOn = new Timestamp((new Date()).getTime());
	private static final ObjectType objectType = ObjectType.ENTITY;
	private static final DoiStatus doiStatus = DoiStatus.CREATED;
	private static final String eTag = "eTag";
	private static final Long id = 2L;
	private static final Long objectId = 3L;
	private static final Long objectVersion = 4L;
	private static final Timestamp updatedOn = new Timestamp((new Date()).getTime());

	@Test
	public void testConvertToDto() {
		DBODoi dbo = setUpDbo();
		// Call under test
		Doi dto = DoiUtils.convertToDto(dbo);
		assertEquals(createdBy.toString(), dto.getCreatedBy());
		assertEquals(createdOn.getTime(), dto.getCreatedOn().getTime());
		assertEquals(objectType, dto.getObjectType());
		assertEquals(doiStatus, dto.getDoiStatus());
		assertEquals(eTag, dto.getEtag());
		assertEquals(id.toString(), dto.getId());
		assertEquals("syn" + objectId.toString(), dto.getObjectId());
		assertEquals(objectVersion, dto.getObjectVersion());
		assertEquals(updatedOn.getTime(), dto.getUpdatedOn().getTime());
	}

	@Test
	public void testConvertToDtoNotEntity() {
		DBODoi dbo = setUpDbo();
		dbo.setObjectType(ObjectType.WIKI);
		// Call under test
		Doi dto = DoiUtils.convertToDto(dbo);
		assertEquals(objectId.toString(), dto.getObjectId());
		assertEquals(ObjectType.WIKI, dto.getObjectType());
	}

	@Test
	public void testConvertToDtoNoVersion() {
		DBODoi dbo = setUpDbo();
		dbo.setObjectVersion(DBODoi.NULL_OBJECT_VERSION);
		//Call under test
		Doi dto = DoiUtils.convertToDto(dbo);
		assertNull(dto.getObjectVersion());
	}

	@Test
	public void testConvertToDbo() {
		Doi dto = setUpDto();
		// Call under test
		DBODoi dbo = DoiUtils.convertToDbo(dto);
		assertEquals(createdBy, dbo.getCreatedBy());
		assertEquals(createdOn.getTime(), dbo.getCreatedOn().getTime());
		assertEquals(objectType.name(), dbo.getObjectType());
		assertEquals(doiStatus.name(), dbo.getDoiStatus());
		assertEquals(eTag, dbo.getETag());
		assertEquals(id, dbo.getId());
		assertEquals(objectId, dbo.getObjectId());
		assertEquals(objectVersion, dbo.getObjectVersion());
		assertEquals(updatedOn.getTime(), dbo.getUpdatedOn().getTime());
	}

	@Test
	public void testConvertToDboNoVersion() {
		Doi dto = setUpDto();
		dto.setObjectVersion(null);
		// Call under test
		DBODoi dbo = DoiUtils.convertToDbo(dto);
		assertEquals((Long)DBODoi.NULL_OBJECT_VERSION, dbo.getObjectVersion());
	}

	private static DBODoi setUpDbo() {
		DBODoi dbo = new DBODoi();
		dbo.setCreatedBy(createdBy);
		dbo.setCreatedOn(createdOn);
		dbo.setObjectType(objectType);
		dbo.setDoiStatus(doiStatus);
		dbo.setETag(eTag);
		dbo.setId(id);
		dbo.setObjectId(objectId);
		dbo.setObjectVersion(objectVersion);
		dbo.setUpdatedOn(updatedOn);
		return dbo;
	}

	private static Doi setUpDto() {
		Doi dto = new Doi();
		dto.setCreatedBy(createdBy.toString());
		dto.setCreatedOn(createdOn);
		dto.setObjectType(objectType);
		dto.setDoiStatus(doiStatus);
		dto.setEtag(eTag);
		dto.setId(id.toString());
		dto.setObjectId(objectId.toString());
		dto.setObjectVersion(objectVersion);
		dto.setUpdatedOn(updatedOn);
		return dto;
	}
}
