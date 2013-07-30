package org.sagebionetworks.evaluation.manager;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.PARTICIPATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
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

	 // TODO: To be removed once the web ui (SWC-728) is ready
	private boolean turnOffAcl = true;

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

		if (turnOffAcl) {
			acl = addPublicReadParticipate(acl);
		}

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
			throws NotFoundException, DatastoreException, ACLInheritanceException {
		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation ID cannot be null or empty.");
		}
		try {
			AccessControlList acl = aclDAO.get(evalId, ObjectType.EVALUATION);
			return acl;
		} catch (NotFoundException e) {
			AccessControlList acl = backfill(userInfo, evalId);
			if (acl == null) {
				throw e;
			}
			return acl;
		}
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
		permission.setCanChangePermissions(canAccess(userInfo, evalId, CHANGE_PERMISSIONS));
		permission.setCanDelete(canAccess(userInfo, evalId, DELETE));
		permission.setCanEdit(canAccess(userInfo, evalId, UPDATE));
		permission.setCanParticipate(canAccess(userInfo, evalId, PARTICIPATE));
		permission.setCanView(canAccess(userInfo, evalId, READ));

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

		// A temporary flag to let bypass ACLs before the web portal is ready
		if (turnOffAcl) {
			// TODO: To be removed once web ui is in place
			// Anyone can read
			if (ACCESS_TYPE.READ.equals(accessType)) {
				return Boolean.TRUE;
			}
			// Any registered user, once logging in, can participate
			if (!AuthorizationConstants.ANONYMOUS_USER_ID.equals(
					userInfo.getUser().getId())) {
				if (ACCESS_TYPE.PARTICIPATE.equals(accessType)) {
					return Boolean.TRUE;
				}
			}
		}

		return aclDAO.canAccess(userInfo.getGroups(), evalId, accessType);
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

	private AccessControlList backfill(UserInfo userInfo, String evalId)
			throws NotFoundException {

		// Make sure only the owner and admin can backfill
		final Evaluation eval = getEvaluation(evalId);
		if (!userInfo.isAdmin()) {
			if (!isEvalOwner(userInfo, eval)) {
				return null;
			}
		}

		// Backfill the evaluation owner
		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(12);
		accessSet.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		accessSet.add(ACCESS_TYPE.CREATE);
		accessSet.add(ACCESS_TYPE.DELETE);
		accessSet.add(ACCESS_TYPE.PARTICIPATE);
		accessSet.add(ACCESS_TYPE.READ);
		accessSet.add(ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		accessSet.add(ACCESS_TYPE.UPDATE);

		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(accessSet);
		String userId = eval.getOwnerId();
		ra.setPrincipalId(Long.parseLong(userId));

		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		raSet.add(ra);

		AccessControlList acl = new AccessControlList();
		acl.setId(evalId);
		acl.setCreationDate(new Date());
		acl.setResourceAccess(raSet);

		acl = addPublicReadParticipate(acl);

		final String aclId = aclDAO.create(acl);
		acl = aclDAO.get(aclId, ObjectType.EVALUATION);
		return acl;
	}

	// TODO: This is a temporary method that goes with the 'turnOffAcl' flag
	private AccessControlList addPublicReadParticipate(final AccessControlList acl) {

		if (turnOffAcl) {

			// The public can read (but not participate)
			Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>();
			accessSet.add(ACCESS_TYPE.READ);
			ResourceAccess ra = new ResourceAccess();
			ra.setAccessType(accessSet);
			String publicUserId = userManager.getDefaultUserGroup(DEFAULT_GROUPS.PUBLIC).getId();
			ra.setPrincipalId(Long.parseLong(publicUserId));
			final Set<ResourceAccess> raSet = acl.getResourceAccess();
			raSet.add(ra);

			// Authenticated users can read and participate
			accessSet = new HashSet<ACCESS_TYPE>();
			accessSet.add(ACCESS_TYPE.READ);
			accessSet.add(ACCESS_TYPE.PARTICIPATE);
			ra = new ResourceAccess();
			ra.setAccessType(accessSet);
			String userId = userManager.getDefaultUserGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS).getId();
			ra.setPrincipalId(Long.parseLong(userId));
			raSet.add(ra);
		}

		return acl;
	}
}
