package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamState;
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

	@Test
	public void testCopyDtoToDbo() {
		Team dto = new Team();
		dto.setId("123");
		dto.setEtag("etag");
		dto.setCreatedOn(new Date());
		dto.setCanPublicJoin(false);
		dto.setCanRequestMembership(true);

		DBOTeam dbo = new DBOTeam();

		// call under test
		TeamUtils.copyDtoToDbo(dto, dbo);

		assertEquals(dbo.getState(), TeamState.OPEN.name());
	}

	@Test
	public void testCopyDboToDtoWithNoMembershipStatusInDbo() {
		Team dto = new Team();
		dto.setId("123");
		dto.setEtag("etag");
		dto.setCreatedOn(new Date());

		DBOTeam dbo = new DBOTeam();
		TeamUtils.copyDtoToDbo(dto, dbo);
		assertEquals(dbo.getState(), TeamState.OPEN.name());

		//call under test
		Team result = TeamUtils.copyDboToDto(dbo);
		assertFalse(result.getCanPublicJoin());
		assertTrue(result.getCanRequestMembership());
	}

	@Test
	public void testCopyDboToDtoWithMembershipStatusInDbo() {
		Team dto = new Team();
		dto.setId("123");
		dto.setEtag("etag");
		dto.setCreatedOn(new Date());
		dto.setCanPublicJoin(true);
		dto.setCanRequestMembership(false);

		DBOTeam dbo = new DBOTeam();
		TeamUtils.copyDtoToDbo(dto, dbo);
		assertEquals(dbo.getState(), TeamState.PUBLIC.name());

		//call under test
		Team result = TeamUtils.copyDboToDto(dbo);
		assertTrue(result.getCanPublicJoin());
		assertFalse(result.getCanRequestMembership());
	}

	@Test
	public void testSetMembershipStatusForPUBLICState() {
		Team team = new Team();
		team.setId("123");
		team.setEtag("etag");

		DBOTeam dbo = new DBOTeam();
		dbo.setId(123L);
		dbo.setEtag("etag");
		dbo.setState(TeamState.PUBLIC.name());

		//call under test
		TeamUtils.setMembershipStatus(dbo.getState(), team);
		assertTrue(team.getCanPublicJoin());
		assertFalse(team.getCanRequestMembership());
	}

	@Test
	public void testSetMembershipStatusForOPENState() {
		Team team = new Team();
		team.setId("123");
		team.setEtag("etag");

		DBOTeam dbo = new DBOTeam();
		dbo.setId(123L);
		dbo.setEtag("etag");
		dbo.setState(TeamState.OPEN.name());

		//call under test
		TeamUtils.setMembershipStatus(dbo.getState(), team);
		assertFalse(team.getCanPublicJoin());
		assertTrue(team.getCanRequestMembership());
	}

	@Test
	public void testSetMembershipStatusForCLOSEDState() {
		Team team = new Team();
		team.setId("123");
		team.setEtag("etag");

		DBOTeam dbo = new DBOTeam();
		dbo.setId(123L);
		dbo.setEtag("etag");
		dbo.setState(TeamState.CLOSED.name());

		//call under test
		TeamUtils.setMembershipStatus(dbo.getState(), team);
		assertFalse(team.getCanPublicJoin());
		assertFalse(team.getCanRequestMembership());
	}
}
