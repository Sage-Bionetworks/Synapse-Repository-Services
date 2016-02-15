package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

import com.amazonaws.services.sqs.model.Message;

public class PrincipalObjectRecordWriterTest {
	
	private PrincipalObjectRecordWriter writer;
	@Mock
	private UserProfileManager mockUserProfileManager;
	@Mock
	private UserGroupDAO mockUserGroupDAO;
	@Mock
	private TeamDAO mockTeamDAO;
	@Mock
	private ObjectRecordDAO mockObjectRecordDao;
	@Mock
	private GroupMembersDAO mockGroupMembersDao;
	
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
		MockitoAnnotations.initMocks(this);

		writer = new PrincipalObjectRecordWriter(mockUserGroupDAO, mockUserProfileManager,
				mockTeamDAO, mockGroupMembersDao, mockObjectRecordDao);	

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
		Mockito.verify(mockUserProfileManager, Mockito.never()).getUserProfile(Mockito.anyString());
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
		Mockito.verify(mockUserProfileManager, Mockito.never()).getUserProfile(Mockito.anyString());
		Mockito.verify(mockTeamDAO).get(principalID.toString());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
	}

	@Test
	public void createUserProfileTest() throws IOException {
		ug.setIsIndividual(true);
		Mockito.when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		Mockito.when(mockUserProfileManager.getUserProfile(principalID.toString())).thenReturn(up);
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		ObjectRecord ugr = ObjectRecordBuilderUtils.buildObjectRecord(ug, timestamp);
		ObjectRecord upr = ObjectRecordBuilderUtils.buildObjectRecord(up, timestamp);
		writer.buildAndWriteRecord(changeMessage);
		Mockito.verify(mockUserGroupDAO).get(principalID);
		Mockito.verify(mockUserProfileManager).getUserProfile(principalID.toString());
		Mockito.verify(mockTeamDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
		Mockito.verify(mockObjectRecordDao).saveBatch(Mockito.eq(Arrays.asList(ugr)), Mockito.eq(ugr.getJsonClassName()));
		Mockito.verify(mockObjectRecordDao).saveBatch(Mockito.eq(Arrays.asList(upr)), Mockito.eq(upr.getJsonClassName()));
	}

	@Test
	public void updateUserProfileTest() throws IOException {
		ug.setIsIndividual(true);
		Mockito.when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		Mockito.when(mockUserProfileManager.getUserProfile(principalID.toString())).thenReturn(up);
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecord(changeMessage);
		Mockito.verify(mockUserGroupDAO).get(principalID);
		Mockito.verify(mockUserProfileManager).getUserProfile(principalID.toString());
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
	public void logGroupMembersWithZeroMembersTest() throws IOException {
		Mockito.when(mockGroupMembersDao.getMembers(principalID.toString())).thenReturn(new ArrayList<UserGroup>());
		writer.captureAllMembers(principalID.toString(), timestamp);
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
		Mockito.verify(mockObjectRecordDao, Mockito.never()).saveBatch(Mockito.anyList(), Mockito.anyString());
	}

	@Test
	public void logGroupMembersTest() throws IOException {
		List<UserGroup> list = createListOfMembers(2);
		Mockito.when(mockGroupMembersDao.getMembers(principalID.toString())).thenReturn(list);
		TeamMember teamMember = new TeamMember();
		Mockito.when(mockTeamDAO.getMember(Mockito.anyString(), Mockito.anyString())).thenReturn(teamMember);
		writer.captureAllMembers(principalID.toString(), timestamp);
		Mockito.verify(mockObjectRecordDao).saveBatch(Mockito.anyList(), Mockito.anyString());
	}

	/**
	 * create a list of members
	 * @param numberOfMembers
	 * @return
	 */
	private List<UserGroup> createListOfMembers(int numberOfMembers) {
		List<UserGroup> list = new ArrayList<UserGroup>();
		for (int i = 0; i < numberOfMembers; i++) {
			UserGroup ug = new UserGroup();
			ug.setId(""+i);
			list.add(ug);
		}
		return list;
	}
}
