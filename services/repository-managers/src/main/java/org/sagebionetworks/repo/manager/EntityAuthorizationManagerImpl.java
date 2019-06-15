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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.project.ExternalSyncSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityAuthorizationManagerImpl implements EntityAuthorizationManager {

	private static final Long TRASH_FOLDER_ID = Long.parseLong(
			StackConfigurationSingleton.singleton().getTrashFolderEntityId());

	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private AuthorizationDAO authorizationDAO;
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
			return AuthorizationStatus.authorized();
		}
		if (parentId == null) {
			return AuthorizationStatus.accessDenied("Cannot create a entity having no parent.");
		}

		if (!isCertifiedUserOrFeatureDisabled(userInfo) && !EntityType.project.equals(nodeType)) 
			return AuthorizationStatus.accessDenied("Only certified users may create content in Synapse.");
		
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
				return AuthorizationStatus.
						accessDenied("Anonymous users have only READ access permission.");
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
		if (authorizationDAO.canAccess(userInfo.getGroups(), benefactor, ObjectType.ENTITY, accessType)) {
			return AuthorizationStatus.authorized();
		} else {
			return AuthorizationStatus.
					accessDenied("You do not have "+accessType+" permission for the requested entity.");
		}
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

	// entities have to meet access requirements (ARs)
	private AuthorizationStatus canDownload(UserInfo userInfo, String entityId, String benefactor, EntityType entityType)
			throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) return AuthorizationStatus.authorized();
		
		// if the ACL and access requirements permit DOWNLOAD, then its permitted,
		// and this applies to any type of entity
		boolean aclAllowsDownload = authorizationDAO.canAccess(userInfo.getGroups(), benefactor, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD);
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
		if (!agreesToTermsOfUse(userInfo)) return AuthorizationStatus.
					accessDenied("You have not yet agreed to the Synapse Terms of Use.");
		
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
		if (userInfo.isAdmin()) return AuthorizationStatus.authorized();
		if (!agreesToTermsOfUse(userInfo)) return AuthorizationStatus.
				accessDenied("You have not yet agreed to the Synapse Terms of Use.");
		
		ExternalSyncSetting projectSettingForNode = projectSettingsManager.getProjectSettingForNode(userInfo, parentOrNodeId,
				ProjectSettingsType.external_sync, ExternalSyncSetting.class);
		if (projectSettingForNode != null && projectSettingForNode.getAutoSync()!=null && projectSettingForNode.getAutoSync()) {
			return AuthorizationStatus.accessDenied("This is an autosync folder. No content can be placed in this container.");
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
		return authorizationDAO.getNonVisibleChildrenOfEntity(user.getGroups(), parentId);
	}
}
