package org.sagebionetworks.snapshot.workers.writers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.audit.KinesisJsonEntityRecord;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class PrincipalObjectRecordWriterTest {
	@Mock
	private UserProfileManager mockUserProfileManager;
	@Mock
	private UserGroupDAO mockUserGroupDAO;
	@Mock
	private TeamDAO mockTeamDAO;
	@Mock
	private GroupMembersDAO mockGroupMembersDao;
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private AwsKinesisFirehoseLogger firehoseLogger;
	
	@InjectMocks
	private PrincipalObjectRecordWriter writer;

	@Captor
	private ArgumentCaptor<List<KinesisJsonEntityRecord<?>>> recordCaptor;
	@Captor
	private ArgumentCaptor<List<KinesisObjectSnapshotRecord<TeamMember>>> memberCaptor;
	@Captor
	private ArgumentCaptor<String> streamNameCaptor;
	private Long principalID = 123L;
	private Date createdOn = new Date();
	private Date modifiedOn = new Date();
	private String etag = "etag";
	private String teamId = "111";
	private Long timestamp = System.currentTimeMillis();
	
	UserGroup ug;
	Team team;
	UserProfile up;
	
	@BeforeEach
	public void setup() {
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
	
	@Test
	public void nonPrincipalChangeMessage() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACTIVITY, "1", "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		
		assertThrows(IllegalArgumentException.class, () -> {			
			writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		});
	}

	@Test
	public void createTeamTest() throws IOException {
		ug.setIsIndividual(false);
		when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		when(mockTeamDAO.get(principalID.toString())).thenReturn(team);
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(mockUserGroupDAO).get(principalID);
		verify(mockUserProfileManager, never()).getUserProfile(anyString());
		verify(mockTeamDAO).get(principalID.toString());
		verify(mockTeamDAO, never()).getMember(anyString(), anyString());
        verify(firehoseLogger,times(2)).logBatch(streamNameCaptor.capture(),anyList());
		assertTrue(streamNameCaptor.getAllValues().containsAll(Arrays.asList("userGroupSnapshots", "teamSnapshots")));
	}
	
	@Test
	public void deleteTeamTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(firehoseLogger, never()).logBatch(anyString(),anyList());
	}

	@Test
	public void updateTeamTest() throws IOException {
		ug.setIsIndividual(false);
		when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		when(mockTeamDAO.get(principalID.toString())).thenReturn(team);
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(mockUserGroupDAO).get(principalID);
		verify(mockUserProfileManager, never()).getUserProfile(anyString());
		verify(mockTeamDAO).get(principalID.toString());
		verify(mockTeamDAO, never()).getMember(anyString(), anyString());
	}

	@Test
	public void createUserProfileTest() throws IOException {
		ug.setIsIndividual(true);
		when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		when(mockUserProfileManager.getUserProfile(principalID.toString())).thenReturn(up);
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(mockUserGroupDAO).get(principalID);
		verify(mockUserProfileManager).getUserProfile(principalID.toString());
		verify(mockTeamDAO, never()).get(anyString());
		verify(mockTeamDAO, never()).getMember(anyString(), anyString());
		verify(firehoseLogger,times(2)).logBatch(streamNameCaptor.capture(),anyList());
		assertTrue(streamNameCaptor.getAllValues().containsAll(Arrays.asList("userGroupSnapshots", "userProfileSnapshots")));
	}

	@Test
	public void updateUserProfileTest() throws IOException {
		ug.setIsIndividual(true);
		when(mockUserGroupDAO.get(principalID)).thenReturn(ug);
		when(mockUserProfileManager.getUserProfile(principalID.toString())).thenReturn(up);
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, principalID.toString(),
				ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(mockUserGroupDAO).get(principalID);
		verify(mockUserProfileManager).getUserProfile(principalID.toString());
		verify(mockTeamDAO, never()).get(anyString());
		verify(mockTeamDAO, never()).getMember(anyString(), anyString());
		verify(firehoseLogger,times(2))
				.logBatch(streamNameCaptor.capture(), anyList());
		assertTrue(streamNameCaptor.getAllValues().containsAll(Arrays.asList("userProfileSnapshots","userGroupSnapshots")));
	}

	@Test
	public void deleteUserProfileTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, principalID.toString(), ObjectType.PRINCIPAL, etag, timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(firehoseLogger,never()).logBatch(anyString(),anyList());
	}

	@Test
	public void logGroupMembersWithZeroMembersTest() throws IOException {
		Mockito.when(mockGroupMembersDao.getMembers(principalID.toString())).thenReturn(new ArrayList<UserGroup>());
		writer.captureAllMembers(new ChangeMessage().setObjectId(principalID.toString()).setTimestamp(new Date(timestamp)));
		Mockito.verify(mockTeamDAO, Mockito.never()).getMember(Mockito.anyString(), Mockito.anyString());
		verify(firehoseLogger,never()).logBatch(anyString(),anyList());
	}

	@Test
	public void logGroupMembersTest() throws IOException {
		List<UserGroup> list = createListOfMembers(2);
		when(mockGroupMembersDao.getMembers(principalID.toString())).thenReturn(list);
		when(mockTeamDAO.getAdminTeamMemberIds(any())).thenReturn(List.of("1"));
		
		// call under test
		writer.captureAllMembers(
				new ChangeMessage().setObjectId(principalID.toString()).setTimestamp(new Date(timestamp)));

		verify(mockTeamDAO).getAdminTeamMemberIds(principalID.toString());
		verify(firehoseLogger).logBatch(streamNameCaptor.capture(), memberCaptor.capture());
		assertTrue(streamNameCaptor.getValue().equals("teamMemberSnapshots"));

		List<TeamMember> members = memberCaptor.getValue().stream().map(r -> r.getSnapshot())
				.collect(Collectors.toList());
		List<TeamMember> expected = List.of(
				new TeamMember().setIsAdmin(false).setTeamId("123").setMember(new UserGroupHeader().setOwnerId("0")),
				new TeamMember().setIsAdmin(true).setTeamId("123").setMember(new UserGroupHeader().setOwnerId("1")));
		assertEquals(expected, members);
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
