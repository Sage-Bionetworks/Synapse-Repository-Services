package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_SETTINGS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CREATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DOWNLOAD;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.MODERATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPLOAD;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.collections.Transform;
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
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.project.ExternalSyncSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class EntityPermissionsManagerImpl implements EntityPermissionsManager {

	private static final Long TRASH_FOLDER_ID = Long.parseLong(
			StackConfigurationSingleton.singleton().getTrashFolderEntityId());

	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	private AccessRequirementDAO  accessRequirementDAO;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private ProjectSettingsManager projectSettingsManager;
	@Autowired
	private StackConfiguration configuration;

	@Autowired
	private ProjectStatsManager projectStatsManager;
	@Autowired
	private TransactionalMessenger transactionalMessenger;


	@Override
	public AccessControlList getACL(String nodeId, UserInfo userInfo) throws NotFoundException, DatastoreException, ACLInheritanceException {
		// Get the id that this node inherits its permissions from
		String benefactor = nodeDao.getBenefactor(nodeId);		
		// This is a fix for PLFM-398
		if (!benefactor.equals(nodeId)) {
			throw new ACLInheritanceException("Cannot access the ACL of a node that inherits it permissions. This node inherits its permissions from: "+benefactor, benefactor);
		}
		AccessControlList acl = aclDAO.get(nodeId, ObjectType.ENTITY);
		return acl;
	}
	
	private static final Function<ResourceAccess, Long> RESOURCE_ACCESS_TO_PRINCIPAL_TRANSFORMER = new Function<ResourceAccess, Long>() {
		@Override
		public Long apply(ResourceAccess input) {
			return input.getPrincipalId();
		}
	};
		
	@WriteTransaction
	@Override
	public AccessControlList updateACL(AccessControlList acl, UserInfo userInfo) throws NotFoundException, DatastoreException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		String rId = acl.getId();
		String benefactor = nodeDao.getBenefactor(rId);
		if (!benefactor.equals(rId)) throw new UnauthorizedException("Cannot update ACL for a resource which inherits its permissions.");
		// check permissions of user to change permissions for the resource
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				hasAccess(rId, CHANGE_PERMISSIONS, userInfo));
		// validate content
		Long ownerId = nodeDao.getCreatedBy(acl.getId());
		PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
		
		AccessControlList oldAcl = aclDAO.get(acl.getId(), ObjectType.ENTITY);
		
		aclDAO.update(acl, ObjectType.ENTITY);
		
		// Now we compare the old and the new acl to see what might have
		// changed, so we can send notifications out.
		// We only care about principals being added or removed, not what
		// exactly has happened.
		Set<Long> oldPrincipals = Transform.toSet(
				oldAcl.getResourceAccess(),
				RESOURCE_ACCESS_TO_PRINCIPAL_TRANSFORMER);
		Set<Long> newPrincipals = Transform.toSet(acl.getResourceAccess(),
				RESOURCE_ACCESS_TO_PRINCIPAL_TRANSFORMER);

		SetView<Long> addedPrincipals = Sets.difference(newPrincipals,
				oldPrincipals);
		
		Date now = new Date();
		for (Long principal : addedPrincipals) {
			// update the stats for each new principal
			projectStatsManager.updateProjectStats(principal, rId, ObjectType.ENTITY, now);
		}
		
		acl = aclDAO.get(acl.getId(), ObjectType.ENTITY);
		return acl;
	}


	@WriteTransaction
	@Override
	public AccessControlList overrideInheritance(AccessControlList acl, UserInfo userInfo) throws NotFoundException, DatastoreException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		String entityId = acl.getId();
		Node node = nodeDao.getNode(entityId);
		String benefactorId = nodeDao.getBenefactor(entityId);
		if(KeyFactory.equals(benefactorId, entityId)){
			throw new UnauthorizedException("Resource already has an ACL.");
		}
		// check permissions of user to change permissions for the resource
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				hasAccess(benefactorId, CHANGE_PERMISSIONS, userInfo));
		
		// validate the Entity owners will still have access.
		PermissionsManagerUtils.validateACLContent(acl, userInfo, node.getCreatedByPrincipalId());
		// Before we can update the ACL we must grab the lock on the node.
		String newEtag = nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());
		// persist acl and return
		aclDAO.create(acl, ObjectType.ENTITY);
		acl = aclDAO.get(acl.getId(), ObjectType.ENTITY);
		// Send a container message for projects or folders.
		if(NodeUtils.isProjectOrFolder(node.getNodeType())){
			// Notify listeners of the hierarchy change to this container.
			transactionalMessenger.sendMessageAfterCommit(entityId, ObjectType.ENTITY_CONTAINER, newEtag, ChangeType.UPDATE);
		}
		return acl;
	}

	@WriteTransaction
	@Override
	public AccessControlList restoreInheritance(String entityId, UserInfo userInfo) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// Before we can update the ACL we must grab the lock on the node.
		Node node = nodeDao.getNode(entityId);
		String benefactorId = nodeDao.getBenefactor(entityId);
		// check permissions of user to change permissions for the resource
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				hasAccess(entityId, CHANGE_PERMISSIONS, userInfo));
		if(!KeyFactory.equals(entityId, benefactorId)){
			throw new UnauthorizedException("Resource already inherits its permissions.");	
		}

		// if parent is root, than can't inherit, must have own ACL
		if (nodeDao.isNodesParentRoot(entityId)) throw new UnauthorizedException("Cannot restore inheritance for resource which has no parent.");
		
		String newEtag = nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());
		
		// delete access control list
		aclDAO.delete(entityId, ObjectType.ENTITY);
		
		// now find the newly governing ACL
		String benefactor = nodeDao.getBenefactor(entityId);
		
		// Send a container message for projects or folders.
		if(NodeUtils.isProjectOrFolder(node.getNodeType())){
			/*
			 *  Notify listeners of the hierarchy change to this container.
			 *  See PLFM-4410.
			 */
			transactionalMessenger.sendMessageAfterCommit(entityId, ObjectType.ENTITY_CONTAINER, newEtag, ChangeType.UPDATE);
		}
		
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

		String benefactorId = nodeDao.getBenefactor(parentId);
		applyInheritanceToChildrenHelper(parentId, benefactorId, userInfo);

		// return governing parent ACL
		return aclDAO.get(nodeDao.getBenefactor(parentId), ObjectType.ENTITY);
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
	public AuthorizationStatus canCreate(String parentId, EntityType nodeType, UserInfo userInfo) 
			throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) {
			return AuthorizationManagerUtil.AUTHORIZED;
		}
		if (parentId == null) {
			return AuthorizationManagerUtil.accessDenied("Cannot create a entity having no parent.");
		}

		if (!isCertifiedUserOrFeatureDisabled(userInfo) && !EntityType.project.equals(nodeType)) 
			return AuthorizationManagerUtil.accessDenied("Only certified users may create content in Synapse.");
		
		return certifiedUserHasAccess(parentId, null, CREATE, userInfo);
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

		return certifiedUserHasAccess(node.getId(), node.getNodeType(), ACCESS_TYPE.CHANGE_SETTINGS, userInfo);
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
		
		EntityType entityType = nodeDao.getNodeTypeById(entityId);
		
		if (!userInfo.isAdmin() && 
			!isCertifiedUserOrFeatureDisabled(userInfo) && 
				(accessType==CREATE ||
				(accessType==UPDATE && entityType!=EntityType.project)))
			return AuthorizationManagerUtil.accessDenied("Only certified users may create or update content in Synapse.");
		
		return certifiedUserHasAccess(entityId, entityType, accessType, userInfo);
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
	public AuthorizationStatus certifiedUserHasAccess(String entityId, EntityType entityType, ACCESS_TYPE accessType, UserInfo userInfo)
				throws NotFoundException, DatastoreException  {
		// In the case of the trash can, throw the EntityInTrashCanException
		// The only operations allowed over the trash can is CREATE (i.e. moving
		// items into the trash can) and DELETE (i.e. purging the trash).
		final String benefactor = nodeDao.getBenefactor(entityId);
		if (TRASH_FOLDER_ID.equals(KeyFactory.stringToKey(benefactor))
				&& !CREATE.equals(accessType)
				&& !DELETE.equals(accessType)) {
			throw new EntityInTrashCanException("Entity " + entityId + " is in trash can.");
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
		// Can download
		if (accessType == DOWNLOAD) {
			return canDownload(userInfo, entityId, benefactor, entityType);
		}
		// Can upload
		if (accessType == UPLOAD) {
			return canUpload(userInfo, entityId);
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
		return nodeDao.getBenefactor(nodeId);
	}

	@Override
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo,	String entityId)
			throws NotFoundException, DatastoreException {

		Node node = nodeDao.getNode(entityId);
		
		String benefactor = nodeDao.getBenefactor(entityId);

		UserEntityPermissions permissions = new UserEntityPermissions();
		permissions.setCanAddChild(hasAccess(entityId, CREATE, userInfo).getAuthorized());
		permissions.setCanCertifiedUserAddChild(certifiedUserHasAccess(entityId, node.getNodeType(), CREATE, userInfo).getAuthorized());
		permissions.setCanChangePermissions(hasAccess(entityId, CHANGE_PERMISSIONS, userInfo).getAuthorized());
		permissions.setCanChangeSettings(hasAccess(entityId, CHANGE_SETTINGS, userInfo).getAuthorized());
		permissions.setCanDelete(hasAccess(entityId, DELETE, userInfo).getAuthorized());
		permissions.setCanEdit(hasAccess(entityId, UPDATE, userInfo).getAuthorized());
		permissions.setCanCertifiedUserEdit(certifiedUserHasAccess(entityId, node.getNodeType(), UPDATE, userInfo).getAuthorized());
		permissions.setCanView(hasAccess(entityId, READ, userInfo).getAuthorized());
		permissions.setCanDownload(canDownload(userInfo, entityId, benefactor, node.getNodeType()).getAuthorized());
		permissions.setCanUpload(canUpload(userInfo, entityId).getAuthorized());
		permissions.setCanModerate(hasAccess(entityId, MODERATE, userInfo).getAuthorized());

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
			return nodeDao.getBenefactor(resourceId).equals(resourceId);
		} catch (Exception e) {
			return false;
		}
	}

	// entities have to meet access requirements (ARs)
	private AuthorizationStatus canDownload(UserInfo userInfo, String entityId, String benefactor, EntityType entityType)
			throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) return AuthorizationManagerUtil.AUTHORIZED;
		
		// if the ACL and access requirements permit DOWNLOAD, then its permitted,
		// and this applies to any type of entity
		boolean aclAllowsDownload = aclDAO.canAccess(userInfo.getGroups(), benefactor, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD);
		AuthorizationStatus meetsAccessRequirements = meetsAccessRequirements(userInfo, entityId);
		if (meetsAccessRequirements.getAuthorized() && aclAllowsDownload) {
			return AuthorizationManagerUtil.AUTHORIZED;
		}
		
		// at this point the entity is NOT authorized via ACL+access requirements
		if (!aclAllowsDownload) return new AuthorizationStatus(false, "You lack DOWNLOAD access to the requested entity.");
		return meetsAccessRequirements;
	}
	
	private AuthorizationStatus meetsAccessRequirements(UserInfo userInfo, final String nodeId)
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
		return AuthorizationManagerUtil.AUTHORIZED;
	}

	private boolean agreesToTermsOfUse(UserInfo userInfo) throws NotFoundException {
		return authenticationManager.hasUserAcceptedTermsOfUse(userInfo.getId());
	}
	
	@Override
	public AuthorizationStatus canCreateWiki(String entityId, UserInfo userInfo) throws DatastoreException, NotFoundException {
		EntityType entityType = nodeDao.getNodeTypeById(entityId);
		if (!userInfo.isAdmin() && 
			!isCertifiedUserOrFeatureDisabled(userInfo) && 
				entityType!=EntityType.project)
			return AuthorizationManagerUtil.accessDenied("Only certified users may create non-project wikis in Synapse.");
		
		return certifiedUserHasAccess(entityId, entityType, ACCESS_TYPE.CREATE, userInfo);
	}

	@Override
	public Set<Long> getNonvisibleChildren(UserInfo user, String parentId) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(parentId, "parentId");
		if(user.isAdmin()){
			return new HashSet<Long>(0);
		}
		return aclDAO.getNonVisibleChilrenOfEntity(user.getGroups(), parentId);
	}
}
