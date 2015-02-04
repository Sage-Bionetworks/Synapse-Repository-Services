package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
import org.sagebionetworks.repo.model.ChallengePagedResults;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.ChallengeTeamDAO;
import org.sagebionetworks.repo.model.ChallengeTeamPagedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.SubmissionTeamPagedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class ChallengeManagerImpl implements ChallengeManager {
	
	@Autowired
	ChallengeDAO challengeDAO;
	
	@Autowired
	ChallengeDAO challengeTeamDAO;
	
	@Autowired
	AuthorizationManager authorizationManager;
	
	public ChallengeManagerImpl(ChallengeDAO challengeDAO, 
			ChallengeTeamDAO challengeTeamDAO, 
			AuthorizationManager authorizationManager) {
		this.challengeDAO=challengeDAO;
		this.authorizationManager=authorizationManager;
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Challenge createChallenge(UserInfo userInfo, Challenge challenge) throws DatastoreException, NotFoundException {
		if (challenge.getProjectId()==null) throw new InvalidModelException("Project ID is required.");
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, 
						challenge.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.CREATE));
		return challengeDAO.create(challenge);
	}

	@Override
	public Challenge getChallengeByProjectId(UserInfo userInfo, String projectId)
			throws DatastoreException, NotFoundException {
		if (projectId==null) throw new IllegalArgumentException("Project ID is required.");
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, 
						projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		return challengeDAO.getForProject(projectId);
	}

	@Override
	public ChallengePagedResults listChallengesForParticipant(
			UserInfo userInfo, long participantId, long limit, long offset) throws NotFoundException, DatastoreException {
		ChallengePagedResults result = new ChallengePagedResults();
		if (userInfo.isAdmin()) {
			result.setResults(challengeDAO.listForUser(participantId, limit, offset));
			result.setTotalNumberOfResults(challengeDAO.listForUserCount(participantId));
		} else {
			result.setResults(challengeDAO.listForUser(participantId, userInfo.getGroups(), limit, offset));
			result.setTotalNumberOfResults(challengeDAO.listForUserCount(participantId, userInfo.getGroups()));
		}
		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Challenge updateChallenge(UserInfo userInfo, Challenge challenge)
			throws DatastoreException, NotFoundException {
		if (challenge.getProjectId()==null) throw new InvalidModelException("Project ID is required.");
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, 
						challenge.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE));
		return challengeDAO.update(challenge);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteChallenge(UserInfo userInfo, long challengeId)
			throws DatastoreException, NotFoundException {
		Challenge challenge = challengeDAO.get(challengeId);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, 
						challenge.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.DELETE));
		challengeDAO.delete(challengeId);
	}

	@Override
	public PaginatedIds listParticipantsInChallenge(UserInfo userInfo,
			long challengeId, Boolean affiliated, long limit, long offset)
			throws DatastoreException, NotFoundException {
		Challenge challenge = challengeDAO.get(challengeId);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, 
						challenge.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		PaginatedIds result = new PaginatedIds();
		List<Long> longIds = challengeDAO.listParticipants(challengeId, affiliated, limit, offset);
		List<String> stringIds = new ArrayList<String>(longIds.size());
		for (Long id : longIds) stringIds.add(id.toString());
		result.setResults(stringIds);
		result.setTotalNumberOfResults(challengeDAO.listParticipantsCount(challengeId, affiliated));
		return result;
	}

	@Override
	public ChallengeTeam createChallengeTeam(UserInfo userInfo,
			ChallengeTeam challengeTeam) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChallengeTeamPagedResults listChallengeTeams(UserInfo userInfo,
			long challengeId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PaginatedIds listRegistratableTeams(UserInfo userInfo,
			long challengeId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChallengeTeam updateChallengeTeam(UserInfo userInfo,
			ChallengeTeam challengeTeam) throws DatastoreException,
			NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteChallengeTeam(UserInfo userInfo, long challengeTeamId)
			throws DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SubmissionTeamPagedResults listSubmissionTeams(UserInfo userInfo,
			long challengeId, long submitterPrincipalId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

}
