package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
import org.sagebionetworks.repo.model.ChallengePagedResults;
import org.sagebionetworks.repo.model.ChallengeTeamDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;

public class ChallengeManagerImplTest {
	
	private ChallengeManagerImpl challengeManager;
	private AuthorizationManager mockAuthorizationManager;
	private ChallengeDAO mockChallengeDAO;
	private ChallengeTeamDAO mockChallengeTeamDAO;
	
	private static final String PROJECT_ID="syn123456";
	private static final String TEAM_ID="987654321";
	
	private static final UserInfo USER_INFO = new UserInfo(false);

	private Challenge newChallenge() {
		Challenge challenge = new Challenge();
		challenge.setParticipantTeamId(TEAM_ID);
		challenge.setProjectId(PROJECT_ID);
		return challenge;
	}
	
	@Before
	public void setUp() throws Exception {
		mockChallengeDAO = Mockito.mock(ChallengeDAO.class);
		mockChallengeTeamDAO = Mockito.mock(ChallengeTeamDAO.class);
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		challengeManager = new ChallengeManagerImpl(mockChallengeDAO, mockChallengeTeamDAO, mockAuthorizationManager);
	}

	@Test
	public void testCreate() throws Exception {
		Challenge challenge = newChallenge();
		Challenge created = newChallenge();
		created.setId("111");
		when(mockChallengeDAO.create(challenge)).thenReturn(created);
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(
						AuthorizationManagerUtil.AUTHORIZED);
		
		assertEquals(created, challengeManager.createChallenge(USER_INFO, challenge));
		verify(mockAuthorizationManager).canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.CREATE);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testCreateUnathorized() throws Exception {
		Challenge challenge = newChallenge();
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(
						AuthorizationManagerUtil.ACCESS_DENIED);
		challengeManager.createChallenge(USER_INFO, challenge);
	}
	
	@Test
	public void testGetChallengeByProjectId() throws Exception {
		Challenge created = newChallenge();
		created.setId("111");
		when(mockChallengeDAO.getForProject(PROJECT_ID)).thenReturn(created);
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(
						AuthorizationManagerUtil.AUTHORIZED);
		
		assertEquals(created, challengeManager.getChallengeByProjectId(USER_INFO, PROJECT_ID));
		verify(mockAuthorizationManager).canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.READ);
	}

	@Test(expected=UnauthorizedException.class)
	public void getChallengeForProjectIdUnathorized() throws Exception {
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(
						AuthorizationManagerUtil.ACCESS_DENIED);
		challengeManager.getChallengeByProjectId(USER_INFO, PROJECT_ID);
	}
	
	@Test
	public void testListChallengesForParticipant() throws Exception {
		UserInfo requester = new UserInfo(false);
		long limit = 10L;
		long offset = 0L;
		long participantId = 123L;
		Challenge publicChallenge = newChallenge(); publicChallenge.setId("123");
		Challenge privateChallenge = newChallenge(); privateChallenge.setId("456");
		List<Challenge> participantsPublicChallenges = Collections.singletonList(newChallenge());
		List<Challenge> participantsPrivateChallenges = new ArrayList<Challenge>();
		participantsPrivateChallenges.add(publicChallenge);
		participantsPrivateChallenges.add(privateChallenge);
		
		when(mockChallengeDAO.listForUser(participantId, limit, offset)).thenReturn(participantsPrivateChallenges);
		when(mockChallengeDAO.listForUserCount(participantId)).thenReturn((long)participantsPrivateChallenges.size());
		// requester can see only the public challenge
		when(mockChallengeDAO.listForUser(participantId, requester.getGroups(), limit, offset)).thenReturn(participantsPublicChallenges);
		when(mockChallengeDAO.listForUserCount(participantId, requester.getGroups())).thenReturn((long)participantsPublicChallenges.size());
		
		ChallengePagedResults expected = new ChallengePagedResults();
		expected.setResults(participantsPublicChallenges);
		expected.setTotalNumberOfResults((long)participantsPublicChallenges.size());
		assertEquals(expected, challengeManager.listChallengesForParticipant(requester, participantId, limit, offset));

		// now check administrative access
		requester = new UserInfo(true);
		
		expected = new ChallengePagedResults();
		expected.setResults(participantsPrivateChallenges);
		expected.setTotalNumberOfResults((long)participantsPrivateChallenges.size());
		assertEquals(expected, challengeManager.listChallengesForParticipant(requester, participantId, limit, offset));
	}

	@Test
	public void testUpdateChallenge() throws Exception {
		Challenge challenge = newChallenge(); 
		challenge.setId("111");
		Challenge updated = newChallenge(); 
		updated.setId("111"); 
		updated.setParticipantTeamId("999");
		when(mockChallengeDAO.update(challenge)).thenReturn(updated);
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(
						AuthorizationManagerUtil.AUTHORIZED);
		
		assertEquals(updated, challengeManager.updateChallenge(USER_INFO, challenge));
		verify(mockAuthorizationManager).canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUpdateChallengeUnathorized() throws Exception {
		Challenge challenge = newChallenge();
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(
						AuthorizationManagerUtil.ACCESS_DENIED);
		challengeManager.updateChallenge(USER_INFO, challenge);
	}
	
	@Test
	public void testDeleteChallenge() throws Exception {
		Long challengeId = 111L;
		Challenge challenge = newChallenge(); 
		challenge.setId(challengeId.toString());
		when(mockChallengeDAO.get(challengeId)).thenReturn(challenge);
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(
						AuthorizationManagerUtil.AUTHORIZED);
		
		challengeManager.deleteChallenge(USER_INFO, challengeId);
		verify(mockChallengeDAO).get(challengeId);
		verify(mockAuthorizationManager).canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.DELETE);
		verify(mockChallengeDAO).delete(challengeId);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testDeleteChallengeUnathorized() throws Exception {
		Long challengeId = 111L;
		Challenge challenge = newChallenge(); 
		challenge.setId(challengeId.toString());
		when(mockChallengeDAO.get(challengeId)).thenReturn(challenge);
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(
						AuthorizationManagerUtil.ACCESS_DENIED);
		challengeManager.deleteChallenge(USER_INFO, challengeId);
	}
	
	@Test
	public void testListParticipantsInChallenge() throws Exception {
		Long challengeId = 111L;
		Challenge challenge = newChallenge(); 
		challenge.setId(challengeId.toString());
		Boolean affiliated=null; // return all participants
		long limit = 10L;
		long offset = 0L;
		List<Long> participants = Arrays.asList(new Long[]{111L, 222L, 333L});
		PaginatedIds expected = new PaginatedIds();
		List<String> stringIds = new ArrayList<String>();
		for (Long id : participants) stringIds.add(id.toString());
		expected.setResults(stringIds);
		expected.setTotalNumberOfResults((long)participants.size());
		
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(
						AuthorizationManagerUtil.AUTHORIZED);
		
		when(mockChallengeDAO.get(challengeId)).thenReturn(challenge);
		when(mockChallengeDAO.listParticipants(challengeId, affiliated, limit, offset)).
			thenReturn(participants);
		when(mockChallengeDAO.listParticipantsCount(challengeId, affiliated)).
			thenReturn((long)participants.size());
		
		assertEquals(expected, challengeManager.listParticipantsInChallenge(USER_INFO,
				challengeId, affiliated, limit, offset));
	}
}
