package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

import com.amazonaws.services.sqs.model.Message;

public class PrincipalObjectRecordWriterTest {
	
	private PrincipalObjectRecordWriter writer;
	private UserProfileDAO mockUserProfileDAO;
	private UserGroupDAO mockUserGroupDAO;
	private TeamDAO mockTeamDAO;
	private ObjectRecordDAO mockObjectRecordDao;
	
	private Long principalID = 123L;
	private Date createdOn = new Date();
	private Date modifiedOn = new Date();
	private String etag = "etag";
	private String teamId = "111";
	private Long timestamp = System.currentTimeMillis();
	
	UserGroup ug;
	Team team;
	UserProfile up;
	
	@Before
	public void setup() {
		mockUserGroupDAO = Mockito.mock(UserGroupDAO.class);
		mockUserProfileDAO = Mockito.mock(UserProfileDAO.class);
		mockTeamDAO = Mockito.mock(TeamDAO.class);
		mockObjectRecordDao = Mockito.mock(ObjectRecordDAO.class);
		writer = new PrincipalObjectRecordWriter(mockUserGroupDAO, mockUserProfileDAO,
				mockTeamDAO, mockObjectRecordDao);	

		ug = new UserGroup();

		team = buildTeam(teamId);

		buildUserProfile();	
	}

	private void buildUserProfile() {
		up = new UserProfile();
		up.setCompany("Sage");
		up.setEmail("employee@sagebase.org");
		up.setEmails(Arrays.asList("employee@sagebase.org", "employee@gmail.com"));
		up.setEtag(etag);
		up.setOwnerId(principalID.toString());
	}

	private Team buildTeam(String teamId) {
		Team team = new Team();
		team.setCanPublicJoin(true);
		team.setCreatedBy("333");
		team.setCreatedOn(createdOn);
		team.setDescription("test Team");
		team.setEtag(etag);
		team.setId(teamId);
		team.setModifiedBy("444");
		team.setModifiedOn(modifiedOn);
		return team;
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void nonPrincipalChangeMessage() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACTIVITY, "1", "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecord(changeMessage);
	}

	@Test
	public void createTeamTest() throws IOException {
		ug.setIsIndividual(false);
		Mockito.when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		Mockito.when(mockTeamDAO.get(principalID.toString())).thenReturn(team);
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecord(changeMessage);
		Mockito.verify(mockUserGroupDAO).get(principalID);
		Mockito.verify(mockUserProfileDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO).get(principalID.toString());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void deleteTeamTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecord(changeMessage);
	}

	@Test
	public void updateTeamTest() throws IOException {
		ug.setIsIndividual(false);
		Mockito.when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		Mockito.when(mockTeamDAO.get(principalID.toString())).thenReturn(team);
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecord(changeMessage);
		Mockito.verify(mockUserGroupDAO).get(principalID);
		Mockito.verify(mockUserProfileDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO).get(principalID.toString());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
	}

	@Test
	public void createUserProfileTest() throws IOException {
		ug.setIsIndividual(true);
		Mockito.when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		Mockito.when(mockUserProfileDAO.get(principalID.toString())).thenReturn(up);
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		ObjectRecord ugr = ObjectRecordBuilderUtils.buildObjectRecord(ug, timestamp);
		ObjectRecord upr = ObjectRecordBuilderUtils.buildObjectRecord(up, timestamp);
		writer.buildAndWriteRecord(changeMessage);
		Mockito.verify(mockUserGroupDAO).get(principalID);
		Mockito.verify(mockUserProfileDAO).get(principalID.toString());
		Mockito.verify(mockTeamDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
		Mockito.verify(mockObjectRecordDao).saveBatch(Mockito.eq(Arrays.asList(ugr)), Mockito.eq(ugr.getJsonClassName()));
		Mockito.verify(mockObjectRecordDao).saveBatch(Mockito.eq(Arrays.asList(upr)), Mockito.eq(upr.getJsonClassName()));
	}

	@Test
	public void updateUserProfileTest() throws IOException {
		ug.setIsIndividual(true);
		Mockito.when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		Mockito.when(mockUserProfileDAO.get(principalID.toString())).thenReturn(up);
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecord(changeMessage);
		Mockito.verify(mockUserGroupDAO).get(principalID);
		Mockito.verify(mockUserProfileDAO).get(principalID.toString());
		Mockito.verify(mockTeamDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
	}

	@Test (expected=IllegalArgumentException.class)
	public void deleteUserProfileTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecord(changeMessage);
	}

	@Test
	public void logTeamTest() throws IOException {
		// principal that does not belong to any team
		Mockito.when(mockTeamDAO.getCountForMember(Mockito.anyString())).thenReturn(0L);
		writer.logTeam(principalID, 1, timestamp);
		Mockito.verify(mockTeamDAO, Mockito.never()).getForMemberInRange(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
		Mockito.verify(mockObjectRecordDao, Mockito.never()).saveBatch(Mockito.anyList(), Mockito.anyString());
		
		// principal that belongs to 2 team
		Mockito.when(mockTeamDAO.getCountForMember(Mockito.anyString())).thenReturn(4L);
		List<Team> list = createListOfTeam(2);
		Mockito.when(mockTeamDAO.getForMemberInRange(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(list);
		TeamMember teamMember = new TeamMember();
		Mockito.when(mockTeamDAO.getMember(Mockito.anyString(), Mockito.anyString())).thenReturn(teamMember);
		writer.logTeam(principalID, 2, timestamp);
		Mockito.verify(mockTeamDAO, Mockito.times(2)).getForMemberInRange(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong());
		Mockito.verify(mockTeamDAO, Mockito.times(4)).getMember(Mockito.anyString(), Mockito.anyString());
		Mockito.verify(mockObjectRecordDao, Mockito.times(2)).saveBatch(Mockito.anyList(), Mockito.anyString());
	}

	/**
	 * create a list of teams
	 * @param numberOfTeams
	 * @return
	 */
	private List<Team> createListOfTeam(int numberOfTeams) {
		List<Team> list = new ArrayList<Team>();
		for (int i = 0; i < numberOfTeams; i++) {
			list.add(buildTeam(String.valueOf(i)));
		}
		return list;
	}
}
