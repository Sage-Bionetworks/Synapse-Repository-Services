package org.sagebionetworks.repo.manager.entity;

import static org.sagebionetworks.repo.model.AuthorizationConstants.ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MESSAGE_CERTIFIED_USER_CONTENT;
import static org.sagebionetworks.repo.model.AuthorizationConstants.THERE_ARE_UNMET_ACCESS_REQUIREMENTS;
import static org.sagebionetworks.repo.model.AuthorizationConstants.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.manager.dataaccess.RestrictionInformationManager;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.SubjectStatus;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.repo.model.dbo.entity.UsersEntityPermissionsDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntityAuthorizationManagerImpl implements EntityAuthorizationManager {

	public static final Long TRASH_FOLDER_ID = Long
			.parseLong(StackConfigurationSingleton.singleton().getTrashFolderEntityId());

	@Autowired
	private RestrictionInformationManager restrictionInformationManager;
	@Autowired
	private UsersEntityPermissionsDao usersEntityPermissionsDao;

	public static final Decider IS_ADMIN = (c) -> {
		if (c.getUser().isAdmin()) {
			return Optional.of(
					new UsersEntityAccessInfo(c.getPermissionState().getEntityId(), AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	};

	public static final Decider IS_IN_TRASH = (c) -> {
		if (TRASH_FOLDER_ID.equals(c.getPermissionState().getBenefactorId())) {
			return Optional.of(new UsersEntityAccessInfo(c.getPermissionState().getEntityId(),
					AuthorizationStatus
							.accessDenied(new EntityInTrashCanException(String.format(ENTITY_S_IS_IN_TRASH_CAN_TEMPLATE,
									c.getPermissionState().getEntityIdAsString())))));
		} else {
			return Optional.empty();
		}
	};

	public static final Decider DOES_ENTITY_EXIST = (c) -> {
		if (c.getPermissionState().doesEntityExist()) {
			return Optional.of(new UsersEntityAccessInfo(c.getPermissionState().getEntityId(), AuthorizationStatus
					.accessDenied(new NotFoundException(THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND))));
		} else {
			return Optional.empty();
		}
	};

	public static final Decider HAS_UNMET_ACCESS_RESTRICTIONS = (c) -> {
		if (c.getRestrictionStatus().hasUnmet()) {
			return Optional.of(new UsersEntityAccessInfo(c.getPermissionState().getEntityId(),
					AuthorizationStatus.accessDenied(THERE_ARE_UNMET_ACCESS_REQUIREMENTS)));
		} else {
			return Optional.empty();
		}
	};

	public static final Decider IS_OPEN_DATE_WITH_READ = (c) -> {
		if (DataType.OPEN_DATA.equals(c.getPermissionState().getDataType()) && c.getPermissionState().hasRead()) {
			return Optional.of(
					new UsersEntityAccessInfo(c.getPermissionState().getEntityId(), AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	};

	public static final Decider HAS_DOWNLOAD = (c) -> {
		if (c.getPermissionState().hasDownload()) {
			return Optional.of(
					new UsersEntityAccessInfo(c.getPermissionState().getEntityId(), AuthorizationStatus.authorized()));
		} else {
			return Optional.of(new UsersEntityAccessInfo(c.getPermissionState().getEntityId(),
					AuthorizationStatus.accessDenied(String.format(YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE,
							ACCESS_TYPE.DOWNLOAD.name(), c.getPermissionState().getEntityIdAsString()))));
		}
	};

	public static List<Decider> DOWNLOAD_DECIDERS = Arrays.asList(
			IS_ADMIN,
			DOES_ENTITY_EXIST,
			IS_IN_TRASH,
			HAS_UNMET_ACCESS_RESTRICTIONS,
			IS_OPEN_DATE_WITH_READ,
			HAS_DOWNLOAD);
	
	static UsersEntityAccessInfo decide(Context c, List<Decider> deciders) {
		return deciders.stream().map(d->d.apply(c)).filter(r->r.isPresent()).findFirst().get().orElseThrow(()->new IllegalStateException());
	}

	@Override
	public AuthorizationStatus hasAccess(UserInfo userInfo, String entityId, ACCESS_TYPE accessType)
			throws NotFoundException, DatastoreException {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(accessType, "accessType");
		long enityIdLong = KeyFactory.stringToKey(entityId);
		return batchHasAccess(userInfo, Arrays.asList(enityIdLong), accessType).stream().findFirst()
				.orElseThrow(() -> new IllegalStateException()).getAuthroizationStatus();
	}

	@Override
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UsersEntityAccessInfo> batchHasAccess(UserInfo userInfo, List<Long> entityIds, ACCESS_TYPE accessType) {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(entityIds, "entityId");
		ValidateArgument.required(accessType, "accessType");
		List<UserEntityPermissionsState> permissionState = usersEntityPermissionsDao
				.getEntityPermissions(userInfo.getGroups(), entityIds);
		return determineAccess(userInfo, permissionState, accessType);
	}

	/**
	 * Determine the access information for a batch of entities for a given user.
	 * 
	 * @param userInfo
	 * @param permissionState
	 * @param accessType
	 * @return
	 */
	public List<UsersEntityAccessInfo> determineAccess(UserInfo userInfo,
			List<UserEntityPermissionsState> permissionState, ACCESS_TYPE accessType) {
		switch (accessType) {
		case CREATE:
			EntityType newEntityType = null;
			return permissionState.stream().map(t -> determineCreateAccess(userInfo, t, newEntityType))
					.collect(Collectors.toList());
		case READ:
			return permissionState.stream().map(t -> determineReadAccess(userInfo, t)).collect(Collectors.toList());
		case UPDATE:
			return permissionState.stream().map(t -> determineUpdateAccess(userInfo, t)).collect(Collectors.toList());
		case DELETE:
			return permissionState.stream().map(t -> determineDeleteAccess(userInfo, t)).collect(Collectors.toList());
		case CHANGE_PERMISSIONS:
			return permissionState.stream().map(t -> determineChangePermissionAccess(userInfo, t))
					.collect(Collectors.toList());
		case DOWNLOAD:
			return determineDownloadAccess(userInfo, permissionState);
		case UPLOAD:
			return permissionState.stream().map(t -> determineUpdateAccess(userInfo, t)).collect(Collectors.toList());
		case CHANGE_SETTINGS:
			return permissionState.stream().map(t -> determineChangeSettingsAccess(userInfo, t))
					.collect(Collectors.toList());
		case MODERATE:
			return permissionState.stream().map(t -> determineModerateAccess(userInfo, t)).collect(Collectors.toList());
		default:
			throw new IllegalArgumentException("Unknown access type: " + accessType);

		}
	}

	UsersEntityAccessInfo determineModerateAccess(UserInfo userInfo, UserEntityPermissionsState permissionState) {
		validateNotInTrash(permissionState);
		AuthorizationStatus authroizationStatus = null;
		if (userInfo.isAdmin()) {
			authroizationStatus = AuthorizationStatus.authorized();
		} else if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			authroizationStatus = AuthorizationStatus.accessDenied(ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION);
		}
		return null;
	}

	UsersEntityAccessInfo determineChangeSettingsAccess(UserInfo userInfo, UserEntityPermissionsState permissionState) {
		validateNotInTrash(permissionState);
		AuthorizationStatus authroizationStatus = null;
		if (userInfo.isAdmin()) {
			authroizationStatus = AuthorizationStatus.authorized();
		} else if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			authroizationStatus = AuthorizationStatus.accessDenied(ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION);
		}
		return null;
	}

	UsersEntityAccessInfo determineChangePermissionAccess(UserInfo userInfo,
			UserEntityPermissionsState permissionState) {
		validateNotInTrash(permissionState);
		AuthorizationStatus authroizationStatus = null;
		if (userInfo.isAdmin()) {
			authroizationStatus = AuthorizationStatus.authorized();
		} else if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			authroizationStatus = AuthorizationStatus.accessDenied(ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION);
		}
		return null;
	}

	UsersEntityAccessInfo determineDeleteAccess(UserInfo userInfo, UserEntityPermissionsState permissionState) {
		AuthorizationStatus authroizationStatus = null;
		if (userInfo.isAdmin()) {
			authroizationStatus = AuthorizationStatus.authorized();
		} else if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			authroizationStatus = AuthorizationStatus.accessDenied(ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION);
		}
		return null;
	}

	List<UsersEntityAccessInfo> determineDownloadAccess(UserInfo userInfo,
			List<UserEntityPermissionsState> permissionState) {
		List<Long> entityIds = permissionState.stream().map(t -> t.getEntityId()).collect(Collectors.toList());
		List<SubjectStatus> restrictionStatus = restrictionInformationManager.getEntityRestrictionInformation(userInfo,
				entityIds);
		LinkedHashMap<Long, SubjectStatus> statusMap = new LinkedHashMap<Long, SubjectStatus>(restrictionStatus.size());
		for (SubjectStatus status : restrictionStatus) {
			statusMap.put(status.getSubjectId(), status);
		}
		return permissionState.stream().map(t -> determineDownloadAccess(userInfo, t, statusMap.get(t.getEntityId())))
				.collect(Collectors.toList());
	}

	UsersEntityAccessInfo determineDownloadAccess(UserInfo userInfo, UserEntityPermissionsState permissionState,
			SubjectStatus restrictionStatus) {
		AuthorizationStatus authroizationStatus = null;
		if (userInfo.isAdmin()) {
			authroizationStatus = AuthorizationStatus.authorized();
		} else if (!permissionState.doesEntityExist()) {
			authroizationStatus = AuthorizationStatus
					.accessDenied(new NotFoundException(THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND));
		} else if (TRASH_FOLDER_ID.equals(permissionState.getBenefactorId())) {
			authroizationStatus = AuthorizationStatus.accessDenied(new EntityInTrashCanException(
					String.format(ENTITY_S_IS_IN_TRASH_CAN_TEMPLATE, permissionState.getEntityIdAsString())));
		} else if (restrictionStatus.hasUnmet()) {
			authroizationStatus = AuthorizationStatus.accessDenied(THERE_ARE_UNMET_ACCESS_REQUIREMENTS);
		} else if (DataType.OPEN_DATA.equals(permissionState.getDataType()) && permissionState.hasRead()) {
			authroizationStatus = AuthorizationStatus.authorized();
		} else if (permissionState.hasDownload()) {
			authroizationStatus = AuthorizationStatus.authorized();
		} else {
			authroizationStatus = AuthorizationStatus.accessDenied(String.format(YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE,
					ACCESS_TYPE.DOWNLOAD.name(), permissionState.getEntityIdAsString()));
		}
		return new UsersEntityAccessInfo(permissionState.getEntityId(), authroizationStatus)
				.withAccessRestrictions(restrictionStatus);
	}

	UsersEntityAccessInfo determineUpdateAccess(UserInfo userInfo, UserEntityPermissionsState permissionState) {
		validateNotInTrash(permissionState);
		AuthorizationStatus authroizationStatus = null;
		Boolean wouldHaveAccesIfCertified = null;
		if (userInfo.isAdmin()) {
			authroizationStatus = AuthorizationStatus.authorized();
		} else if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			authroizationStatus = AuthorizationStatus.accessDenied(ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION);
		} else if (!EntityType.project.equals(permissionState.getEntityType())
				&& !AuthorizationUtils.isCertifiedUser(userInfo)) {
			authroizationStatus = AuthorizationStatus.accessDenied(ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION);
			wouldHaveAccesIfCertified = permissionState.hasUpdate();
		} else if (permissionState.hasUpdate()) {
			authroizationStatus = AuthorizationStatus.authorized();
		} else {
			authroizationStatus = AuthorizationStatus.accessDenied(String.format(YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE,
					ACCESS_TYPE.UPDATE.name(), KeyFactory.keyToString(permissionState.getEntityId())));
			wouldHaveAccesIfCertified = false;
		}
		return new UsersEntityAccessInfo(permissionState.getEntityId(), authroizationStatus)
				.withWouldHaveAccesIfCertified(wouldHaveAccesIfCertified);
	}

	UsersEntityAccessInfo determineReadAccess(UserInfo userInfo, UserEntityPermissionsState permissionState) {
		validateNotInTrash(permissionState);
		AuthorizationStatus authroizationStatus = null;
		if (userInfo.isAdmin()) {
			authroizationStatus = AuthorizationStatus.authorized();
		} else if (permissionState.hasRead()) {
			authroizationStatus = AuthorizationStatus.authorized();
		} else {
			authroizationStatus = AuthorizationStatus.accessDenied(String.format(YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE,
					ACCESS_TYPE.READ.name(), KeyFactory.keyToString(permissionState.getEntityId())));
		}
		return new UsersEntityAccessInfo(permissionState.getEntityId(), authroizationStatus);
	}

	UsersEntityAccessInfo determineCreateAccess(UserInfo userInfo, UserEntityPermissionsState permissionState,
			EntityType newEntityType) {
		AuthorizationStatus authroizationStatus = null;
		Boolean wouldHaveAccesIfCertified = null;
		if (userInfo.isAdmin()) {
			authroizationStatus = AuthorizationStatus.authorized();
		} else if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			authroizationStatus = AuthorizationStatus.accessDenied(ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION);
		} else if (newEntityType != null && !EntityType.project.equals(newEntityType)
				&& !AuthorizationUtils.isCertifiedUser(userInfo)) {
			authroizationStatus = AuthorizationStatus.accessDenied(ERR_MESSAGE_CERTIFIED_USER_CONTENT);
			wouldHaveAccesIfCertified = permissionState.hasCreate();
		} else if (permissionState.hasCreate()) {
			authroizationStatus = AuthorizationStatus.authorized();
		} else {
			authroizationStatus = AuthorizationStatus.accessDenied(String.format(YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE,
					ACCESS_TYPE.CREATE.name(), KeyFactory.keyToString(permissionState.getEntityId())));
			wouldHaveAccesIfCertified = false;
		}
		return new UsersEntityAccessInfo(permissionState.getEntityId(), authroizationStatus)
				.withWouldHaveAccesIfCertified(wouldHaveAccesIfCertified);
	}

	public static void validateNotInTrash(UserEntityPermissionsState permissionState) {
		if (TRASH_FOLDER_ID.equals(permissionState.getBenefactorId())) {
			throw new EntityInTrashCanException(
					"Entity " + KeyFactory.keyToString(permissionState.getEntityId()) + " is in trash can.");
		}
	}

}
