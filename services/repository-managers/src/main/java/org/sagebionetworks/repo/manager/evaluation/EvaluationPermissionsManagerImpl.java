package org.sagebionetworks.repo.manager.evaluation;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE_SUBMISSION;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ_PRIVATE_SUBMISSION;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.SUBMIT;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE_SUBMISSION;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.repo.manager.AccessControlListManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.util.ModelConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EvaluationPermissionsManagerImpl implements EvaluationPermissionsManager {

	public static final String ACL_DOES_NOT_EXIST = "ACL for '%s' of type '%s' does not exist";

	private final EvaluationDAO evaluationDAO;
	private final UserManager userManager;
	private final AccessControlListManager aclManager;
	private final SubmissionDAO submissionDAO;

	@Autowired
	public EvaluationPermissionsManagerImpl(EvaluationDAO evaluationDAO,
											UserManager userManager, AccessControlListManager aclManager,
											SubmissionDAO submissionDAO) {
		this.evaluationDAO = evaluationDAO;
		this.userManager = userManager;
		this.aclManager = aclManager;
		this.submissionDAO = submissionDAO;
	}

	@Override
	public AccessControlList createAcl(UserInfo userInfo, AccessControlList acl)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException {

		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (acl == null) {
			throw new IllegalArgumentException("ACL cannot be null.");
		}

		final String evalId = acl.getId();
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("ACL's evaluation ID must not be null or empty.");
		}

		final Evaluation eval = getEvaluation(evalId);
		if (!isEvalOwner(userInfo, eval)) {
			throw new UnauthorizedException("Only the owner of evaluation " + evalId + " can create ACL.");
		}

		final String evalOwerId = eval.getOwnerId();

		aclManager.create(userInfo, acl, ObjectType.EVALUATION, Long.parseLong(evalOwerId));
		acl = aclManager.getAcl(evalId, ObjectType.EVALUATION).orElseThrow(() ->
				new NotFoundException(String.format(ACL_DOES_NOT_EXIST, evalId, ObjectType.EVALUATION)));
		return acl;
	}

	@Override
	public AccessControlList updateAcl(UserInfo userInfo, AccessControlList acl)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException {

		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (acl == null) {
			throw new IllegalArgumentException("ACL cannot be null.");
		}

		final String evalId = acl.getId();
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("ACL's evaluation ID must not be null or empty.");
		}

		if(acl.getResourceAccess() == null) {
			acl.setResourceAccess(Collections.emptySet());
		}

		final Evaluation eval = getEvaluation(evalId);
		hasAccess(userInfo, evalId, CHANGE_PERMISSIONS).checkAuthorizationOrElseThrow();

		final Long evalOwnerId = KeyFactory.stringToKey(eval.getOwnerId());

		validateUserGroupPermissions(acl.getResourceAccess(), userInfo);

		aclManager.update(userInfo, acl, ObjectType.EVALUATION, evalOwnerId);
		return aclManager.getAcl(evalId, ObjectType.EVALUATION).orElseThrow(() ->
				new NotFoundException(String.format(ACL_DOES_NOT_EXIST, evalId, ObjectType.EVALUATION)));
	}

	@Override
	public void deleteAcl(UserInfo userInfo, String evalId)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException {
		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation Id cannot be null or empty.");
		}
		if (!hasAccess(userInfo, evalId, CHANGE_PERMISSIONS).isAuthorized()) {
			throw new UnauthorizedException("User " + userInfo.getId().toString()
					+ " not authorized to change permissions on evaluation " + evalId);
		}
		aclManager.delete(evalId, ObjectType.EVALUATION);
	}

	@Override
	public AccessControlList getAcl(UserInfo userInfo, String evalId)
			throws NotFoundException, DatastoreException {
		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation ID cannot be null or empty.");
		}

		return aclManager.getAcl(evalId, ObjectType.EVALUATION).orElseThrow(() ->
				new NotFoundException(String.format(ACL_DOES_NOT_EXIST, evalId, ObjectType.EVALUATION)));
	}

	/**
	 * Whether the user has the access to the specified evaluation.
	 * Has the same logic as 'hasAccess' but throws informative exception if the answer is false.
	 */
	@Override
	public AuthorizationStatus hasAccess(UserInfo userInfo, String evalId, ACCESS_TYPE accessType)
			throws NotFoundException, DatastoreException {
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation ID cannot be null or empty.");
		}

		return hasAccess(userInfo, accessType).orElseGet(() -> {
			if (!aclManager.canAccess(userInfo.getGroups(), evalId, ObjectType.EVALUATION, accessType)) {
				return AuthorizationStatus.accessDenied("User lacks "+accessType+" access to Evaluation "+evalId);
			}
			
			return AuthorizationStatus.authorized();
		});
	}
	
	@Override
	public AuthorizationStatus hasAccess(UserInfo userInfo, ACCESS_TYPE accessType, List<String> evaluationIds)
			throws NotFoundException, DatastoreException {
		if (evaluationIds == null || evaluationIds.isEmpty()) {
			throw new IllegalArgumentException("The set of evaluation ids cannot be null or empty.");
		}
		
		Set<Long> benefactorIds = evaluationIds.stream()
				.map((id) -> KeyFactory.stringToKey(id))
				.collect(Collectors.toSet());
		
		return hasAccess(userInfo, accessType).orElseGet(() -> {
			Set<Long> accessibleSet = aclManager.getAccessibleBenefactors(userInfo, ObjectType.EVALUATION, benefactorIds, accessType);
			
			if (accessibleSet.size() != benefactorIds.size()) {
				return AuthorizationStatus.accessDenied("User lacks "+accessType+" access to all the evaluations in the set.");
			}
			
			return AuthorizationStatus.authorized();
		});
		
	}
	
	/**
	 * Checks if the user is an admin and has unconditional access or if it's anonymous so without any access 
	 */
	private Optional<AuthorizationStatus> hasAccess(UserInfo userInfo, ACCESS_TYPE accessType) {
		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (accessType == null) {
			throw new IllegalArgumentException("Access type cannot be null.");
		}
		if (userInfo.isAdmin()) {
			return Optional.of(AuthorizationStatus.authorized());
		}

		if (isAnonymousWithNonReadAccess(userInfo, accessType)) {
			return Optional.of(AuthorizationStatus.accessDenied("Anonymous user is not allowed to access Evaluation."));
		}
		
		return Optional.empty();
	}

	@Override
	public UserEvaluationPermissions getUserPermissionsForEvaluation(UserInfo userInfo, String evalId)
			throws NotFoundException, DatastoreException {

		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation ID cannot be null or empty.");
		}

		UserEvaluationPermissions permission = new UserEvaluationPermissions();

		final Evaluation eval = getEvaluation(evalId);
		permission.setOwnerPrincipalId(KeyFactory.stringToKey(eval.getOwnerId()));

		// Public read
		UserInfo anonymousUser = userManager.getUserInfo(userInfo.getRealmAnonymousUserId());
		permission.setCanPublicRead(hasAccess(anonymousUser, evalId, READ).isAuthorized());

		// Other permissions
		permission.setCanView(hasAccess(userInfo, evalId, READ).isAuthorized());
		permission.setCanEdit(hasAccess(userInfo, evalId, UPDATE).isAuthorized());
		permission.setCanDelete(hasAccess(userInfo, evalId, DELETE).isAuthorized());
		permission.setCanChangePermissions(hasAccess(userInfo, evalId, CHANGE_PERMISSIONS).isAuthorized());
		permission.setCanSubmit(hasAccess(userInfo, evalId, SUBMIT).isAuthorized());
		permission.setCanViewPrivateSubmissionStatusAnnotations(hasAccess(userInfo, evalId, READ_PRIVATE_SUBMISSION).isAuthorized());
		permission.setCanEditSubmissionStatuses(hasAccess(userInfo, evalId, UPDATE_SUBMISSION).isAuthorized());
		permission.setCanDeleteSubmissions(hasAccess(userInfo, evalId, DELETE_SUBMISSION).isAuthorized());

		return permission;
	}

	/*
	 * Ensures that public/anonymous users are not given more permissions than they should be allowed to have on an evaluation
	 */
	private static void validateUserGroupPermissions(Set<ResourceAccess> resourceAccess, UserInfo userInfo) {
		for (ResourceAccess ra : resourceAccess) {
			if (ra.getPrincipalId().equals(userInfo.getRealmPublicUsersId())) {
				if (!CollectionUtils.isSubCollection(ra.getAccessType(), ModelConstants.EVALUATION_PUBLIC_MAXIMUM_ACCESS_PERMISSIONS)) {
					throw new InvalidModelException("Public users may only have read access on an evaluation.");
				}
			} else if (ra.getPrincipalId().equals(userInfo.getRealmAnonymousUserId())) {
				// Note, we need to check all anonymous users (from all realms) are rejected
				// however anonymous users from other realms will be addressed by the constraint
				// that all ACL entries must be from the same realm
				// (Ditto for authenticted users and the public group.)
				//
				// PLFM-9438 TODO Anonymous should not be in an ACL AT ALL
				if (!CollectionUtils.isSubCollection(ra.getAccessType(), ModelConstants.EVALUATION_ANONYMOUS_MAXIMUM_ACCESS_PERMISSIONS)) {
					throw new InvalidModelException("Anonymous users may only have read access on an evaluation.");
				}
			} else if (ra.getPrincipalId().equals(userInfo.getRealmAuthenticatedUsersId())) {
				if (!CollectionUtils.isSubCollection(ra.getAccessType(), ModelConstants.EVALUATION_AUTH_USER_MAXIMUM_ACCESS_PERMISSIONS)) {
					throw new InvalidModelException("Only read access on an evaluation can be granted to all authenticated Synapse users.");
				}
			}
		}
	}
	
	private static boolean isAnonymousWithNonReadAccess(UserInfo userInfo, ACCESS_TYPE accessType) {
		return userInfo.isUserAnonymous() && !READ.equals(accessType);
	}

	private boolean isEvalOwner(final UserInfo userInfo, final Evaluation eval) {
		String userId = userInfo.getId().toString();
		String evalOwnerId = eval.getOwnerId();
		if (userId != null && evalOwnerId != null && userId.equals(evalOwnerId)) {
			return true;
		}
		return false;
	}

	private Evaluation getEvaluation(final String evalId) throws NotFoundException {
		try {
			return evaluationDAO.get(evalId);
		}
		catch (NotFoundException e) {
			// Rethrow with a more specific message
			throw new NotFoundException("Evaluation of ID " + evalId + " does not exist yet.");
		}
	}
	
	/**
	 * User must have submit permission and be a member of the given team
	 * 
	 * @param userInfo
	 * @param evaluationId
	 * @param teamId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Override
	public AuthorizationStatus canCheckTeamSubmissionEligibility(UserInfo userInfo, String evaluationId, String teamId) throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) return AuthorizationStatus.authorized();
		if (!userInfo.getGroups().contains(Long.parseLong(teamId))) {
			return AuthorizationStatus.accessDenied("Requester is not a member of the Submission Team.");
		}
		return hasAccess(userInfo, evaluationId, ACCESS_TYPE.SUBMIT);
	}
	
	@Override
	public boolean isDockerRepoNameInEvaluationWithAccess(String dockerRepoName, Set<Long> principalIds, ACCESS_TYPE accessType) {
		return submissionDAO.isDockerRepoNameInAnyEvaluationWithAccess(dockerRepoName, principalIds, accessType);
	}

}
