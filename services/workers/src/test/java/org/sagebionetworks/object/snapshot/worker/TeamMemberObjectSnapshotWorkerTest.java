package org.sagebionetworks.object.snapshot.worker;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.progress.ProgressCallback;

import com.amazonaws.services.sqs.model.Message;

public class TeamMemberObjectSnapshotWorkerTest {
	
	private ObjectSnapshotWorker worker;
	private ObjectRecordDAO mockObjectRecordDAO;
	private UserProfileDAO mockUserProfileDAO;
	private UserGroupDAO mockUserGroupDAO;
	private TeamDAO mockTeamDAO;
	@SuppressWarnings("rawtypes")
	private ProgressCallback mockProgressCallback;
	
	private String etag = "etag";
	private String teamId = "111";
	private Long timestamp = System.currentTimeMillis();
	
	TeamMember teamMember;
	UserGroupHeader member;
	
	@Before
	public void setup() {
		mockObjectRecordDAO = Mockito.mock(ObjectRecordDAO.class);
		mockUserGroupDAO = Mockito.mock(UserGroupDAO.class);
		mockUserProfileDAO = Mockito.mock(UserProfileDAO.class);
		mockTeamDAO = Mockito.mock(TeamDAO.class);
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		worker = new ObjectSnapshotWorker(mockObjectRecordDAO, mockUserGroupDAO, mockUserProfileDAO, mockTeamDAO);	
		
		member = new UserGroupHeader();
		member.setEmail("employee@sagebase.org");
		member.setIsIndividual(true);
		
		teamMember = new TeamMember();
		teamMember.setTeamId(teamId);
		teamMember.setIsAdmin(false);
		teamMember.setMember(member);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void nonTeamMemberChangeMessage() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACTIVITY, "etag", timestamp);
		worker.run(mockProgressCallback, message);
		Mockito.verify(mockProgressCallback).progressMade(message);
		Mockito.verify(mockObjectRecordDAO, Mockito.never()).saveBatch(Mockito.anyList());
		Mockito.verify(mockUserGroupDAO, Mockito.never()).get(Mockito.anyLong());
		Mockito.verify(mockUserProfileDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void addTeamMemberTest() throws IOException {
		Mockito.when(mockTeamDAO.getMember(teamId, "1")).thenReturn(teamMember);
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "1", ObjectType.TEAM_MEMBER, teamId, etag, timestamp);
		worker.run(mockProgressCallback, message);
		Mockito.verify(mockProgressCallback).progressMade(message);
		Mockito.verify(mockObjectRecordDAO, Mockito.times(1)).saveBatch(Mockito.anyList());
		Mockito.verify(mockUserGroupDAO, Mockito.never()).get(Mockito.anyLong());
		Mockito.verify(mockUserProfileDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO).getMember(teamId, "1");
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void removeTeamMemberTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, "1", ObjectType.TEAM_MEMBER, teamId, etag, timestamp);
		worker.run(mockProgressCallback, message);
		Mockito.verify(mockProgressCallback).progressMade(message);
		Mockito.verify(mockObjectRecordDAO, Mockito.never()).saveBatch(Mockito.anyList());
		Mockito.verify(mockUserGroupDAO, Mockito.never()).get(Mockito.anyLong());
		Mockito.verify(mockUserProfileDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO, Mockito.never()).get(Mockito.anyString());
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
	}
}
