package org.sagebionetworks.evaluation.manager;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE_SUBMISSION;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.PARTICIPATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ_PRIVATE_SUBMISSION;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.SUBMIT;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE_SUBMISSION;

import java.util.List;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.repo.manager.AccessRequirementUtil;
import org.sagebionetworks.repo.manager.PermissionsManagerUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class EvaluationPermissionsManagerImpl implements EvaluationPermissionsManager {

	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	private EvaluationDAO evaluationDAO;
	@Autowired
	private NodeDAO nodeDAO;
	@Autowired
	private AccessRequirementDAO  accessRequirementDAO;
	@Autowired
	private UserManager userManager;

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
		PermissionsManagerUtils.validateACLContent(acl, userInfo, Long.parseLong(evalOwerId));

		final String aclId = aclDAO.create(acl);
		acl = aclDAO.get(aclId, ObjectType.EVALUATION);
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

		final Evaluation eval = getEvaluation(evalId);
		if (!canAccess(userInfo, evalId, CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("User " + userInfo.getIndividualGroup().getId()
					+ " not authorized to change permissions on evaluation " + evalId);
		}

		final Long evalOwnerId = KeyFactory.stringToKey(eval.getOwnerId());
		PermissionsManagerUtils.validateACLContent(acl, userInfo, evalOwnerId);

		aclDAO.update(acl);
		return aclDAO.get(evalId, ObjectType.EVALUATION);
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
		if (!canAccess(userInfo, evalId, CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("User " + userInfo.getIndividualGroup().getId()
					+ " not authorized to change permissions on evaluation " + evalId);
		}
		aclDAO.delete(evalId);
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

		AccessControlList acl = aclDAO.get(evalId, ObjectType.EVALUATION);
		return acl;
	}

	@Override
	public boolean hasAccess(UserInfo userInfo, String evalId, ACCESS_TYPE accessType)
			throws NotFoundException, DatastoreException {
		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation ID cannot be null or empty.");
		}
		if (accessType == null) {
			throw new IllegalArgumentException("Access type cannot be null.");
		}
		return canAccess(userInfo, evalId, accessType);
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
		UserInfo anonymousUser = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		permission.setCanPublicRead(canAccess(anonymousUser, evalId, READ));

		// Other permissions
		permission.setCanView(canAccess(userInfo, evalId, READ));
		permission.setCanEdit(canAccess(userInfo, evalId, UPDATE));
		permission.setCanDelete(canAccess(userInfo, evalId, DELETE));
		permission.setCanChangePermissions(canAccess(userInfo, evalId, CHANGE_PERMISSIONS));
		permission.setCanParticipate(canAccess(userInfo, evalId, PARTICIPATE));
		permission.setCanSubmit(canAccess(userInfo, evalId, SUBMIT));
		permission.setCanViewPrivateSubmissionStatusAnnotations(canAccess(userInfo, evalId, READ_PRIVATE_SUBMISSION));
		permission.setCanEditSubmissionStatuses(canAccess(userInfo, evalId, UPDATE_SUBMISSION));
		permission.setCanDeleteSubmissions(canAccess(userInfo, evalId, DELETE_SUBMISSION));

		return permission;
	}

	/**
	 * Whether the user can access the specified evaluation.
	 */
	private boolean canAccess(final UserInfo userInfo, final String evalId,
			final ACCESS_TYPE accessType) throws NotFoundException {

		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(
				userInfo.getUser().getUserId())) {
			if (!READ.equals(accessType)) {
				return false;
			}
		}
		if (userInfo.isAdmin()) {
			return true;
		}

		boolean canAccess = aclDAO.canAccess(userInfo.getGroups(), evalId, accessType);
		if (canAccess && PARTICIPATE.equals(accessType)) {
			RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
			rod.setId(evalId);
			rod.setType(RestrictableObjectType.EVALUATION);
			List<Long> unmetRequirements = AccessRequirementUtil.unmetAccessRequirementIds(
					userInfo, rod, nodeDAO, accessRequirementDAO);
			canAccess = canAccess && unmetRequirements.isEmpty();
		}
		return canAccess;
	}

	private boolean isEvalOwner(final UserInfo userInfo, final Evaluation eval) {
		String userId = userInfo.getIndividualGroup().getId();
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
}
