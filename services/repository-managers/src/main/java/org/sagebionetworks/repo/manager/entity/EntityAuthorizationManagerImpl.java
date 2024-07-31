package org.sagebionetworks.repo.manager.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.AccessRestrictionStatusDao;
import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.repo.model.dbo.entity.UsersEntityPermissionsDao;
import org.sagebionetworks.repo.model.dbo.file.download.v2.FileActionRequired;
import org.sagebionetworks.repo.model.download.EnableTwoFa;
import org.sagebionetworks.repo.model.download.MeetAccessRequirement;
import org.sagebionetworks.repo.model.download.RequestDownload;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntityAuthorizationManagerImpl implements EntityAuthorizationManager {

	private final AccessRestrictionStatusDao accessRestrictionStatusDao;
	private final UsersEntityPermissionsDao usersEntityPermissionsDao;

	@Autowired
	public EntityAuthorizationManagerImpl(AccessRestrictionStatusDao accessRestrictionStatusDao, UsersEntityPermissionsDao usersEntityPermissionsDao) {
		super();
		this.accessRestrictionStatusDao = accessRestrictionStatusDao;
		this.usersEntityPermissionsDao = usersEntityPermissionsDao;
	}

	@Override
	public AuthorizationStatus hasAccess(UserInfo userInfo, String entityId, ACCESS_TYPE... accessTypes)
			throws NotFoundException, DatastoreException {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(accessTypes, "accessTypes");
		if (accessTypes.length < 1) {
			throw new IllegalArgumentException("At least one ACCESS_TYPE must be provided");
		}
		EntityStateProvider stateProvider = new LazyEntityStateProvider(accessRestrictionStatusDao,
				usersEntityPermissionsDao, userInfo, KeyFactory.stringToKeySingletonList(entityId));
		AuthorizationStatus lastResult = null;
		for (ACCESS_TYPE accessType : accessTypes) {
			lastResult = EntityAuthorizationUtils.determineAccess(userInfo, KeyFactory.stringToKey(entityId), stateProvider, accessType)
					.getAuthorizationStatus();
			if (!lastResult.isAuthorized()) {
				return lastResult;
			}
		}
		return lastResult;
	}

	@Override
	public AuthorizationStatus canCreate(String parentId, EntityType entityCreateType, UserInfo userInfo)
			throws DatastoreException, NotFoundException {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(parentId, "parentId");
		ValidateArgument.required(entityCreateType, "entityCreateType");
		UserEntityPermissionsState state = usersEntityPermissionsDao
				.getEntityPermissions(userInfo.getGroups(), KeyFactory.stringToKeySingletonList(parentId)).stream()
				.findFirst().get();
		return EntityAuthorizationUtils.determineCreateAccess(userInfo, state, entityCreateType).getAuthorizationStatus();
	}

	@Override
	public AuthorizationStatus canCreateWiki(String entityId, UserInfo userInfo) {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(entityId, "entityId");
		UserEntityPermissionsState state = usersEntityPermissionsDao
				.getEntityPermissions(userInfo.getGroups(), KeyFactory.stringToKeySingletonList(entityId)).stream()
				.findFirst().get();
		return EntityAuthorizationUtils.determineCreateAccess(userInfo, state, state.getEntityType()).getAuthorizationStatus();
	}

	@Override
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException {
		EntityStateProvider stateProvider = new LazyEntityStateProvider(accessRestrictionStatusDao,
				usersEntityPermissionsDao, userInfo, KeyFactory.stringToKeySingletonList(entityId));

		return EntityAuthorizationUtils.getUserPermissionsForEntity(userInfo, entityId, stateProvider);
	}

	@Override
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo, String entityId, EntityStateProvider stateProvider)
			throws NotFoundException, DatastoreException {
		return EntityAuthorizationUtils.getUserPermissionsForEntity(userInfo, entityId, stateProvider);
	}

	@Override
	public List<UsersEntityAccessInfo> batchHasAccess(UserInfo userInfo, List<Long> entityIds, ACCESS_TYPE accessType) {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(entityIds, "entityId");
		ValidateArgument.required(accessType, "accessType");

		EntityStateProvider stateProvider = new LazyEntityStateProvider(accessRestrictionStatusDao,
				usersEntityPermissionsDao, userInfo, entityIds);
		return entityIds.stream().map(id -> EntityAuthorizationUtils.determineAccess(userInfo, id, stateProvider, accessType))
				.collect(Collectors.toList());
	}

	@Override
	public AuthorizationStatus canDeleteACL(UserInfo userInfo, String entityId) {
		EntityStateProvider stateProvider = new LazyEntityStateProvider(accessRestrictionStatusDao,
				usersEntityPermissionsDao, userInfo, KeyFactory.stringToKeySingletonList(entityId));
		return EntityAuthorizationUtils.determineCanDeleteACL(userInfo, stateProvider.getPermissionsState(KeyFactory.stringToKey(entityId)))
				.getAuthorizationStatus();
	}

	@Override
	public List<FileActionRequired> getActionsRequiredForDownload(UserInfo userInfo, List<Long> entityIds) {
		ValidateArgument.required(userInfo, "The userInfo");
		ValidateArgument.required(entityIds, "The entityIds");

		List<UsersEntityAccessInfo> batchInfo = batchHasAccess(userInfo, entityIds, ACCESS_TYPE.DOWNLOAD);

		List<FileActionRequired> actions = new ArrayList<>(batchInfo.size());

		boolean hasTwoFactorAuthEnabled = userInfo.hasTwoFactorAuthEnabled();

		for (UsersEntityAccessInfo info : batchInfo) {
			ValidateArgument.required(info.getAuthorizationStatus(), "info.authorizationStatus");
			ValidateArgument.required(info.getAccessRestrictions(), "info.accessRestrictions()");
			if (!info.getAuthorizationStatus().isAuthorized() && info.doesEntityExist()) {
				// First check if the user has any unapproved AR
				if (!info.getUnmetAccessRequirements().isEmpty()) {
					for (long arId : info.getUnmetAccessRequirements()) {
							actions.add(new FileActionRequired().withFileId(info.getEntityId()).withAction(
									new MeetAccessRequirement().setAccessRequirementId(arId)
								)
							);
					}
				}

				// The user might need to enable 2FA in order to download data
				Optional<UsersRequirementStatus> twoFaRequirement = info.getAccessRestrictions().getAccessRestrictions().stream().filter(UsersRequirementStatus::isTwoFaRequired).findFirst();
				if (!hasTwoFactorAuthEnabled && twoFaRequirement.isPresent()) {
					actions.add(new FileActionRequired().withFileId(info.getEntityId()).withAction(
							new EnableTwoFa().setAccessRequirementId(twoFaRequirement.get().getRequirementId())
						)
					);
				} else if (info.getUnmetAccessRequirements().isEmpty()) {
					// The last check is on the ACL
					actions.add(new FileActionRequired().withFileId(info.getEntityId())
						.withAction(new RequestDownload().setBenefactorId(info.getBenefactorId())));
				}
			}
		}

		return actions;
	}

}
