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
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;

public class DoiUtilsTest {

	private static final Long createdBy = 1L;
	private static final Long updatedBy = 7L;
	private static final Timestamp createdOn = new Timestamp((new Date()).getTime());
	private static final ObjectType objectType = ObjectType.ENTITY;
	private static final DoiStatus doiStatus = DoiStatus.CREATED;
	private static final String eTag = "eTag";
	private static final Long id = 2L;
	private static final Long objectId = 3L;
	private static final Long objectVersion = 4L;
	private static final Timestamp updatedOn = new Timestamp((new Date()).getTime());

	@Deprecated
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
	public void testConvertToDtoV2() {
		DBODoi dbo = setUpDbo();
		// Call under test
		DoiAssociation dto = DoiUtils.convertToDtoV2(dbo);
		assertEquals(createdBy.toString(), dto.getAssociatedBy());
		assertEquals(createdOn.getTime(), dto.getAssociatedOn().getTime());
		assertEquals(updatedBy.toString(), dto.getUpdatedBy());
		assertEquals(objectType, dto.getObjectType());
		assertEquals(doiStatus, doiStatus);
		assertEquals(eTag, dto.getEtag());
		assertEquals(id.toString(), dto.getAssociationId());
		assertEquals("syn" + objectId.toString(), dto.getObjectId());
		assertEquals(objectVersion, dto.getObjectVersion());
		assertEquals(updatedOn.getTime(), dto.getUpdatedOn().getTime());
	}

	@Deprecated
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
	public void testConvertToDtoV2NotEntity() {
		DBODoi dbo = setUpDbo();
		dbo.setObjectType(ObjectType.WIKI);
		// Call under test
		DoiAssociation dto = DoiUtils.convertToDtoV2(dbo);
		assertEquals(objectId.toString(), dto.getObjectId());
		assertEquals(ObjectType.WIKI, dto.getObjectType());
	}

	@Deprecated
	@Test
	public void testConvertToDtoNoVersion() {
		DBODoi dbo = setUpDbo();
		dbo.setObjectVersion(DBODoi.NULL_OBJECT_VERSION);
		//Call under test
		Doi dto = DoiUtils.convertToDto(dbo);
		assertNull(dto.getObjectVersion());
	}

	@Test
	public void testConvertToDtoV2NoVersion() {
		DBODoi dbo = setUpDbo();
		dbo.setObjectVersion(DBODoi.NULL_OBJECT_VERSION);
		//Call under test
		DoiAssociation dto = DoiUtils.convertToDtoV2(dbo);
		assertNull(dto.getObjectVersion());
	}

	@Deprecated
	@Test
	public void testConvertToDbo() {
		Doi dto = setUpDto();
		// Call under test
		DBODoi dbo = DoiUtils.convertToDbo(dto);
		assertEquals(createdBy, dbo.getCreatedBy());
		// NOTE that the old DTO does not support updatedBy, so it will always match createdBy in the DBO
		assertEquals(createdBy, dbo.getUpdatedBy());
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
	public void testConvertV2ToDbo() {
		DoiAssociation dto = setUpDtoV2();
		// Call under test
		DBODoi dbo = DoiUtils.convertToDbo(dto);
		assertEquals(createdBy, dbo.getCreatedBy());
		assertEquals(updatedBy, dbo.getUpdatedBy());
		assertEquals(createdOn.getTime(), dbo.getCreatedOn().getTime());
		assertEquals(objectType.name(), dbo.getObjectType());
		// NOTE the new DTO does not support DoiStatus, so it will always be 'READY' in the DBO
		assertEquals(DoiStatus.READY.name(), dbo.getDoiStatus());
		assertEquals(eTag, dbo.getETag());
		assertEquals(id, dbo.getId());
		assertEquals(objectId, dbo.getObjectId());
		assertEquals(objectVersion, dbo.getObjectVersion());
		assertEquals(updatedOn.getTime(), dbo.getUpdatedOn().getTime());
	}


	@Deprecated
	@Test
	public void testConvertToDboNotEntity() {
		Doi dto = setUpDto();
		dto.setId("3");
		dto.setObjectType(ObjectType.WIKI);
		// Call under test
		DBODoi dbo = DoiUtils.convertToDbo(dto);
		assertEquals(dto.getId(), dbo.getId().toString());
	}

	@Deprecated
	@Test
	public void testConvertToDboNoVersion() {
		Doi dto = setUpDto();
		dto.setObjectVersion(null);
		// Call under test
		DBODoi dbo = DoiUtils.convertToDbo(dto);
		assertEquals((Long)DBODoi.NULL_OBJECT_VERSION, dbo.getObjectVersion());
	}

	@Test
	public void testConvertV2ToDboNoVersion() {
		DoiAssociation dto = setUpDtoV2();
		dto.setObjectVersion(null);
		// Call under test
		DBODoi dbo = DoiUtils.convertToDbo(dto);
		assertEquals((Long)DBODoi.NULL_OBJECT_VERSION, dbo.getObjectVersion());
	}

	@Deprecated
	@Test(expected = IllegalArgumentException.class)
	public void testConvertToDboNullDto() {
		Doi dto = null;
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Deprecated
	@Test(expected = IllegalArgumentException.class)
	public void testConvertToDboNullId() {
		Doi dto = setUpDto();
		 dto.setId(null); // Omit required field
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Deprecated
	@Test(expected = IllegalArgumentException.class)
	public void testConvertToDboNullCreatedBy() {
		Doi dto = setUpDto();
		dto.setCreatedBy(null); // Omit required field.
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Deprecated
	@Test(expected = IllegalArgumentException.class)
	public void testConvertToDboNullEtag() {
		Doi dto = setUpDto();
		dto.setEtag(null); // Omit required field.
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Deprecated
	@Test(expected = IllegalArgumentException.class)
	public void testConvertToDboNullStatus() {
		Doi dto = setUpDto();
		dto.setDoiStatus(null); // Omit required field.
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Deprecated
	@Test(expected = IllegalArgumentException.class)
	public void testConvertToDboNullObjectId() {
		Doi dto = setUpDto();
		dto.setObjectId(null); // Omit required field.
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Deprecated
	@Test(expected = IllegalArgumentException.class)
	public void testConvertToDboNullObjectType() {
		Doi dto = setUpDto();
		dto.setObjectType(null); // Omit required field.
		DoiUtils.convertToDbo(dto);
	}

	@Deprecated
	@Test(expected = IllegalArgumentException.class)
	public void testConvertToDboNullCreatedOn() {
		Doi dto = setUpDto();
		dto.setCreatedOn(null); // Omit required field.
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Deprecated
	@Test(expected = IllegalArgumentException.class)
	public void testConvertToDboNullUpdatedOn() {
		Doi dto = setUpDto();
		dto.setUpdatedOn(null); // Omit required field.
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Deprecated
	@Test(expected = IllegalArgumentException.class)
	public void testConvertToDtoNullDbo() {
		DBODoi dbo = null;
		// Note DBO is null, so it should not be converted to a DTO.
		// Call under test.
		DoiUtils.convertToDto(dbo);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertToDtoV2NullDbo() {
		DBODoi dbo = null;
		// Note DBO is null, so it should not be converted to a DTO.
		// Call under test.
		DoiUtils.convertToDtoV2(dbo);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertV2ToDboNullDto() {
		DoiAssociation dto = null;
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertV2ToDboNullId() {
		DoiAssociation dto = setUpDtoV2();
		dto.setAssociationId(null); // Omit required field
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertV2ToDboNullAssociatedBy() {
		DoiAssociation dto = setUpDtoV2();
		dto.setAssociatedBy(null); // Omit required field.
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertV2ToDboNullAssociatedOn() {
		DoiAssociation dto = setUpDtoV2();
		dto.setAssociatedOn(null); // Omit required field.
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertV2ToDboNullEtag() {
		DoiAssociation dto = setUpDtoV2();
		dto.setEtag(null); // Omit required field.
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertV2ToDboNullObjectId() {
		DoiAssociation dto = setUpDtoV2();
		dto.setObjectId(null); // Omit required field.
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertV2ToDboNullObjectType() {
		DoiAssociation dto = setUpDtoV2();
		dto.setObjectType(null); // Omit required field.
		DoiUtils.convertToDbo(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertV2ToDboNullUpdatedBy() {
		DoiAssociation dto = setUpDtoV2();
		dto.setUpdatedBy(null); // Omit required field.
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertV2ToDboNullUpdatedOn() {
		DoiAssociation dto = setUpDtoV2();
		dto.setUpdatedOn(null); // Omit required field.
		// Call under test
		DoiUtils.convertToDbo(dto);
	}

	private static DBODoi setUpDbo() {
		DBODoi dbo = new DBODoi();
		dbo.setCreatedBy(createdBy);
		dbo.setUpdatedBy(updatedBy);
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

	@Deprecated
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

	private static DoiAssociation setUpDtoV2() {
		DoiAssociation dto = new DoiAssociation();
		dto.setAssociatedBy(createdBy.toString());
		dto.setAssociatedOn(createdOn);
		dto.setObjectType(objectType);
		dto.setEtag(eTag);
		dto.setAssociationId(id.toString());
		dto.setObjectId(objectId.toString());
		dto.setObjectVersion(objectVersion);
		dto.setUpdatedBy(updatedBy.toString());
		dto.setUpdatedOn(updatedOn);
		return dto;
	}

}
