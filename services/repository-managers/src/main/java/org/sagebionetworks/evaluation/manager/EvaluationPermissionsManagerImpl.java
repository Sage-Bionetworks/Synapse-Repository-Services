package org.sagebionetworks.evaluation.manager;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.PARTICIPATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;

import java.util.Collection;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.repo.manager.PermissionsManagerUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
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
		if (!canAccess(userInfo, eval, CHANGE_PERMISSIONS)) {
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
			throws NotFoundException, DatastoreException, ACLInheritanceException {
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

		UserInfo anonymousUser = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		permission.setCanPublicRead(aclDAO.canAccess(anonymousUser.getGroups(), evalId, READ));

		// Admin gets all
		if (userInfo.isAdmin()) {
			permission.setCanChangePermissions(true);
			permission.setCanDelete(true);
			permission.setCanEdit(true);
			permission.setCanParticipate(true);
			permission.setCanView(true);
			return permission;
		}

		// Owner gets all except for participate
		if (isEvalOwner(userInfo, eval)) {
			permission.setCanChangePermissions(true);
			permission.setCanDelete(true);
			permission.setCanEdit(true);
			permission.setCanParticipate(false);
			permission.setCanView(true);
			return permission;
		}

		// Other users
		permission.setCanView(aclDAO.canAccess(userInfo.getGroups(), evalId, READ));
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userInfo.getUser().getUserId())) {
			permission.setCanChangePermissions(false);
			permission.setCanDelete(false);
			permission.setCanEdit(false);
			permission.setCanParticipate(false);
		} else {
			Collection<UserGroup> userGroups = userInfo.getGroups();
			permission.setCanChangePermissions(aclDAO.canAccess(userGroups, evalId, CHANGE_PERMISSIONS));
			permission.setCanDelete(aclDAO.canAccess(userGroups, evalId, DELETE));
			permission.setCanEdit(aclDAO.canAccess(userGroups, evalId, UPDATE));
			permission.setCanParticipate(aclDAO.canAccess(userGroups, evalId, PARTICIPATE));
		}
		return permission;
	}

	/**
	 * Whether the user can access the specified evaluation.
	 */
	private boolean canAccess(final UserInfo userInfo, final String evalId,
			final ACCESS_TYPE accessType) throws NotFoundException {
		Boolean canAccess = canAccess(userInfo, accessType);
		if (canAccess != null) {
			return canAccess.booleanValue();
		}
		Evaluation eval = getEvaluation(evalId);
		return canAccess(userInfo, eval, accessType);
	}

	/**
	 * Whether the user can access the specified evaluation.
	 */
	private boolean canAccess(final UserInfo userInfo, final Evaluation eval, final ACCESS_TYPE accessType) {
		Boolean canAccess = canAccess(userInfo, accessType);
		if (canAccess != null) {
			return canAccess.booleanValue();
		}
		if (isEvalOwner(userInfo, eval)) {
			if (!ACCESS_TYPE.PARTICIPATE.equals(accessType)) {
				return true;
			}
		}
		return aclDAO.canAccess(userInfo.getGroups(), eval.getId(), accessType);
	}

	// Shortcuts that do not involve database calls
	private Boolean canAccess(final UserInfo userInfo, final ACCESS_TYPE accessType) {
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userInfo.getUser().getUserId())) {
			// A shortcut. Anonymous user at most has read access.
			if (READ != accessType) {
				return Boolean.FALSE;
			}
		}
		if (userInfo.isAdmin()) {
			return Boolean.TRUE;
		}
		return null;
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

	private boolean isEvalOwner(final UserInfo userInfo, final Evaluation eval) {
		String userId = userInfo.getIndividualGroup().getId();
		String evalOwnerId = eval.getOwnerId();
		if (userId != null && evalOwnerId != null && userId.equals(evalOwnerId)) {
			return true;
		}
		return false;
	}
}
