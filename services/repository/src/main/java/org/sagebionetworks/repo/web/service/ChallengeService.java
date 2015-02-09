package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengePagedResults;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.ChallengeTeamPagedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ChallengeService {
	public Challenge createChallenge(Long userId, Challenge challenge) throws DatastoreException, NotFoundException;

	Challenge getChallengeByProjectId(Long userId, String projectId)
			throws DatastoreException, NotFoundException;

	ChallengePagedResults listChallengesForParticipant(Long userId,
			long participantId, long limit, long offset)
			throws DatastoreException, NotFoundException;

	Challenge updateChallenge(Long userId, Challenge challenge)
			throws DatastoreException, NotFoundException;

	void deleteChallenge(Long userId, long challengeId)
			throws DatastoreException, NotFoundException;

	PaginatedIds listParticipantsInChallenge(Long userId, long challengeId,
			Boolean affiliated, long limit, long offset)
			throws DatastoreException, NotFoundException;

	ChallengeTeam createChallengeTeam(Long userId, ChallengeTeam challengeTeam)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	ChallengeTeamPagedResults listChallengeTeams(Long userId, long challengeId,
			long limit, long offset) throws DatastoreException,
			NotFoundException;

	PaginatedIds listRegistratableTeams(Long userId, long challengeId,
			long limit, long offset) throws DatastoreException,
			NotFoundException;

	ChallengeTeam updateChallengeTeam(Long userId, ChallengeTeam challengeTeam)
			throws DatastoreException, NotFoundException;

	void deleteChallengeTeam(Long userId, long challengeTeamId)
			throws DatastoreException, NotFoundException;

	PaginatedIds listSubmissionTeams(Long userId, long challengeId,
			long submitterPrincipalId, long limit, long offset)
			throws DatastoreException, NotFoundException;
}
