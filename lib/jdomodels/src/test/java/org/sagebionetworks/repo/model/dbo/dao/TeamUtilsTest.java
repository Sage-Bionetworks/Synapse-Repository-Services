package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTeam;
import org.sagebionetworks.util.TemporaryCode;

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

	@TemporaryCode(author = "peter.harvey@sagebase.org", comment = "One time migration of property canRequestMembership.  Can be removed after all teams have a canRequestMembership value.")
	@Test
	public void testCreateDatabaseObjectFromBackupWithNullCanRequestMembership() {
		Team dto = new Team();
		dto.setId("123");
		dto.setEtag(UUID.randomUUID().toString());
		dto.setCanRequestMembership(null);

		DBOTeam dbo = new DBOTeam();
		TeamUtils.copyDtoToDbo(dto, dbo);

		dbo = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertEquals(true, TeamUtils.copyDboToDto(dbo).getCanRequestMembership());
	}

	@TemporaryCode(author = "peter.harvey@sagebase.org", comment = "One time migration of property canRequestMembership.  Can be removed after all teams have a canRequestMembership value.")
	@Test
	public void testCreateDatabaseObjectFromBackupWithFalseCanRequestMembership() {
		Team dto = new Team();
		dto.setId("123");
		dto.setEtag(UUID.randomUUID().toString());
		dto.setCanRequestMembership(false);

		DBOTeam dbo = new DBOTeam();
		TeamUtils.copyDtoToDbo(dto, dbo);

		dbo = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertEquals(false, TeamUtils.copyDboToDto(dbo).getCanRequestMembership());
	}

	@TemporaryCode(author = "peter.harvey@sagebase.org", comment = "One time migration of property canRequestMembership.  Can be removed after all teams have a canRequestMembership value.")
	@Test
	public void testCreateDatabaseObjectFromBackupWithTrueCanRequestMembership() {
		Team dto = new Team();
		dto.setId("123");
		dto.setEtag(UUID.randomUUID().toString());
		dto.setCanRequestMembership(true);

		DBOTeam dbo = new DBOTeam();
		TeamUtils.copyDtoToDbo(dto, dbo);

		dbo = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertEquals(true, TeamUtils.copyDboToDto(dbo).getCanRequestMembership());
	}
}
