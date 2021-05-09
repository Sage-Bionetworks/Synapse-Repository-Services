package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTeam;

public class TeamUtilsTest {

	@Test
	public void testRoundTrip() {
		Team dto = new Team();
		
		dto.setId("123");
		dto.setEtag("etag");
		dto.setIcon("456");
		dto.setCreatedOn(new Date());
		dto.setCanPublicJoin(false);
		dto.setCreatedBy("123");
		dto.setModifiedBy("123");
		dto.setModifiedOn(new Date());
		dto.setDescription("Some description");
		dto.setName("Some team");
		
		DBOTeam dbo = new DBOTeam();
		
		// This should not have any effect
		dbo.setId(123456L);
		dbo.setEtag("someOther");
		dbo.setIcon(213L);
		
		TeamUtils.copyDtoToDbo(dto, dbo);
		assertEquals(dto, TeamUtils.copyDboToDto(dbo));
		
	}
}
