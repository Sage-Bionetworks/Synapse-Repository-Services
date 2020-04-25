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
import java.util.Optional;
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
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.project.ProjectCertificationSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class EntityPermissionsManagerImpl implements EntityPermissionsManager {

	private static final Long TRASH_FOLDER_ID = Long.parseLong(StackConfigurationSingleton.singleton().getTrashFolderEntityId());
	private static final String ERR_MESSAGE_CERTIFIED_USER_CONTENT = "Only certified users may create or update content in Synapse.";

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
	@Autowired
	private ObjectTypeManager objectTypeManager;

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

		// Can't override ACL inheritance if the entity lives inside an STS-enabled folder.
		// Note that even though the method  is called getProjectId(), it can actually refer to either a Project or a
		// Folder.
		Optional<UploadDestinationListSetting> projectSetting = projectSettingsManager.getProjectSettingForNode(userInfo,
				entityId, ProjectSettingsType.upload, UploadDestinationListSetting.class);
		if (projectSetting.isPresent() && !KeyFactory.equals(projectSetting.get().getProjectId(), entityId)) {
			// If the project setting is defined on the current entity, you can still override ACL inheritance.
			// Overriding ACL inheritance is only blocked for child entities.
			if (projectSettingsManager.isStsStorageLocationSetting(projectSetting.get())) {
				throw new IllegalArgumentException("Cannot override ACLs in a child of an STS-enabled folder");
			}
		}

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
	
	boolean isCertifiedUserOrFeatureDisabled(UserInfo userInfo, String entityId) {
		Boolean featureIsDisabled = configuration.getDisableCertifiedUser();
		
		if (featureIsDisabled == null || featureIsDisabled) {
			return true;
		}
		
		if (AuthorizationUtils.isCertifiedUser(userInfo)) {
			return true;
		}
		
		return !isCertificationRequired(userInfo, entityId);
	}
	
	boolean isCertificationRequired(UserInfo userInfo, String entityId) {
		// by default the certification is required, checks if the project is configured to disable the certification requirement
		
		Optional<ProjectCertificationSetting> certificationSetting = projectSettingsManager.getProjectSettingForNode(userInfo, entityId, ProjectSettingsType.certification, ProjectCertificationSetting.class);
		
		// By default the certification is required on any project
		boolean isCertificationRequired = true;
		
		return certificationSetting
				.map(ProjectCertificationSetting::getCertificationRequired)
				.orElse(isCertificationRequired);
		
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

		if (!EntityType.project.equals(nodeType) && !isCertifiedUserOrFeatureDisabled(userInfo, parentId)) {
			return AuthorizationStatus.accessDenied(new UserCertificationRequiredException(ERR_MESSAGE_CERTIFIED_USER_CONTENT));
		}
		
		return certifiedUserHasAccess(parentId, null, CREATE, userInfo);
	}

	@Override
	public AuthorizationStatus canChangeSettings(Node node, UserInfo userInfo) throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) {
			return AuthorizationStatus.authorized();
		}

		if (!isCertifiedUserOrFeatureDisabled(userInfo, node.getId())) {
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
			(accessType==CREATE || (accessType==UPDATE && entityType!=EntityType.project)) &&
			!isCertifiedUserOrFeatureDisabled(userInfo, entityId)) {
			return AuthorizationStatus.accessDenied(ERR_MESSAGE_CERTIFIED_USER_CONTENT);
		}
		
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
	private AuthorizationStatus certifiedUserHasAccess(String entityId, EntityType entityType, ACCESS_TYPE accessType, UserInfo userInfo)
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
		
		// Anonymous can at most READ (or DOWNLOAD when the entity is marked with OPEN_ACCESS)
		if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			if (accessType != ACCESS_TYPE.READ && accessType != ACCESS_TYPE.DOWNLOAD) {
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
		permissions.setIsCertificationRequired(isCertificationRequired(userInfo, entityId));

		permissions.setOwnerPrincipalId(node.getCreatedByPrincipalId());
		
		permissions.setIsCertifiedUser(AuthorizationUtils.isCertifiedUser(userInfo));

		UserInfo anonymousUser = UserInfoHelper.createAnonymousUserInfo();
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
		
		if (userInfo.isAdmin()) {
			return AuthorizationStatus.authorized();
		}
		
		// We check on the terms of use agreement if the user is not anonymous
		if (!AuthorizationUtils.isUserAnonymous(userInfo) && !agreesToTermsOfUse(userInfo)) {
			return AuthorizationStatus.accessDenied("You have not yet agreed to the Synapse Terms of Use.");
		}
		
		ACCESS_TYPE accessTypeCheck = DOWNLOAD;
		
		DataType entityDataType = objectTypeManager.getObjectsDataType(entityId, ObjectType.ENTITY);
		
		// If the access type is OPEN_DATA then the user can download as long as it has READ access (See PLFM-6059)
		// We do not check DOWNLOAD access since historically tables marked as OPEN_DATA were checked on READ access only
		if (DataType.OPEN_DATA == entityDataType) {
			accessTypeCheck = READ;
		}
		
		boolean aclAllowsDownload = aclDAO.canAccess(userInfo.getGroups(), benefactor, ObjectType.ENTITY, accessTypeCheck);
		
		if (!aclAllowsDownload) {
			return AuthorizationStatus.accessDenied("You lack " + accessTypeCheck.name() + " access to the requested entity.");	
		}
		
		// if the ACL and access requirements permit DOWNLOAD (or READ for OPEN_DATA), then its permitted,
		// and this applies to any type of entity
		return meetsAccessRequirements(userInfo, entityId);
		
	}
	
	private AuthorizationStatus meetsAccessRequirements(final UserInfo userInfo, final String nodeId)
			throws DatastoreException, NotFoundException {
		// if there are any unmet access requirements return false
		List<Long> nodeAncestorIds = nodeDao.getEntityPathIds(nodeId, false);

		List<Long> accessRequirementIds = AccessRequirementUtil.unmetDownloadAccessRequirementIdsForEntity(
				userInfo, nodeId, nodeAncestorIds, nodeDao, accessRequirementDAO);
		
		if (!accessRequirementIds.isEmpty()) {		
			return AuthorizationStatus
					.accessDenied("There are unmet access requirements that must be met to read content in the requested container.");
		}
		
		return AuthorizationStatus.authorized();
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
			EntityType.project != entityType &&
			!isCertifiedUserOrFeatureDisabled(userInfo, entityId)) {
			return AuthorizationStatus.accessDenied("Only certified users may create non-project wikis in Synapse.");
		}
		
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
