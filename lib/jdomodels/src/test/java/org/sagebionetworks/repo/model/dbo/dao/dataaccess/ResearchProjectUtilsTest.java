package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;

public class ResearchProjectUtilsTest {

	@Test
	public void testRoundTrip() {
		ResearchProject dto = new ResearchProject();
		dto.setId("1");
		dto.setAccessRequirementId("2");
		dto.setCreatedBy("3");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("4");
		dto.setModifiedOn(new Date());
		dto.setOwnerId("5");
		dto.setEtag("etag");
		dto.setProjectLead("projectLead");
		dto.setInstitution("institution");
		dto.setIntendedDataUseStatement("intendedDataUseStatement");

		DBOResearchProject dbo = new DBOResearchProject();
		ResearchProject newDto = new ResearchProject();
		ResearchProjectUtils.copyDtoToDbo(dto, dbo);
		ResearchProjectUtils.copyDboToDto(dbo, newDto);
		assertEquals(dto, newDto);
	}

}
