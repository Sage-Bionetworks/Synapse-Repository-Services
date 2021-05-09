package org.sagebionetworks.repo.manager.evaluation;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Manages evaluation permissions.
 */
public interface EvaluationPermissionsManager {

	/**
	 * Creates a new ACL.
	 */
	public AccessControlList createAcl(UserInfo userInfo, AccessControlList acl)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException;

	/**
	 * Updates with the given ACL.
	 */
	public AccessControlList updateAcl(UserInfo userInfo, AccessControlList acl)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException;

	/**
	 * Deletes the ACL of the specified evaluation.
	 */
	public void deleteAcl(UserInfo userInfo, String evalId)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException;

	/**
	 * Gets the access control list (ACL) governing the given evaluation.
	 */
	public AccessControlList getAcl(UserInfo userInfo, String evalId)
			throws NotFoundException, DatastoreException;

	/**
	 * Whether the user has the access to the specified evaluation.
	 */
	public AuthorizationStatus hasAccess(UserInfo userInfo, String evalId, ACCESS_TYPE accessType)
			throws NotFoundException, DatastoreException;
	
	/**
	 * Whether the user has the access to the specified list of evaluations
	 */
	AuthorizationStatus hasAccess(UserInfo userInfo, ACCESS_TYPE accessType, List<String> evaluationIds)
			throws NotFoundException, DatastoreException;

	/**
	 * Gets the user permissions for an evaluation.
	 */
	public UserEvaluationPermissions getUserPermissionsForEvaluation(UserInfo userInfo, String evalId)
			throws NotFoundException, DatastoreException;

	/**
	 * User must have submit permission and be a member of the given team
	 * 
	 * @param userInfo
	 * @param evaluationId
	 * @param teamId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public AuthorizationStatus canCheckTeamSubmissionEligibility(UserInfo userInfo, String evaluationId, String teamId) throws DatastoreException, NotFoundException;

	/*
	 * Return true if and only if the given Docker Repository name is in a Submission under some Evaluation 
	 * in which the given user (represented by a list of principalIds) has the given access type.
	 */
	boolean isDockerRepoNameInEvaluationWithAccess(String dockerRepoName, Set<Long> principalIds, ACCESS_TYPE accessType);

}
