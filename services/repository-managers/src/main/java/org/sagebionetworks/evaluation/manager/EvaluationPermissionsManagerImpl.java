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
import org.sagebionetworks.repo.manager.AuthorizationManager;
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
import org.sagebionetworks.repo.model.NodeDAO;
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
	private NodeDAO nodeDao;

	@Autowired
	private AuthorizationManager authorizationManager;
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

		String evalId = acl.getId();
		Evaluation eval = evaluationDAO.get(evalId);
		if (eval == null) {
			throw new IllegalArgumentException("Evaluation of ID " + evalId + " does not exist yet.");
		}

		String entityId = eval.getContentSource();
		if (!authorizationManager.canAccess(userInfo, entityId, CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("User " + userInfo.getIndividualGroup().getId()
					+ " not authorized on entity " + entityId + " to create ACL for evaluation " + evalId);
		}

		Long nodeOwnerId = nodeDao.getCreatedBy(entityId);
		PermissionsManagerUtils.validateACLContent(acl, userInfo, nodeOwnerId);

		String aclId = aclDAO.create(acl);
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

		String evalId = acl.getId();
		Evaluation eval = evaluationDAO.get(evalId);
		if (eval == null) {
			throw new IllegalArgumentException("Evaluation of ID " + evalId + " does not exist yet.");
		}

		Long evalOwnerId = KeyFactory.stringToKey(eval.getOwnerId());
		PermissionsManagerUtils.validateACLContent(acl, userInfo, evalOwnerId);

		if (!canAccess(userInfo, evalId, CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("User " + userInfo.getIndividualGroup().getId()
					+ " not authorized to change permissions on evaluation " + evalId);
		}

		aclDAO.update(acl);
		acl = aclDAO.get(evalId, ObjectType.EVALUATION);
		return acl;
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

		final Evaluation eval = evaluationDAO.get(evalId);
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

	private boolean canAccess(UserInfo userInfo, String evalId, ACCESS_TYPE accessType) {
		// TODO: Does it make sense to combine with AuthorizationManagerImpl.canAccess()?
		// For evaluations, we don't check for benefactor and we don't check canDown().
		if (userInfo.isAdmin()) {
			return true;
		}
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userInfo.getUser().getUserId())) {
			if (READ != accessType) {
				return false;
			}
		}
		return aclDAO.canAccess(userInfo.getGroups(), evalId, accessType);
	}
}
