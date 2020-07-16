package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApproval;

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
		dto.setRequirementId(888L);
		dto.setRequirementVersion(3L);
		dto.setSubmitterId("555");
		dto.setState(ApprovalState.APPROVED);
		return dto;
	}
	
	@Test
	public void testRoundtrip() throws Exception {
		AccessApproval dto = createDTO();
		DBOAccessApproval dbo = new DBOAccessApproval();
		AccessApprovalUtils.copyDtoToDbo(dto, dbo);
		AccessApproval dto2 =  AccessApprovalUtils.copyDboToDto(dbo);
		assertEquals(dto, dto2);
	}

	@Test
	public void testRoundtripWithNulls() throws Exception {
		AccessApproval dto = createDTO();
		dto.setId(null);
		DBOAccessApproval dbo = new DBOAccessApproval();
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
