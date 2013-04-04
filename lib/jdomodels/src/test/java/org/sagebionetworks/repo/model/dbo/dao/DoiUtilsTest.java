package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoi;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public class DoiUtilsTest {

	@Test
	public void testConvertToDto() {
		final Long createdBy = 1L;
		final Timestamp createdOn = new Timestamp((new Date()).getTime());
		final DoiObjectType doiObjectType = DoiObjectType.ENTITY;
		final DoiStatus doiStatus = DoiStatus.READY;
		final String eTag = "eTag";
		final Long id = 2L;
		final Long objectId = 3L;
		final Long objectVersion = 4L;
		final Timestamp updatedOn = new Timestamp((new Date()).getTime());
		DBODoi dbo = new DBODoi();
		dbo.setCreatedBy(createdBy);
		dbo.setCreatedOn(createdOn);
		dbo.setDoiObjectType(doiObjectType);
		dbo.setDoiStatus(doiStatus);
		dbo.setETag(eTag);
		dbo.setId(id);
		dbo.setObjectId(objectId);
		dbo.setObjectVersion(objectVersion);
		dbo.setUpdatedOn(updatedOn);
		Doi dto = DoiUtils.convertToDto(dbo);
		assertEquals(createdBy.toString(), dto.getCreatedBy());
		assertEquals(createdOn.getTime(), dto.getCreatedOn().getTime());
		assertEquals(doiObjectType, dto.getDoiObjectType());
		assertEquals(doiStatus, dto.getDoiStatus());
		assertEquals(eTag, dto.getEtag());
		assertEquals(id.toString(), dto.getId());
		assertEquals(objectId, KeyFactory.stringToKey(dto.getObjectId()));
		assertEquals(objectVersion, dto.getObjectVersion());
		assertEquals(updatedOn.getTime(), dto.getUpdatedOn().getTime());
	}

	@Test
	public void testConvertToDbo() {
		final Long createdBy = 1L;
		final Timestamp createdOn = new Timestamp((new Date()).getTime());
		final DoiObjectType doiObjectType = DoiObjectType.ENTITY;
		final DoiStatus doiStatus = DoiStatus.READY;
		final String eTag = "eTag";
		final Long id = 2L;
		final Long objectId = 3L;
		final Long objectVersion = 4L;
		final Timestamp updatedOn = new Timestamp((new Date()).getTime());
		Doi dto = new Doi();
		dto.setCreatedBy(createdBy.toString());
		dto.setCreatedOn(createdOn);
		dto.setDoiObjectType(doiObjectType);
		dto.setDoiStatus(doiStatus);
		dto.setEtag(eTag);
		dto.setId(id.toString());
		dto.setObjectId(objectId.toString());
		dto.setObjectVersion(objectVersion);
		dto.setUpdatedOn(updatedOn);
		DBODoi dbo = DoiUtils.convertToDbo(dto);
		assertEquals(createdBy, dbo.getCreatedBy());
		assertEquals(createdOn.getTime(), dbo.getCreatedOn().getTime());
		assertEquals(doiObjectType.name(), dbo.getDoiObjectType());
		assertEquals(doiStatus.name(), dbo.getDoiStatus());
		assertEquals(eTag, dbo.getETag());
		assertEquals(id, dbo.getId());
		assertEquals(objectId, dbo.getObjectId());
		assertEquals(objectVersion, dbo.getObjectVersion());
		assertEquals(updatedOn.getTime(), dbo.getUpdatedOn().getTime());
	}
}
