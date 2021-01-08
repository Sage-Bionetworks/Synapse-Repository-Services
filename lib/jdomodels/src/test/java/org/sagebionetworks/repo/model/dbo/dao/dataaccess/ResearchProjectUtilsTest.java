package org.sagebionetworks.repo.model.dbo.dao.dataaccess;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;

public class ResearchProjectUtilsTest {

	@Test
	public void testRoundTrip() {
		ResearchProject dto = ResearchProjectTestUtils.createNewDto();
		dto.setId("101");
		DBOResearchProject dbo = new DBOResearchProject();
		ResearchProject newDto = new ResearchProject();
		ResearchProjectUtils.copyDtoToDbo(dto, dbo);
		ResearchProjectUtils.copyDboToDto(dbo, newDto);
		assertEquals(dto, newDto);
	}
	
	@Test
	public void testRoundTripWithNoIDU() {
		ResearchProject dto = ResearchProjectTestUtils.createNewDto();
		dto.setId("101");
		dto.setIntendedDataUseStatement(null);
		DBOResearchProject dbo = new DBOResearchProject();
		ResearchProject newDto = new ResearchProject();
		ResearchProjectUtils.copyDtoToDbo(dto, dbo);
		ResearchProjectUtils.copyDboToDto(dbo, newDto);
		assertEquals(dto, newDto);
	}

}
