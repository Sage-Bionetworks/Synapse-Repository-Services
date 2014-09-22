package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;

public class AccessRequirementUtilsTest {
	
	public static RestrictableObjectDescriptor createRestrictableObjectDescriptor(String id) {
		return createRestrictableObjectDescriptor(id, RestrictableObjectType.ENTITY);
	}
	
	public static RestrictableObjectDescriptor createRestrictableObjectDescriptor(String id, RestrictableObjectType type) {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(id);
		rod.setType(type);
		return rod;
	}

	private static AccessRequirement createDTO() {
		TermsOfUseAccessRequirement dto = new TermsOfUseAccessRequirement();
		dto.setId(101L);
		dto.setEtag("0");
		dto.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{createRestrictableObjectDescriptor("syn999")}));
		dto.setCreatedBy("555");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("666");
		dto.setModifiedOn(new Date());
		dto.setConcreteType("org.sagebionetworks.repo.model.TermsOfUseAcessRequirement");
		dto.setAccessType(ACCESS_TYPE.DOWNLOAD);	
		dto.setTermsOfUse("foo");
		return dto;
	}
	
	@Test
	public void testRoundtrip() throws Exception {
		AccessRequirement dto = createDTO();
			
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
		List<RestrictableObjectDescriptor> nodeIds = new ArrayList<RestrictableObjectDescriptor>();
		for (RestrictableObjectDescriptor s : dto.getSubjectIds()) nodeIds.add(s);
		AccessRequirement dto2 = AccessRequirementUtils.copyDboToDto(dbo, nodeIds);
		assertEquals(dto, dto2);
	}

	@Test
	public void testRoundtripWithNulls() throws Exception {
		AccessRequirement dto = createDTO();
		dto.setId(null);
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
		List<RestrictableObjectDescriptor> nodeIds = new ArrayList<RestrictableObjectDescriptor>();
		for (RestrictableObjectDescriptor s : dto.getSubjectIds()) nodeIds.add(s);
		AccessRequirement dto2 = AccessRequirementUtils.copyDboToDto(dbo, nodeIds);
		assertEquals(dto, dto2);
	}

}
