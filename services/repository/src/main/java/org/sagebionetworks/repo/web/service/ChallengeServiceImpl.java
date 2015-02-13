package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.ChallengeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengePagedResults;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.ChallengeTeamPagedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ChallengeServiceImpl implements ChallengeService {

	@Autowired
	private ChallengeManager challengeManager;

	@Autowired
	private UserManager userManager;

	@Override
	public Challenge createChallenge(Long userId, Challenge challenge) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return challengeManager.createChallenge(userInfo, challenge);
	}
	
	@Override
	public Challenge getChallengeByProjectId(Long userId, String projectId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return challengeManager.getChallengeByProjectId(userInfo, projectId);
	}
	
	@Override
	public ChallengePagedResults listChallengesForParticipant(Long userId, long participantId, long limit, long offset) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return challengeManager.listChallengesForParticipant(userInfo, participantId, limit, offset);
	}

	@Override
	public Challenge updateChallenge(Long userId, Challenge challenge) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return challengeManager.updateChallenge(userInfo, challenge);
	}
	
	@Override
	public void deleteChallenge(Long userId, long challengeId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		challengeManager.deleteChallenge(userInfo, challengeId);
	}
	
	@Override
	public PaginatedIds listParticipantsInChallenge(Long userId, long challengeId, Boolean affiliated, long limit, long offset) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return challengeManager.listParticipantsInChallenge(userInfo, challengeId, affiliated, limit, offset);
	}

	@Override
	public ChallengeTeam createChallengeTeam(Long userId, ChallengeTeam challengeTeam) throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return challengeManager.createChallengeTeam(userInfo, challengeTeam);
	}
	
	@Override
	public ChallengeTeamPagedResults listChallengeTeams(Long userId, long challengeId, long limit, long offset) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return challengeManager.listChallengeTeams(userInfo, challengeId, limit, offset);
	}

	@Override
	public PaginatedIds listRegistratableTeams(Long userId, long challengeId, long limit, long offset) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return challengeManager.listRegistratableTeams(userInfo, challengeId, limit, offset);
	}

	@Override
	public ChallengeTeam updateChallengeTeam(Long userId, ChallengeTeam challengeTeam) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return challengeManager.updateChallengeTeam(userInfo, challengeTeam);
	}
	
	@Override
	public void deleteChallengeTeam(Long userId, long challengeTeamId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		challengeManager.deleteChallengeTeam(userInfo, challengeTeamId);
	}
	
	@Override
	public PaginatedIds listSubmissionTeams(Long userId, long challengeId, long submitterPrincipalId, long limit, long offset) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return challengeManager.listSubmissionTeams(userInfo, challengeId, submitterPrincipalId, limit, offset);
	}



}
