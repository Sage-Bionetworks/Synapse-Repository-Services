package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
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
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.util.ModelConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ChallengeManagerImpl implements ChallengeManager {
	
	@Autowired
	ChallengeDAO challengeDAO;
	
	@Autowired
	ChallengeTeamDAO challengeTeamDAO;
	
	@Autowired
	AuthorizationManager authorizationManager;
	
	@Autowired
	TeamDAO teamDAO;
	
	@Autowired 
	AccessControlListDAO aclDAO;
	
	private static final AuthorizationStatus NOT_IN_CHALLENGE = 
			AuthorizationStatus.accessDenied("You must be a Challenge participant for this operation.");
	
	private static final AuthorizationStatus NOT_TEAM_ADMIN = 
			AuthorizationStatus.accessDenied("You must be a Team manager for this operation.");
	
	private static final AuthorizationStatus NOT_SELF = 
			AuthorizationStatus.accessDenied("You may not make this request on another user's behalf.");

	public ChallengeManagerImpl() {}
	
	/*
	 * for testing
	 */
	public ChallengeManagerImpl(ChallengeDAO challengeDAO, 
			ChallengeTeamDAO challengeTeamDAO, 
			AuthorizationManager authorizationManager,
			TeamDAO teamDAO,
			AccessControlListDAO aclDAO) {
		this.challengeDAO=challengeDAO;
		this.challengeTeamDAO=challengeTeamDAO;
		this.authorizationManager=authorizationManager;
		this.teamDAO=teamDAO;
		this.aclDAO=aclDAO;
	}
	
	private static void validateChallenge(Challenge challenge) {
		if (challenge.getProjectId()==null) 
			throw new InvalidModelException("Project ID is required.");
		if (challenge.getParticipantTeamId()==null) 
			throw new InvalidModelException("Participant Team ID is required.");
	}

	@WriteTransaction
	@Override
	public Challenge createChallenge(UserInfo userInfo, Challenge challenge) throws DatastoreException, NotFoundException {
		validateChallenge(challenge);
		authorizationManager.canAccess(userInfo, challenge.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.CREATE)
				.checkAuthorizationOrElseThrow();
		return challengeDAO.create(challenge);
	}

	@Override
	public Challenge getChallenge(UserInfo userInfo, long challengeId)
			throws DatastoreException, NotFoundException {
		Challenge challenge = challengeDAO.get(challengeId);
		authorizationManager.canAccess(userInfo, challenge.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
		return challenge;
	}

	@Override
	public Challenge getChallengeByProjectId(UserInfo userInfo, String projectId)
			throws DatastoreException, NotFoundException {
		if (projectId==null) throw new IllegalArgumentException("Project ID is required.");
		authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
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

	@WriteTransaction
	@Override
	public Challenge updateChallenge(UserInfo userInfo, Challenge challenge)
			throws DatastoreException, NotFoundException {
		validateChallenge(challenge);
		authorizationManager.canAccess(userInfo, challenge.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)
				.checkAuthorizationOrElseThrow();
		return challengeDAO.update(challenge);
	}

	@WriteTransaction
	@Override
	public void deleteChallenge(UserInfo userInfo, long challengeId)
			throws DatastoreException, NotFoundException {
		Challenge challenge = challengeDAO.get(challengeId);
		authorizationManager.canAccess(userInfo, challenge.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.DELETE)
				.checkAuthorizationOrElseThrow();
		challengeDAO.delete(challengeId);
	}

	@Override
	public PaginatedIds listParticipantsInChallenge(UserInfo userInfo,
			long challengeId, Boolean affiliated, long limit, long offset)
			throws DatastoreException, NotFoundException {
		Challenge challenge = challengeDAO.get(challengeId);
		authorizationManager.canAccess(userInfo, challenge.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
		PaginatedIds result = new PaginatedIds();
		List<Long> longIds = challengeDAO.listParticipants(challengeId, affiliated, limit, offset);
		List<String> stringIds = new ArrayList<String>(longIds.size());
		for (Long id : longIds) stringIds.add(id.toString());
		result.setResults(stringIds);
		result.setTotalNumberOfResults(challengeDAO.listParticipantsCount(challengeId, affiliated));
		return result;
	}
	
	/*
	 * returns true iff the given user is a Team admin in the given ACL
	 * NOTE:  This is only applicable to Team ACLs as it uses Team admin
	 * permissions for comparison.
	 */
	public static boolean isTeamAdmin(AccessControlList acl, UserInfo userInfo) {
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (!userInfo.getGroups().contains(ra.getPrincipalId())) continue;
			if (ra.getAccessType().containsAll(ModelConstants.TEAM_ADMIN_PERMISSIONS)) return true;
		}
		return false;
	}
	
	/*
	 * Must be a member of the Participant Team and be an admin on the referenced Challenge Team.
	 * 
	 */
	public AuthorizationStatus isRegisteredAndIsAdminForChallengeTeam(UserInfo userInfo, ChallengeTeam challengeTeam) throws NotFoundException {
		if (userInfo.isAdmin()) return AuthorizationStatus.authorized();
		Challenge challenge = challengeDAO.get(Long.parseLong(challengeTeam.getChallengeId()));
		try {
			teamDAO.getMember(challenge.getParticipantTeamId(), userInfo.getId().toString());
		} catch (NotFoundException e) {
			return NOT_IN_CHALLENGE;
		}
		try {
			AccessControlList acl = aclDAO.get(challengeTeam.getTeamId(), ObjectType.TEAM);
			if (!isTeamAdmin(acl, userInfo)) return NOT_TEAM_ADMIN;
		} catch  (NotFoundException e) {
			return NOT_TEAM_ADMIN;
		}

		return AuthorizationStatus.authorized();
	}
	
	private static final int MAX_CHALLENGE_TEAM_MESSAGE_LENGTH = 500;
	
	private static void validateChallengeTeam(ChallengeTeam challengeTeam) {
		if (challengeTeam.getChallengeId()==null) 
			throw new InvalidModelException("Challenge ID is required.");
		if (challengeTeam.getTeamId()==null) 
			throw new InvalidModelException("Team ID is required.");
		if (challengeTeam.getMessage()!=null && challengeTeam.getMessage().length()>MAX_CHALLENGE_TEAM_MESSAGE_LENGTH) {
			throw new InvalidModelException("Message may not exceed "+MAX_CHALLENGE_TEAM_MESSAGE_LENGTH+" characters in length.");
		}
	}

	@WriteTransaction
	@Override
	public ChallengeTeam createChallengeTeam(UserInfo userInfo,
			ChallengeTeam challengeTeam) throws DatastoreException, UnauthorizedException, NotFoundException {
		validateChallengeTeam(challengeTeam);
		isRegisteredAndIsAdminForChallengeTeam(userInfo, challengeTeam).checkAuthorizationOrElseThrow();
		return challengeTeamDAO.create(challengeTeam);
	}

	@Override
	public ChallengeTeamPagedResults listChallengeTeams(UserInfo userInfo,
			long challengeId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		Challenge challenge = challengeDAO.get(challengeId);
		authorizationManager.canAccess(userInfo, challenge.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
		ChallengeTeamPagedResults result = new ChallengeTeamPagedResults();
		result.setResults(challengeTeamDAO.listForChallenge(challengeId, limit, offset));
		result.setTotalNumberOfResults(challengeTeamDAO.listForChallengeCount(challengeId));
		return result;
	}

	@Override
	public PaginatedIds listRegistratableTeams(UserInfo userInfo,
			long challengeId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		Challenge challenge = challengeDAO.get(challengeId);
		authorizationManager.canAccess(userInfo, challenge.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
		PaginatedIds result = new PaginatedIds();
		Long userId = userInfo.getId();
		result.setResults(challengeTeamDAO.listRegistratable(challengeId, userId, limit, offset));
		result.setTotalNumberOfResults(challengeTeamDAO.listRegistratableCount(challengeId, userId));
		return result;
	}

	@WriteTransaction
	@Override
	public ChallengeTeam updateChallengeTeam(UserInfo userInfo,
			ChallengeTeam challengeTeam) throws DatastoreException,
			NotFoundException {
		validateChallengeTeam(challengeTeam);
		isRegisteredAndIsAdminForChallengeTeam(userInfo, challengeTeam).checkAuthorizationOrElseThrow();
		return challengeTeamDAO.update(challengeTeam);
	}

	@WriteTransaction
	@Override
	public void deleteChallengeTeam(UserInfo userInfo, long challengeTeamId)
			throws NotFoundException, DatastoreException {
		ChallengeTeam challengeTeam = challengeTeamDAO.get(challengeTeamId);
		isRegisteredAndIsAdminForChallengeTeam(userInfo, challengeTeam).checkAuthorizationOrElseThrow();
		challengeTeamDAO.delete(challengeTeamId);
	}
	
	/*
	 * Must be the user specified by 'principalId' or be a Synapse Admin
	 */
	public AuthorizationStatus canListSubmissionTeams(UserInfo userInfo, long submitterPrincipalId) {
		if (userInfo.isAdmin() ||userInfo.getId().equals(submitterPrincipalId)) 
			return AuthorizationStatus.authorized();
		return NOT_SELF;
	}

	@Override
	public PaginatedIds listSubmissionTeams(UserInfo userInfo,
			long challengeId, long submitterPrincipalId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		canListSubmissionTeams(userInfo, submitterPrincipalId).checkAuthorizationOrElseThrow();
		PaginatedIds result = new PaginatedIds();
		result.setResults(challengeTeamDAO.listSubmissionTeams(challengeId, submitterPrincipalId, limit, offset));
		result.setTotalNumberOfResults(challengeTeamDAO.listSubmissionTeamsCount(challengeId, submitterPrincipalId));
		return result;
	}

}
