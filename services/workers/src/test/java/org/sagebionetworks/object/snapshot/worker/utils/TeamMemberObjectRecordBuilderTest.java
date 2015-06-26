package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

import com.amazonaws.services.sqs.model.Message;

public class TeamMemberObjectRecordBuilderTest {
	
	private TeamMemberObjectRecordBuilder builder;
	private TeamDAO mockTeamDAO;
	
	private String etag = "etag";
	private String teamId = "111";
	private Long timestamp = System.currentTimeMillis();
	
	TeamMember teamMember;
	UserGroupHeader member;
	
	@Before
	public void setup() {
		mockTeamDAO = Mockito.mock(TeamDAO.class);
		builder = new TeamMemberObjectRecordBuilder(mockTeamDAO);	
		
		member = new UserGroupHeader();
		member.setEmail("employee@sagebase.org");
		member.setIsIndividual(true);
		
		teamMember = new TeamMember();
		teamMember.setTeamId(teamId);
		teamMember.setIsAdmin(false);
		teamMember.setMember(member);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void nonTeamMemberChangeMessage() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACTIVITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
	}
	
	@Test
	public void addTeamMemberTest() throws IOException {
		Mockito.when(mockTeamDAO.getMember(teamId, "1")).thenReturn(teamMember);
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "1", ObjectType.TEAM_MEMBER, teamId, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
		Mockito.verify(mockTeamDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO).getMember(teamId, "1");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void removeTeamMemberTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, "1", ObjectType.TEAM_MEMBER, teamId, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
	}
}
