package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class AccessRequirementUtilsTest {

	private static AccessRequirement createDTO() {
		TermsOfUseAccessRequirement dto = new TermsOfUseAccessRequirement();
		dto.setId(101L);
		dto.setEtag("0");
		dto.setEntityIds(Arrays.asList(new String[]{"syn999"}));
		dto.setCreatedBy("555");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("666");
		dto.setModifiedOn(new Date());
		dto.setEntityType("org.sagebionetworks.repo.model.TermsOfUseAcessRequirement");
		dto.setAccessType(ACCESS_TYPE.DOWNLOAD);	
		dto.setTermsOfUse("foo");
		return dto;
	}
	
	@Test
	public void testRoundtrip() throws Exception {
		AccessRequirement dto = createDTO();
			
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
		List<Long> nodeIds = new ArrayList<Long>();
		for (String s : dto.getEntityIds()) nodeIds.add(KeyFactory.stringToKey(s));
		AccessRequirement dto2 = AccessRequirementUtils.copyDboToDto(dbo, nodeIds);
		assertEquals(dto, dto2);
	}

	@Test(expected = NumberFormatException.class)
	public void testInvalidEtag() throws Exception {
		AccessRequirement dto = createDTO();
		dto.setEtag("not a number");
		DBOAccessRequirement dbo = new DBOAccessRequirement();
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
		List<Long> nodeIds = new ArrayList<Long>();
		for (String s : dto.getEntityIds()) nodeIds.add(KeyFactory.stringToKey(s));
		AccessRequirement dto2 = AccessRequirementUtils.copyDboToDto(dbo, nodeIds);
		assertEquals(dto, dto2);
	}



}
