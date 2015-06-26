package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

import com.amazonaws.services.sqs.model.Message;

public class PrincipalObjectRecordBuilderTest {
	
	private PrincipalObjectRecordBuilder builder;
	private UserProfileDAO mockUserProfileDAO;
	private UserGroupDAO mockUserGroupDAO;
	private TeamDAO mockTeamDAO;
	
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
		builder = new PrincipalObjectRecordBuilder(mockUserGroupDAO, mockUserProfileDAO, mockTeamDAO);	
		
		ug = new UserGroup();
		
		buildTeam();
		
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

	private void buildTeam() {
		team = new Team();
		team.setCanPublicJoin(true);
		team.setCreatedBy("333");
		team.setCreatedOn(createdOn);
		team.setDescription("test Team");
		team.setEtag(etag);
		team.setId(teamId);
		team.setModifiedBy("444");
		team.setModifiedOn(modifiedOn);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void nonPrincipalChangeMessage() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACTIVITY, "1", "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
	}

	@Test
	public void createTeamTest() throws IOException {
		ug.setIsIndividual(false);
		Mockito.when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		Mockito.when(mockTeamDAO.get(principalID.toString())).thenReturn(team);
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
		Mockito.verify(mockUserGroupDAO).get(principalID);
		Mockito.verify(mockUserProfileDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO).get(principalID.toString());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void deleteTeamTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
	}

	@Test
	public void updateTeamTest() throws IOException {
		ug.setIsIndividual(false);
		Mockito.when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		Mockito.when(mockTeamDAO.get(principalID.toString())).thenReturn(team);
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
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
		builder.build(changeMessage);
		Mockito.verify(mockUserGroupDAO).get(principalID);
		Mockito.verify(mockUserProfileDAO).get(principalID.toString());
		Mockito.verify(mockTeamDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
	}
	
	@Test
	public void updateUserProfileTest() throws IOException {
		ug.setIsIndividual(true);
		Mockito.when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		Mockito.when(mockUserProfileDAO.get(principalID.toString())).thenReturn(up);
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
		Mockito.verify(mockUserGroupDAO).get(principalID);
		Mockito.verify(mockUserProfileDAO).get(principalID.toString());
		Mockito.verify(mockTeamDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void deleteUserProfileTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
	}
}
