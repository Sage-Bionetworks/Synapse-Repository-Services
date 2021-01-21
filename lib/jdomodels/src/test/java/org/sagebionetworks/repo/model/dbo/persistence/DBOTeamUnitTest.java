package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.dbo.dao.TeamUtils;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.util.TemporaryCode;

@TemporaryCode(author = "marco.marasca@sagebase.org", comment = "Can be removed after stack 341 migration")
public class DBOTeamUnitTest {

	@Test
	public void testMigrationWithNoIcon() {
		Team team = new Team();
		
		team.setId("1");
		team.setEtag("etag");
		team.setIcon(null);
		
		// Emulates a team coming from the prev version
		DBOTeam backup = new DBOTeam();
		
		TeamUtils.copyDtoToDbo(team, backup);
		backup.setProperties(TeamUtils.serialize(team));
		
		MigratableTableTranslation<DBOTeam, DBOTeam> translator = new DBOTeam().getTranslator();
		
		DBOTeam result = translator.createDatabaseObjectFromBackup(backup);
		
		assertNull(result.getIcon());
	}
	
	@Test
	public void testMigrationWithIcon() {
		Team team = new Team();
		
		team.setId("1");
		team.setEtag("etag");
		team.setIcon("123");
		
		// Emulates a team coming from the prev version
		DBOTeam backup = new DBOTeam();
		
		TeamUtils.copyDtoToDbo(team, backup);
		
		// In the prev version the icon was only in the serialzied form
		backup.setProperties(TeamUtils.serialize(team));
		backup.setIcon(null);
		
		MigratableTableTranslation<DBOTeam, DBOTeam> translator = new DBOTeam().getTranslator();
		
		DBOTeam result = translator.createDatabaseObjectFromBackup(backup);
		
		assertEquals(123L, result.getIcon());
	}
	
}
