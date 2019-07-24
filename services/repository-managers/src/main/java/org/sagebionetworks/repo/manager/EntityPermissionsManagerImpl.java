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
		//
		// PLFM-2399:  There is a case in which a node ID is passed in without the 'syn' prefix.  
		// In this case 'nodeId' might be '12345' while benefactor might be 'syn12345'.
		// The change below normalizes the format.
		//
		// This is a fix for PLFM-398
		if (!benefactor.equals(KeyFactory.keyToString(KeyFactory.stringToKey(nodeId)))) {
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
		hasAccess(rId, CHANGE_PERMISSIONS, userInfo).checkAuthorizationOrElseThrow();
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
		hasAccess(benefactorId, CHANGE_PERMISSIONS, userInfo).checkAuthorizationOrElseThrow();
		
		// validate the Entity owners will still have access.
		PermissionsManagerUtils.validateACLContent(acl, userInfo, node.getCreatedByPrincipalId());
		// Before we can update the ACL we must grab the lock on the node.
		String newEtag = nodeDao.touch(userInfo.getId(), entityId);
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
		String benefactorId = nodeDao.getBenefactor(entityId);
		// check permissions of user to change permissions for the resource
		hasAccess(entityId, CHANGE_PERMISSIONS, userInfo).checkAuthorizationOrElseThrow();
		if(!KeyFactory.equals(entityId, benefactorId)){
			throw new UnauthorizedException("Resource already inherits its permissions.");	
		}

		// if parent is root, than can't inherit, must have own ACL
		if (nodeDao.isNodesParentRoot(entityId)) throw new UnauthorizedException("Cannot restore inheritance for resource which has no parent.");
		// lock and update the entity owner of this acl.
		String newEtag = nodeDao.touch(userInfo.getId(), entityId);
		
		// delete access control list
		aclDAO.delete(entityId, ObjectType.ENTITY);
		
		// now find the newly governing ACL
		String benefactor = nodeDao.getBenefactor(entityId);
		
		EntityType entityType = nodeDao.getNodeTypeById(entityId);
		
		// Send a container message for projects or folders.
		if(NodeUtils.isProjectOrFolder(entityType)){
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
		hasAccess(parentId,CHANGE_PERMISSIONS, userInfo).checkAuthorizationOrElseThrow();
		
		// Before we can update the ACL we must grab the lock on the node.
		nodeDao.touch(userInfo.getId(), parentId);

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
			if (hasAccess(idToChange, CHANGE_PERMISSIONS, userInfo).isAuthorized()) {
				// delete child ACL, if present
				if (hasLocalACL(idToChange)) {
					// Touch and lock the owner node before updating the ACL.
					nodeDao.touch(userInfo.getId(), idToChange);
					
					// delete ACL
					aclDAO.delete(idToChange, ObjectType.ENTITY);
				}								
			}
		}
	}
	
	private boolean isCertifiedUserOrFeatureDisabled(UserInfo userInfo) {
		Boolean featureIsDisabled = configuration.getDisableCertifiedUser();
		return featureIsDisabled == null || featureIsDisabled || AuthorizationUtils.isCertifiedUser(userInfo);
	}
	
	@Override
	public AuthorizationStatus canCreate(String parentId, EntityType nodeType, UserInfo userInfo) 
			throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) {
			return AuthorizationStatus.authorized();
		}
		if (parentId == null) {
			return AuthorizationStatus.accessDenied("Cannot create a entity having no parent.");
		}

		if (!isCertifiedUserOrFeatureDisabled(userInfo) && !EntityType.project.equals(nodeType)) 
			return AuthorizationStatus.accessDenied(new UserCertificationRequiredException("Only certified users may create content in Synapse."));
		
		return certifiedUserHasAccess(parentId, null, CREATE, userInfo);
	}

	@Override
	public AuthorizationStatus canChangeSettings(Node node, UserInfo userInfo) throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) {
			return AuthorizationStatus.authorized();
		}

		if (!isCertifiedUserOrFeatureDisabled(userInfo)) {
			return AuthorizationStatus.accessDenied("Only certified users may change node settings.");
		}

		// the creator always has change settings permissions
		if (node.getCreatedByPrincipalId().equals(userInfo.getId())) {
			return AuthorizationStatus.authorized();
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
			return AuthorizationStatus.accessDenied("Only certified users may create or update content in Synapse.");
		
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
				return AuthorizationStatus.accessDenied("Anonymous users have only READ access permission.");
			}
		}
		// Admin
		if (userInfo.isAdmin()) {
			return AuthorizationStatus.authorized();
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
			return AuthorizationStatus.authorized();
		} else {
			return AuthorizationStatus.accessDenied("You do not have "+accessType+" permission for the requested entity.");
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
		permissions.setCanAddChild(hasAccess(entityId, CREATE, userInfo).isAuthorized());
		permissions.setCanCertifiedUserAddChild(certifiedUserHasAccess(entityId, node.getNodeType(), CREATE, userInfo).isAuthorized());
		permissions.setCanChangePermissions(hasAccess(entityId, CHANGE_PERMISSIONS, userInfo).isAuthorized());
		permissions.setCanChangeSettings(hasAccess(entityId, CHANGE_SETTINGS, userInfo).isAuthorized());
		permissions.setCanDelete(hasAccess(entityId, DELETE, userInfo).isAuthorized());
		permissions.setCanEdit(hasAccess(entityId, UPDATE, userInfo).isAuthorized());
		permissions.setCanCertifiedUserEdit(certifiedUserHasAccess(entityId, node.getNodeType(), UPDATE, userInfo).isAuthorized());
		permissions.setCanView(hasAccess(entityId, READ, userInfo).isAuthorized());
		permissions.setCanDownload(canDownload(userInfo, entityId, benefactor, node.getNodeType()).isAuthorized());
		permissions.setCanUpload(canUpload(userInfo, entityId).isAuthorized());
		permissions.setCanModerate(hasAccess(entityId, MODERATE, userInfo).isAuthorized());

		permissions.setOwnerPrincipalId(node.getCreatedByPrincipalId());
		
		permissions.setIsCertifiedUser(AuthorizationUtils.isCertifiedUser(userInfo));

		UserInfo anonymousUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		permissions.setCanPublicRead(hasAccess(entityId, READ, anonymousUser).isAuthorized());

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
		if (userInfo.isAdmin()) return AuthorizationStatus.authorized();
		
		// if the ACL and access requirements permit DOWNLOAD, then its permitted,
		// and this applies to any type of entity
		boolean aclAllowsDownload = aclDAO.canAccess(userInfo.getGroups(), benefactor, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD);
		AuthorizationStatus meetsAccessRequirements = meetsAccessRequirements(userInfo, entityId);
		if (meetsAccessRequirements.isAuthorized() && aclAllowsDownload) {
			return AuthorizationStatus.authorized();
		}
		
		// at this point the entity is NOT authorized via ACL+access requirements
		if (!aclAllowsDownload) return AuthorizationStatus.accessDenied("You lack DOWNLOAD access to the requested entity.");
		return meetsAccessRequirements;
	}
	
	private AuthorizationStatus meetsAccessRequirements(UserInfo userInfo, final String nodeId)
			throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) return AuthorizationStatus.authorized();
		if (!agreesToTermsOfUse(userInfo)) return AuthorizationStatus.accessDenied("You have not yet agreed to the Synapse Terms of Use.");
		
		// if there are any unmet access requirements return false
		List<String> nodeAncestorIds = AccessRequirementUtil.getNodeAncestorIds(nodeDao, nodeId, false);

		List<Long> accessRequirementIds = AccessRequirementUtil.unmetDownloadAccessRequirementIdsForEntity(
				userInfo, nodeId, nodeAncestorIds, nodeDao, accessRequirementDAO);
		if (accessRequirementIds.isEmpty()) {
			return AuthorizationStatus.authorized();
		} else {
			return AuthorizationStatus
					.accessDenied("There are unmet access requirements that must be met to read content in the requested container.");
		}
		
	}
	
	private AuthorizationStatus canUpload(UserInfo userInfo, final String parentOrNodeId)
			throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) {
			return AuthorizationStatus.authorized();
		}
		if (!agreesToTermsOfUse(userInfo)) {
			return AuthorizationStatus.accessDenied("You have not yet agreed to the Synapse Terms of Use.");
		}
		return AuthorizationStatus.authorized();
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
			return AuthorizationStatus.accessDenied("Only certified users may create non-project wikis in Synapse.");
		
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
