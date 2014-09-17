package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApproval;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class AccessApprovalUtilsTest {

	private static AccessApproval createDTO() {
		TermsOfUseAccessApproval dto = new TermsOfUseAccessApproval();
		dto.setId(101L);
		dto.setEtag("0");
		dto.setCreatedBy("555");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("666");
		dto.setModifiedOn(new Date());	
		dto.setAccessorId("777");
		dto.setConcreteType("org.sagebionetworks.repo.model.TermsOfUseAccessApproval");
		dto.setRequirementId(888L);
		return dto;
	}
	
	@Test
	public void testRoundtrip() throws Exception {
		AccessApproval dto = createDTO();
			
		DBOAccessApproval dbo = new DBOAccessApproval();
		String jsonString = (String) AccessApproval.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		AccessApprovalUtils.copyDtoToDbo(dto, dbo);
		AccessApproval dto2 =  AccessApprovalUtils.copyDboToDto(dbo);
		assertEquals(dto, dto2);
	}

	@Test
	public void testRoundtripWithNulls() throws Exception {
		AccessApproval dto = createDTO();
		dto.setId(null);
		DBOAccessApproval dbo = new DBOAccessApproval();
		String jsonString = (String) AccessApproval.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		AccessApprovalUtils.copyDtoToDbo(dto, dbo);
		AccessApproval dto2 =  AccessApprovalUtils.copyDboToDto(dbo);
		assertEquals(dto, dto2);
	}
}
