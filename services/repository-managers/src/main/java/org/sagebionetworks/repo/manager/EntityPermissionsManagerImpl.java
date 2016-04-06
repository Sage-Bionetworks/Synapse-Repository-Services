package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_SETTINGS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CREATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DOWNLOAD;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPLOAD;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.MODERATE;

import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.project.ExternalSyncSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityPermissionsManagerImpl implements EntityPermissionsManager {

	private static final Long TRASH_FOLDER_ID = Long.parseLong(
			StackConfiguration.getTrashFolderEntityIdStatic());

	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	private AccessRequirementDAO  accessRequirementDAO;
	@Autowired
	private NodeInheritanceManager nodeInheritanceManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private ProjectSettingsManager projectSettingsManager;
	@Autowired
	private StackConfiguration configuration;


	@Override
	public AccessControlList getACL(String nodeId, UserInfo userInfo) throws NotFoundException, DatastoreException, ACLInheritanceException {
		// Get the id that this node inherits its permissions from
		String benefactor = nodeInheritanceManager.getBenefactor(nodeId);		
		// This is a fix for PLFM-398
		if (!benefactor.equals(nodeId)) {
			throw new ACLInheritanceException("Cannot access the ACL of a node that inherits it permissions. This node inherits its permissions from: "+benefactor, benefactor);
		}
		AccessControlList acl = aclDAO.get(nodeId, ObjectType.ENTITY);
		return acl;
	}
		
	@WriteTransaction
	@Override
	public AccessControlList updateACL(AccessControlList acl, UserInfo userInfo) throws NotFoundException, DatastoreException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		String rId = acl.getId();
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (!benefactor.equals(rId)) throw new UnauthorizedException("Cannot update ACL for a resource which inherits its permissions.");
		// check permissions of user to change permissions for the resource
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				hasAccess(rId, CHANGE_PERMISSIONS, userInfo));
		// validate content
		Long ownerId = nodeDao.getCreatedBy(acl.getId());
		PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
		aclDAO.update(acl, ObjectType.ENTITY);
		acl = aclDAO.get(acl.getId(), ObjectType.ENTITY);
		return acl;
	}

	@WriteTransaction
	@Override
	public AccessControlList overrideInheritance(AccessControlList acl, UserInfo userInfo) throws NotFoundException, DatastoreException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		String rId = acl.getId();
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (benefactor.equals(rId)) throw new UnauthorizedException("Resource already has an ACL.");
		// check permissions of user to change permissions for the resource
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				hasAccess(benefactor, CHANGE_PERMISSIONS, userInfo));
		
		// validate content
		Long ownerId = nodeDao.getCreatedBy(acl.getId());
		PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
		Node node = nodeDao.getNode(rId);
		// Before we can update the ACL we must grab the lock on the node.
		nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());
		// set permissions 'benefactor' for resource and all resource's descendants to resource
		nodeInheritanceManager.setNodeToInheritFromItself(rId);
		// persist acl and return
		aclDAO.create(acl, ObjectType.ENTITY);
		acl = aclDAO.get(acl.getId(), ObjectType.ENTITY);
		return acl;
	}

	@WriteTransaction
	@Override
	public AccessControlList restoreInheritance(String rId, UserInfo userInfo) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// check permissions of user to change permissions for the resource
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				hasAccess(rId, CHANGE_PERMISSIONS, userInfo));
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (!benefactor.equals(rId)) throw new UnauthorizedException("Resource already inherits its permissions.");	

		// if parent is root, than can't inherit, must have own ACL
		if (nodeDao.isNodesParentRoot(rId)) throw new UnauthorizedException("Cannot restore inheritance for resource which has no parent.");

		// Before we can update the ACL we must grab the lock on the node.
		Node node = nodeDao.getNode(rId);
		nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());
		nodeInheritanceManager.setNodeToInheritFromNearestParent(rId);
		
		// delete access control list
		aclDAO.delete(rId, ObjectType.ENTITY);
		
		// now find the newly governing ACL
		benefactor = nodeInheritanceManager.getBenefactor(rId);
		
		return aclDAO.get(benefactor, ObjectType.ENTITY);
	}	
	
	@WriteTransaction
	@Override
	public AccessControlList applyInheritanceToChildren(String parentId, UserInfo userInfo) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// check permissions of user to change permissions for the resource
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				hasAccess(parentId,CHANGE_PERMISSIONS, userInfo));
		
		// Before we can update the ACL we must grab the lock on the node.
		Node node = nodeDao.getNode(parentId);
		nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());

		String benefactorId = nodeInheritanceManager.getBenefactor(parentId);
		applyInheritanceToChildrenHelper(parentId, benefactorId, userInfo);

		// return governing parent ACL
		return aclDAO.get(nodeInheritanceManager.getBenefactor(parentId), ObjectType.ENTITY);
	}
	
	private void applyInheritanceToChildrenHelper(final String parentId, final String benefactorId, UserInfo userInfo)
			throws NotFoundException, DatastoreException, ConflictingUpdateException {
		// Get all of the child nodes, sorted by id (to prevent deadlock)
		List<String> children = nodeDao.getChildrenIdsAsList(parentId);
		// Update each node
		for(String idToChange: children) {
			// recursively apply to children
			applyInheritanceToChildrenHelper(idToChange, benefactorId, userInfo);
			// must be authorized to modify permissions
			if (hasAccess(idToChange, CHANGE_PERMISSIONS, userInfo).getAuthorized()) {
				// delete child ACL, if present
				if (hasLocalACL(idToChange)) {
					// Before we can update the ACL we must grab the lock on the node.
					Node node = nodeDao.getNode(idToChange);
					nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());
					
					// delete ACL
					aclDAO.delete(idToChange, ObjectType.ENTITY);
				}								
				// set benefactor ACL
				nodeInheritanceManager.addBeneficiary(idToChange, benefactorId);
			}
		}
	}
	
	private boolean isCertifiedUserOrFeatureDisabled(UserInfo userInfo) {
		Boolean pa = configuration.getDisableCertifiedUser();
		Boolean featureIsDisabled = false;
		try {
			featureIsDisabled = pa;
		} catch (NullPointerException npe) {
			featureIsDisabled = false;
		}
		if (featureIsDisabled) return true;
		return AuthorizationUtils.isCertifiedUser(userInfo);
	}
	
	@Override
	public AuthorizationStatus canCreate(Node node, UserInfo userInfo) 
			throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) {
			return AuthorizationManagerUtil.AUTHORIZED;
		}
		String parentId = node.getParentId();
		if (parentId == null) {
			return AuthorizationManagerUtil.accessDenied("Cannot create a entity having no parent.");
		}

		if (!isCertifiedUserOrFeatureDisabled(userInfo) && !EntityType.project.equals(node.getNodeType())) 
			return AuthorizationManagerUtil.accessDenied("Only certified users may create content in Synapse.");
		
		return certifiedUserHasAccess(parentId, CREATE, userInfo);
	}

	@Override
	public AuthorizationStatus canChangeSettings(Node node, UserInfo userInfo) throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) {
			return AuthorizationManagerUtil.AUTHORIZED;
		}

		if (!isCertifiedUserOrFeatureDisabled(userInfo)) {
			return AuthorizationManagerUtil.accessDenied("Only certified users may change node settings.");
		}

		// the creator always has change settings permissions
		if (node.getCreatedByPrincipalId().equals(userInfo.getId())) {
			return AuthorizationManagerUtil.AUTHORIZED;
		}

		return certifiedUserHasAccess(node.getId(), ACCESS_TYPE.CHANGE_SETTINGS, userInfo);
	}
	
	/**
	 * 
	 * @param resource the resource of interest
	 * @param user
	 * @param accessType
	 * @return
	 */
	@Override
	public AuthorizationStatus hasAccess(String entityId, ACCESS_TYPE accessType, UserInfo userInfo)
			throws NotFoundException, DatastoreException  {
		
		if (!userInfo.isAdmin() && 
			!isCertifiedUserOrFeatureDisabled(userInfo) && 
				(accessType==CREATE ||
				(accessType==UPDATE && !nodeDao.getNodeTypeById(entityId).equals(EntityType.project))))
			return AuthorizationManagerUtil.accessDenied("Only certified users may create or update content in Synapse.");
		
		return certifiedUserHasAccess(entityId, accessType, userInfo);
	}
		
	/**
	 * Answers the authorization check  _without_ checking whether the user is a Certified User.
	 * In other words, says whether the user _would_ be authorized for the requested access
	 * if they _were_ a Certified User.  This feature is important to the Web Portal which
	 * enables certain features, though unauthorized for the user at the moment, redirecting them
	 * to certification before allowing them through.
	 * 
	 * @param entityId
	 * @param accessType
	 * @param userInfo
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public AuthorizationStatus certifiedUserHasAccess(String entityId, ACCESS_TYPE accessType, UserInfo userInfo)
				throws NotFoundException, DatastoreException  {
		// In the case of the trash can, throw the EntityInTrashCanException
		// The only operations allowed over the trash can is CREATE (i.e. moving
		// items into the trash can) and DELETE (i.e. purging the trash).
		final String benefactor = nodeInheritanceManager.getBenefactor(entityId);
		if (TRASH_FOLDER_ID.equals(KeyFactory.stringToKey(benefactor))
				&& !CREATE.equals(accessType)
				&& !DELETE.equals(accessType)) {
			throw new EntityInTrashCanException("Entity " + entityId + " is in trash can.");
		}
		
		// Can download
		if (accessType == DOWNLOAD) {
			return canDownload(userInfo, entityId);
		}
		// Can upload
		if (accessType == UPLOAD) {
			return canUpload(userInfo, entityId);
		}
		// Anonymous can at most READ
		if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			if (accessType != ACCESS_TYPE.READ) {
				return AuthorizationManagerUtil.
						accessDenied("Anonymous users have only READ access permission.");
			}
		}
		// Admin
		if (userInfo.isAdmin()) {
			return AuthorizationManagerUtil.AUTHORIZED;
		}
		if (aclDAO.canAccess(userInfo.getGroups(), benefactor, ObjectType.ENTITY, accessType)) {
			return AuthorizationManagerUtil.AUTHORIZED;
		} else {
			return AuthorizationManagerUtil.
					accessDenied("You do not have "+accessType+" permission for the requested entity.");
		}
	}

	/**
	 * Get the permission benefactor of an entity.
	 * @throws DatastoreException 
	 */
	@Override
	public String getPermissionBenefactor(String nodeId, UserInfo userInfo) throws NotFoundException, DatastoreException {
		return nodeInheritanceManager.getBenefactor(nodeId);
	}

	@Override
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo,	String entityId)
			throws NotFoundException, DatastoreException {

		UserEntityPermissions permissions = new UserEntityPermissions();
		permissions.setCanAddChild(hasAccess(entityId, CREATE, userInfo).getAuthorized());
		permissions.setCanCertifiedUserAddChild(certifiedUserHasAccess(entityId, CREATE, userInfo).getAuthorized());
		permissions.setCanChangePermissions(hasAccess(entityId, CHANGE_PERMISSIONS, userInfo).getAuthorized());
		permissions.setCanChangeSettings(hasAccess(entityId, CHANGE_SETTINGS, userInfo).getAuthorized());
		permissions.setCanDelete(hasAccess(entityId, DELETE, userInfo).getAuthorized());
		permissions.setCanEdit(hasAccess(entityId, UPDATE, userInfo).getAuthorized());
		permissions.setCanCertifiedUserEdit(certifiedUserHasAccess(entityId, UPDATE, userInfo).getAuthorized());
		permissions.setCanView(hasAccess(entityId, READ, userInfo).getAuthorized());
		permissions.setCanDownload(canDownload(userInfo, entityId).getAuthorized());
		permissions.setCanUpload(canUpload(userInfo, entityId).getAuthorized());
		permissions.setCanModerate(hasAccess(entityId, MODERATE, userInfo).getAuthorized());

		Node node = nodeDao.getNode(entityId);
		permissions.setOwnerPrincipalId(node.getCreatedByPrincipalId());
		
		permissions.setIsCertifiedUser(AuthorizationUtils.isCertifiedUser(userInfo));

		UserInfo anonymousUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		permissions.setCanPublicRead(hasAccess(entityId, READ, anonymousUser).getAuthorized());

		final boolean parentIsRoot = nodeDao.isNodesParentRoot(entityId);
		if (userInfo.isAdmin()) {
			permissions.setCanEnableInheritance(!parentIsRoot);
		} else if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			permissions.setCanEnableInheritance(false);
		} else {
			permissions.setCanEnableInheritance(!parentIsRoot && permissions.getCanChangePermissions());
		}
		return permissions;
	}

	@Override
	public boolean hasLocalACL(String resourceId) {
		try {
			return nodeInheritanceManager.getBenefactor(resourceId).equals(resourceId);
		} catch (Exception e) {
			return false;
		}
	}

	private AuthorizationStatus canDownload(UserInfo userInfo, final String nodeId)
			throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) return AuthorizationManagerUtil.AUTHORIZED;
		if (!agreesToTermsOfUse(userInfo)) return AuthorizationManagerUtil.
					accessDenied("You have not yet agreed to the Synapse Terms of Use.");
		
		// if there are any unmet access requirements return false
		List<String> nodeAncestorIds = AccessRequirementUtil.getNodeAncestorIds(nodeDao, nodeId, false);

		List<Long> accessRequirementIds = AccessRequirementUtil.unmetDownloadAccessRequirementIdsForEntity(
				userInfo, nodeId, nodeAncestorIds, nodeDao, accessRequirementDAO);
		if (accessRequirementIds.isEmpty()) {
			return AuthorizationManagerUtil.AUTHORIZED;
		} else {
			return AuthorizationManagerUtil
					.accessDenied("There are unmet access requirements that must be met to read content in the requested container.");
		}
	}

	private AuthorizationStatus canUpload(UserInfo userInfo, final String parentOrNodeId)
			throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) return AuthorizationManagerUtil.AUTHORIZED;
		if (!agreesToTermsOfUse(userInfo)) return AuthorizationManagerUtil.
				accessDenied("You have not yet agreed to the Synapse Terms of Use.");
		
		ExternalSyncSetting projectSettingForNode = projectSettingsManager.getProjectSettingForNode(userInfo, parentOrNodeId,
				ProjectSettingsType.external_sync, ExternalSyncSetting.class);
		if (projectSettingForNode != null && BooleanUtils.isTrue(projectSettingForNode.getAutoSync())) {
			return AuthorizationManagerUtil.accessDenied("This is an autosync folder. No content can be placed in this container.");
		}

		// if there are any unmet access requirements return false
		List<String> nodeAncestorIds = AccessRequirementUtil.getNodeAncestorIds(nodeDao, parentOrNodeId, true);

		List<Long> accessRequirementIds = AccessRequirementUtil.unmetUploadAccessRequirementIdsForEntity(
				userInfo, nodeAncestorIds, nodeDao, accessRequirementDAO);
		if (accessRequirementIds.isEmpty()) {
			return AuthorizationManagerUtil.AUTHORIZED;
		} else {
			return AuthorizationManagerUtil
					.accessDenied("There are unmet access requirements that must be met to place content in the requested container.");
		}
	}

	private boolean agreesToTermsOfUse(UserInfo userInfo) throws NotFoundException {
		return authenticationManager.hasUserAcceptedTermsOfUse(userInfo.getId(), DomainType.SYNAPSE);
	}
	
	@Override
	public AuthorizationStatus canCreateWiki(String entityId, UserInfo userInfo) throws DatastoreException, NotFoundException {
		if (!userInfo.isAdmin() && 
			!isCertifiedUserOrFeatureDisabled(userInfo) && 
				!nodeDao.getNodeTypeById(entityId).equals(EntityType.project))
			return AuthorizationManagerUtil.accessDenied("Only certified users may create non-project wikis in Synapse.");
		
		return certifiedUserHasAccess(entityId, ACCESS_TYPE.CREATE, userInfo);
	}
}
