package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
import org.sagebionetworks.repo.model.ChallengePagedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class ChallengeManagerImpl implements ChallengeManager {
	
	@Autowired
	ChallengeDAO challengeDAO;
	
	@Autowired
	AuthorizationManager authorizationManager;
	
	public ChallengeManagerImpl(ChallengeDAO challengeDAO, AuthorizationManager authorizationManager) {
		this.challengeDAO=challengeDAO;
		this.authorizationManager=authorizationManager;
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Challenge createChallenge(UserInfo userInfo, Challenge challenge) throws DatastoreException, NotFoundException {
		if(challenge.getProjectId()==null) throw new InvalidModelException("Project ID is required.");
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, 
						challenge.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.CREATE));
		return challengeDAO.create(challenge);
	}

	@Override
	public Challenge getByProjectId(UserInfo userInfo, String projectId)
			throws DatastoreException, NotFoundException {
		if(projectId==null) throw new IllegalArgumentException("Project ID is required.");
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, 
						projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		return challengeDAO.getForProject(projectId);
	}

	@Override
	public ChallengePagedResults listForParticipant(UserInfo userInfo,
			String participantId) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Challenge update(UserInfo userInfo, Challenge challenge)
			throws DatastoreException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(UserInfo userInfo, Long challengeId)
			throws DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PaginatedIds listParticipants(UserInfo userInfo, long challengeId,
			Boolean affiliated, long limit, long offset)
			throws DatastoreException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

}
