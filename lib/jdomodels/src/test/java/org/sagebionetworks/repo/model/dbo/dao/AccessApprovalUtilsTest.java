package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
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

	@Test
	public void testCopyDtosToDbosForCreation() {
		IdGenerator mockIdGenerator = Mockito.mock(IdGenerator.class);
		AccessApproval dto = createDTO();
		when(mockIdGenerator.generateNewId(IdType.ACCESS_APPROVAL_ID)).thenReturn(1L);
		List<DBOAccessApproval> dbos = AccessApprovalUtils.copyDtosToDbos(Arrays.asList(dto), true, mockIdGenerator);
		assertNotNull(dbos);
		assertEquals(1, dbos.size());
		DBOAccessApproval dbo = dbos.get(0);
		AccessApproval newDto = AccessApprovalUtils.copyDboToDto(dbo);
		assertFalse(newDto.equals(dto));
		dto.setId(newDto.getId());
		dto.setEtag(newDto.getEtag());
		assertEquals(newDto, dto);
	}

	@Test
	public void testCopyDtosToDbosNotForCreation() {
		AccessApproval dto = createDTO();
		List<DBOAccessApproval> dbos = AccessApprovalUtils.copyDtosToDbos(Arrays.asList(dto), false, null);
		List<AccessApproval> dtos = AccessApprovalUtils.copyDbosToDtos(dbos);
		assertNotNull(dtos);
		assertEquals(1, dtos.size());
		assertEquals(dtos.get(0), dto);
	}
}
