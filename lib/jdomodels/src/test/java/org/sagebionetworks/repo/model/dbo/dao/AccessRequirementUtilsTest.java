package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class AccessRequirementUtilsTest {

	private static AccessRequirement createDTO() {
		AccessRequirement dto = new AccessRequirement();
		dto.setId(101L);
		dto.setEtag("0");
		dto.setEntityId("syn999");
		dto.setCreatedBy("555");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("666");
		dto.setModifiedOn(new Date());
		dto.setAccessRequirementType(AccessRequirementType.TOU_Agreement);
		dto.setAccessType(ACCESS_TYPE.DOWNLOAD);	
		return dto;
	}
	
	@Test
	public void testRoundtrip() throws Exception {
		AccessRequirement dto = createDTO();
			
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		String jsonString = (String) AccessRequirement.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
		AccessRequirement dto2 = new AccessRequirement();
		AccessRequirementUtils.copyDboToDto(dbo, dto2);
		assertEquals(dto, dto2);
	}

	@Test(expected = NumberFormatException.class)
	public void testInvalidEtag() throws Exception {
		AccessRequirement dto = createDTO();
		dto.setEtag("not a number");
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		String jsonString = (String) AccessRequirement.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
	}

	@Test
	public void testRoundtripWithNulls() throws Exception {
		AccessRequirement dto = createDTO();
		dto.setId(null);
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		String jsonString = (String) AccessRequirement.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
		AccessRequirement dto2 = new AccessRequirement();
		AccessRequirementUtils.copyDboToDto(dbo, dto2);
		assertEquals(dto, dto2);
	}



}
