package org.sagebionetworks.object.snapshot.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.progress.ProgressCallback;

import com.amazonaws.services.sqs.model.Message;

public class PrincipalObjectSnapshotWorkerTest {
	
	private ObjectSnapshotWorker worker;
	private ObjectRecordDAO mockObjectRecordDAO;
	private UserProfileDAO mockUserProfileDAO;
	private UserGroupDAO mockUserGroupDAO;
	private TeamDAO mockTeamDAO;
	private GroupMembersDAO mockGroupMemberDAO;
	@SuppressWarnings("rawtypes")
	private ProgressCallback mockProgressCallback;
	
	private Long principalID = 123L;
	private Date createdOn = new Date();
	private Date modifiedOn = new Date();
	private String etag = "etag";
	private String teamId = "111";
	private Long timestamp = System.currentTimeMillis();
	
	UserGroup ug;
	UserGroup ug2;
	UserGroup ug1;
	Team team;
	UserProfile up;
	TeamMember teamMember1;
	TeamMember teamMember2;
	UserGroupHeader member1;
	UserGroupHeader member2;
	
	@Before
	public void setup() {
		mockObjectRecordDAO = Mockito.mock(ObjectRecordDAO.class);
		mockUserGroupDAO = Mockito.mock(UserGroupDAO.class);
		mockUserProfileDAO = Mockito.mock(UserProfileDAO.class);
		mockTeamDAO = Mockito.mock(TeamDAO.class);
		mockGroupMemberDAO = Mockito.mock(GroupMembersDAO.class);
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		worker = new ObjectSnapshotWorker(mockObjectRecordDAO, mockUserGroupDAO, mockUserProfileDAO, mockTeamDAO, mockGroupMemberDAO);	
		
		ug = new UserGroup();
		
		team = new Team();
		team.setCanPublicJoin(true);
		team.setCreatedBy("333");
		team.setCreatedOn(createdOn);
		team.setDescription("test Team");
		team.setEtag(etag);
		team.setId(teamId);
		team.setModifiedBy("444");
		team.setModifiedOn(modifiedOn);
		
		ug2 = new UserGroup();
		ug2.setCreationDate(createdOn);
		ug2.setEtag(etag);
		ug2.setId("2");
		ug2.setIsIndividual(true);
		
		ug1 = new UserGroup();
		ug1.setCreationDate(createdOn);
		ug1.setEtag(etag);
		ug1.setId("1");
		ug1.setIsIndividual(true);
		
		up = new UserProfile();
		up.setCompany("Sage");
		up.setEmail("employee@sagebase.org");
		up.setEmails(Arrays.asList("employee@sagebase.org", "employee@gmail.com"));
		up.setEtag(etag);
		up.setOwnerId(principalID.toString());
		
		member1 = new UserGroupHeader();
		member1.setEmail("employee@sagebase.org");
		member1.setIsIndividual(true);
		
		member1 = new UserGroupHeader();
		member1.setEmail("employee@gmail.com");
		member1.setIsIndividual(true);
		
		teamMember1 = new TeamMember();
		teamMember1.setTeamId(teamId);
		teamMember1.setIsAdmin(true);
		teamMember1.setMember(member1);
		
		teamMember2 = new TeamMember();
		teamMember2.setTeamId(teamId);
		teamMember2.setIsAdmin(false);
		teamMember2.setMember(member2);
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void nonPrincipalChangeMessage() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACTIVITY, "etag", timestamp);
		worker.run(mockProgressCallback, message);
		Mockito.verify(mockProgressCallback).progressMade(message);
		Mockito.verify(mockObjectRecordDAO, Mockito.never()).saveBatch(Mockito.anyList());
		Mockito.verify(mockUserGroupDAO, Mockito.never()).get(Mockito.anyLong());
		Mockito.verify(mockUserProfileDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockGroupMemberDAO, Mockito.never()).getMembers(Mockito.anyString());
		Mockito.verify(mockTeamDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void createTeamTest() throws IOException {
		ug.setIsIndividual(false);
		Mockito.when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		Mockito.when(mockTeamDAO.get(principalID.toString())).thenReturn(team);
		Mockito.when(mockGroupMemberDAO.getMembers(principalID.toString())).thenReturn(new ArrayList<UserGroup>());
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		worker.run(mockProgressCallback, message);
		Mockito.verify(mockProgressCallback).progressMade(message);
		Mockito.verify(mockObjectRecordDAO).saveBatch(Mockito.anyList());
		Mockito.verify(mockUserGroupDAO).get(principalID);
		Mockito.verify(mockUserProfileDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockGroupMemberDAO).getMembers(principalID.toString());
		Mockito.verify(mockTeamDAO).get(principalID.toString());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void deleteTeamTest() throws IOException {
		ug.setIsIndividual(false);
		Mockito.when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		Mockito.when(mockTeamDAO.get(principalID.toString())).thenReturn(team);
		Mockito.when(mockGroupMemberDAO.getMembers(principalID.toString())).thenReturn(new ArrayList<UserGroup>());
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		worker.run(mockProgressCallback, message);
		Mockito.verify(mockProgressCallback).progressMade(message);
		Mockito.verify(mockObjectRecordDAO).saveBatch(Mockito.anyList());
		Mockito.verify(mockUserGroupDAO, Mockito.never()).get(Mockito.anyLong());
		Mockito.verify(mockUserProfileDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockGroupMemberDAO, Mockito.never()).getMembers(Mockito.anyString());
		Mockito.verify(mockTeamDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void updateTeamTest() throws IOException {
		ug.setIsIndividual(false);
		Mockito.when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		Mockito.when(mockTeamDAO.get(principalID.toString())).thenReturn(team);
		Mockito.when(mockGroupMemberDAO.getMembers(principalID.toString())).thenReturn(Arrays.asList(ug2, ug1));
		Mockito.when(mockTeamDAO.getMember(teamId, "1")).thenReturn(teamMember1);
		Mockito.when(mockTeamDAO.getMember(teamId, "2")).thenReturn(teamMember2);
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		worker.run(mockProgressCallback, message);
		Mockito.verify(mockProgressCallback).progressMade(message);
		Mockito.verify(mockObjectRecordDAO, Mockito.times(3)).saveBatch(Mockito.anyList());
		Mockito.verify(mockUserGroupDAO).get(principalID);
		Mockito.verify(mockUserProfileDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockGroupMemberDAO).getMembers(principalID.toString());
		Mockito.verify(mockTeamDAO).get(principalID.toString());
		Mockito.verify(mockTeamDAO).getMember(teamId, "1");
		Mockito.verify(mockTeamDAO).getMember(teamId, "2");
	}
	
	@Test
	public void createUserGroupTest() {
		
	}
	
	@Test
	public void updateUserGroupTest() {
		
	}
	
	@Test
	public void updateUserProfileTest() {
		
	}
}
