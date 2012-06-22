package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApproval;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class AccessApprovalUtilsTest {

	private static AccessApproval createDTO() {
		AccessApproval dto = new AccessApproval();
		dto.setId(101L);
		dto.setEtag("0");
		dto.setCreatedBy("555");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("666");
		dto.setModifiedOn(new Date());	
		dto.setAccessorId("777");
		dto.setApprovalType(AccessApprovalType.TOU_Agreement);
		dto.setRequirementId(888L);
		
		return dto;
	}
	
	@Test
	public void testRoundtrip() throws Exception {
		AccessApproval dto = createDTO();
			
		DBOAccessApproval dbo = new DBOAccessApproval();
		String jsonString = (String) AccessApproval.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		AccessApprovalUtils.copyDtoToDbo(dto, dbo);
		AccessApproval dto2 = new AccessApproval();
		AccessApprovalUtils.copyDboToDto(dbo, dto2);
		assertEquals(dto, dto2);
	}

	@Test(expected = NumberFormatException.class)
	public void testInvalidEtag() throws Exception {
		AccessApproval dto = createDTO();
		dto.setEtag("not a number");
		DBOAccessApproval dbo = new DBOAccessApproval();
		String jsonString = (String) AccessApproval.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		AccessApprovalUtils.copyDtoToDbo(dto, dbo);
	}

	@Test
	public void testRoundtripWithNulls() throws Exception {
		AccessApproval dto = createDTO();
		dto.setId(null);
		DBOAccessApproval dbo = new DBOAccessApproval();
		String jsonString = (String) AccessApproval.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		AccessApprovalUtils.copyDtoToDbo(dto, dbo);
		AccessApproval dto2 = new AccessApproval();
		AccessApprovalUtils.copyDboToDto(dbo, dto2);
		assertEquals(dto, dto2);
	}



}
