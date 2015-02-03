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
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;

public class ChallengeManagerImplTest {
	
	private ChallengeManagerImpl challengeManager;
	private AuthorizationManager mockAuthorizationManager;
	private ChallengeDAO mockChallengeDAO;
	
	private static final String PROJECT_ID="syn123456";
	private static final String TEAM_ID="987654321";
	
	private Challenge newChallenge() {
		Challenge challenge = new Challenge();
		challenge.setParticipantTeamId(TEAM_ID);
		challenge.setProjectId(PROJECT_ID);
		return challenge;
	}
	
	@Before
	public void setUp() throws Exception {
		mockChallengeDAO = Mockito.mock(ChallengeDAO.class);
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		challengeManager = new ChallengeManagerImpl(mockChallengeDAO, mockAuthorizationManager);
	}

	@Test
	public void testCreate() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		Challenge challenge = new Challenge();
		challenge.setProjectId(PROJECT_ID);
		challenge.setParticipantTeamId(TEAM_ID);
		Challenge created = newChallenge();
		created.setId("111");
		when(mockChallengeDAO.create(challenge)).thenReturn(created);
		when(mockAuthorizationManager.canAccess(userInfo, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(
						AuthorizationManagerUtil.AUTHORIZED);
		
		assertEquals(created, challengeManager.createChallenge(userInfo, challenge));
		verify(mockAuthorizationManager).canAccess(userInfo, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.CREATE);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testCreateUnathorized() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		Challenge challenge = new Challenge();
		challenge.setProjectId(PROJECT_ID);
		when(mockAuthorizationManager.canAccess(userInfo, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(
						AuthorizationManagerUtil.ACCESS_DENIED);
		challengeManager.createChallenge(userInfo, challenge);
	}

}
