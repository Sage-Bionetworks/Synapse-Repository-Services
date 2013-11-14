package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.MembershipInvtnSubmissionUtils;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOMembershipInvtnSubmissionTest {
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
		
	private List<Long> toDelete = null;
	private List<Long> teamToDelete = null;
	
	@After
	public void after() throws DatastoreException {
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOMembershipInvtnSubmission.class, params);
			}
		}
		if(dboBasicDao != null && teamToDelete != null){
			for(Long id: teamToDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOTeam.class, params);
			}
		}
	}
	
	@Before
	public void before(){
		toDelete = new LinkedList<Long>();
		teamToDelete = new LinkedList<Long>();
	}
	
	public static DBOMembershipInvtnSubmission newMembershipInvtnSubmission(
			IdGenerator idGenerator, 
			UserGroupDAO userGroupDAO,
			DBOBasicDao dboBasicDao) {
		DBOMembershipInvtnSubmission invitation = new DBOMembershipInvtnSubmission();
		invitation.setId(idGenerator.generateNewId());
		invitation.setCreatedOn(System.currentTimeMillis());
		invitation.setExpiresOn(System.currentTimeMillis());
		DBOTeam team = DBOTeamTest.newTeam(userGroupDAO);
		team = dboBasicDao.createNew(team);
		invitation.setTeamId(team.getId());
		invitation.setProperties((new String("abcdefg")).getBytes());
		assertNotNull(userGroupDAO);
		UserGroup bug = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false);
		assertNotNull(bug);
		invitation.setInviteeId(Long.parseLong(bug.getId()));
		return invitation;
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException, UnsupportedEncodingException {
		DBOMembershipInvtnSubmission invitation = newMembershipInvtnSubmission(idGenerator, userGroupDAO, dboBasicDao);
		// Make sure we can create it
		DBOMembershipInvtnSubmission clone = dboBasicDao.createNew(invitation);
		toDelete.add(invitation.getId());
		teamToDelete.add(invitation.getTeamId());
		assertNotNull(clone);
		assertEquals(invitation, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", invitation.getId());
		clone = dboBasicDao.getObjectByPrimaryKey(DBOMembershipInvtnSubmission.class, params);
		assertNotNull(clone);
		assertEquals(invitation, clone);
		
		// Make sure we can update it.
		clone.setProperties(new byte[] { (byte)1 });
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		
		// Get the clone back again
		params = new MapSqlParameterSource();
		params.addValue("id", clone.getId());
		DBOMembershipInvtnSubmission clone2 = dboBasicDao.getObjectByPrimaryKey(DBOMembershipInvtnSubmission.class, params);
		assertEquals(clone, clone2);
	}
	
	private static MembershipInvtnSubmission createMembershipInvtnSubmission() {
		//It's easiest to create a DBO object by first creating a DTO object and then converting it
		MembershipInvtnSubmission dto = new MembershipInvtnSubmission();
		dto.setId("101");
		dto.setCreatedOn(new Date());
		dto.setExpiresOn(null);
		dto.setInviteeId("987");
		dto.setTeamId("456");
		dto.setCreatedBy("123");
		dto.setMessage("foo");
		return dto;
	}

	@Test
	public void testTranslator() throws Exception {
		MembershipInvtnSubmission dto = createMembershipInvtnSubmission();
		DBOMembershipInvtnSubmission dbo = new DBOMembershipInvtnSubmission();
		MembershipInvtnSubmissionUtils.copyDtoToDbo(dto, dbo);
		// now do the round trip
		DBOMembershipInvtnSubmission backup = dbo.getTranslator().createBackupFromDatabaseObject(dbo);
		assertEquals(dbo, dbo.getTranslator().createDatabaseObjectFromBackup(backup));
		assertEquals(dto, MembershipInvtnSubmissionUtils.copyDboToDto(dbo));
	}

	@Test
	public void testTranslatorWithLegacyInput() throws Exception {
		MembershipInvtnSubmission dto = createMembershipInvtnSubmission();
		//It's easiest to create a DBO object by first creating a DTO object and then converting it
		LegacyMembershipInvtnSubmission legacyDto = new LegacyMembershipInvtnSubmission();
		legacyDto.setId(dto.getId());
		legacyDto.setCreatedOn(dto.getCreatedOn());
		legacyDto.setExpiresOn(dto.getExpiresOn());
		legacyDto.setInvitees(Collections.singletonList(dto.getInviteeId()));
		legacyDto.setTeamId(dto.getTeamId());
		legacyDto.setCreatedBy(dto.getCreatedBy());
		legacyDto.setMessage(dto.getMessage());
		DBOMembershipInvtnSubmission backup = new DBOMembershipInvtnSubmission();
		MembershipInvtnSubmissionUtils.copyDtoToDbo(dto, backup);
		// now we overwrite the serialized blob with that of the legacy object
		backup.setProperties(JDOSecondaryPropertyUtils.compressObject(legacyDto, "MembershipInvtnSubmission"));

		// see if we can convert it
		DBOMembershipInvtnSubmission dbo = backup.getTranslator().createDatabaseObjectFromBackup(backup);
		// the result should be the same as if we turned the original DTO into a DBO
		DBOMembershipInvtnSubmission expectedDBO = new DBOMembershipInvtnSubmission();
		MembershipInvtnSubmissionUtils.copyDtoToDbo(dto, expectedDBO);
		assertEquals(expectedDBO, dbo);
		assertEquals(dto, MembershipInvtnSubmissionUtils.copyDboToDto(dbo));
	}


}
