package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;

public class ResearchProjectUtilsTest {

	@Test
	public void testRoundTrip() {
		ResearchProject dto = ResearchProjectTestUtils.createNewDto();

		DBOResearchProject dbo = new DBOResearchProject();
		ResearchProject newDto = new ResearchProject();
		ResearchProjectUtils.copyDtoToDbo(dto, dbo);
		ResearchProjectUtils.copyDboToDto(dbo, newDto);
		assertEquals(dto, newDto);
	}

}
