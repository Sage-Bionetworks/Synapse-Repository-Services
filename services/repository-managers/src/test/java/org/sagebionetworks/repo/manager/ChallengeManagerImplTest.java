package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
import org.sagebionetworks.repo.model.ChallengeTeamDAO;
import org.sagebionetworks.repo.model.ObjectType;
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
	public void getForProjectIdUnathorized() throws Exception {
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(
						AuthorizationManagerUtil.ACCESS_DENIED);
		challengeManager.getChallengeByProjectId(USER_INFO, PROJECT_ID);
	}
	
	@Test
	public void testListChallengesForParticipant() throws Exception {
		
	}

}
