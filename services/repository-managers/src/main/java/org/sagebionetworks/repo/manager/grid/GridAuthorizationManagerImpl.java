package org.sagebionetworks.repo.manager.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.model.grid.AuthorizationMode;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class GridAuthorizationManagerImpl implements GridAuthorizationManager {

	private final GridDao gridDao;
	private final EntityAuthorizationManager entityAuthorizationManager;
	private final UserGroupDAO userGroupDAO;

	public GridAuthorizationManagerImpl(GridDao gridDao, EntityAuthorizationManager entityAuthorizationManager,
			UserGroupDAO userGroupDAO) {
		this.gridDao = gridDao;
		this.entityAuthorizationManager = entityAuthorizationManager;
		this.userGroupDAO = userGroupDAO;
	}

	@Override
	public UserInfo getRowLevelFilterUserInfo(UserInfo user, String gridSessionId) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(gridSessionId, "gridSessionId");

		AuthorizationMode mode = gridDao.getAuthorizationMode(gridSessionId).orElse(AuthorizationMode.SESSION_OWNER);
		if (AuthorizationMode.SOURCE_BENEFACTOR.equals(mode)) {
			return user;
		}

		GridSource gridSource = getGridSource(gridSessionId);

		if (EntityType.table.equals(gridSource.getType())) {
			return user;
		}

		if (!EntityType.entityview.equals(gridSource.getType())) {
			throw new IllegalArgumentException("Unsupported grid source type: " + gridSource.getType());
		}

		Long ownerId = getGridOwner(gridSessionId);
		if (AuthorizationUtils.isUserCreatorOrAdmin(user, ownerId.toString())) {
			return user;
		}

		return createFilterUser(ownerId, user);
	}

	private GridSource getGridSource(String gridSessionId) {
		return gridDao.getSessionSource(gridSessionId)
				.orElseThrow(() -> new NotFoundException("Grid does not have a source"));
	}

	private UserInfo createFilterUser(Long ownerId, UserInfo realmContext) {
		UserInfo filterUser = new UserInfo(false, ownerId, realmContext.getRealmId(),
				Set.of(ownerId, realmContext.getRealmAuthenticatedUsersId(),
						realmContext.getRealmPublicUsersId()));
		filterUser.setRealmAnonymousUserId(realmContext.getRealmAnonymousUserId());
		filterUser.setRealmAuthenticatedUsersId(realmContext.getRealmAuthenticatedUsersId());
		filterUser.setRealmPublicUsersId(realmContext.getRealmPublicUsersId());
		return filterUser;
	}

	private Long getGridOwner(String gridSessionId) {
		return gridDao.getGridSessionOwner(gridSessionId)
				.orElseThrow(() -> new NotFoundException("Grid session not found: " + gridSessionId));
	}

	@Override
	public AuthorizationStatus hasGridSessionAccess(UserInfo user, String gridSessionId) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(gridSessionId, "gridSessionId");
		
		AuthorizationStatus modeStatus = modeSpecificHasAccess(user, gridSessionId);
		if(!modeStatus.isAuthorized()) {
			return modeStatus;
		}
		
		Optional<GridSource> sourceOp = gridDao.getSessionSource(gridSessionId);
		if (sourceOp.isPresent()) {
			return checkSourceAccess(user, sourceOp.get());
		}
		return AuthorizationStatus.authorized();
	}
	
	AuthorizationStatus modeSpecificHasAccess(UserInfo user, String gridSessionId) {
		AuthorizationMode mode = gridDao.getAuthorizationMode(gridSessionId).orElse(AuthorizationMode.SESSION_OWNER);
		switch (mode) {
		case SOURCE_BENEFACTOR:
			return checkAllBenefactorAccess(user, gridSessionId);
		case SESSION_OWNER:
			return checkOwnerAccess(user, gridSessionId);
		default:
			throw new IllegalStateException("Unknown type: " + mode);
		}
	}

	private AuthorizationStatus checkOwnerAccess(UserInfo user, String gridSessionId) {
		Long ownerId = getGridOwner(gridSessionId);
		return isAuthorizedUser(user, ownerId) ? AuthorizationStatus.authorized()
				: AuthorizationStatus.accessDenied("You are not authorized to access this resource.");
	}

	private AuthorizationStatus checkAllBenefactorAccess(UserInfo user, String gridSessionId) {
		Set<Long> storedBenefactorIds = gridDao.getSessionBenefactorIds(gridSessionId);
		if (storedBenefactorIds.isEmpty()) {
			return AuthorizationStatus.authorized();
		}
		List<UsersEntityAccessInfo> results = entityAuthorizationManager.batchHasAccess(
				user, new ArrayList<>(storedBenefactorIds), ACCESS_TYPE.UPDATE);
		Set<Long> accessible = results.stream()
				.filter(a -> a.getAuthorizationStatus().isAuthorized())
				.map(UsersEntityAccessInfo::getEntityId)
				.collect(Collectors.toSet());
		if (accessible.equals(storedBenefactorIds)) {
			return AuthorizationStatus.authorized();
		}
		return AuthorizationStatus.accessDenied(
				"You must have EDIT access on all source benefactors to access this grid session.");
	}

	private boolean isAuthorizedUser(UserInfo user, Long ownerId) {
		return AuthorizationUtils.isUserCreatorOrAdmin(user, ownerId.toString()) || user.getGroups().contains(ownerId);
	}

	private AuthorizationStatus checkSourceAccess(UserInfo user, GridSource source) {
		switch (source.getType()) {
		case entityview:
			return entityAuthorizationManager.hasAccess(user, source.getSourceId().toString(), ACCESS_TYPE.READ);
		case recordset:
		case table:
			return entityAuthorizationManager.hasAccess(user, source.getSourceId().toString(), ACCESS_TYPE.READ,
					ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.UPDATE);
		default:
			throw new IllegalArgumentException("Unsupported grid source type: " + source.getType());
		}
	}

	@Override
	public Long validateGridOwner(UserInfo user, String ownerString) {
		ValidateArgument.required(user, "user");
		AuthorizationUtils.disallowAnonymous(user);

		if (ownerString == null) {
			return user.getId();
		}

		Long ownerId = parseOwner(ownerString);
		if (!userGroupDAO.doesIdExist(ownerId)) {
			throw new IllegalArgumentException(
					String.format("ownerPrincipalId '%s' does not exist.", ownerString));
		}
		if (!isAuthorizedUser(user, ownerId)) {
			throw new UnauthorizedException("Caller must be a member of the owner's team.");
		}
		return ownerId;
	}

	private Long parseOwner(String ownerPrincipalId) {
		try {
			return Long.parseLong(ownerPrincipalId);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(String.format("Invalid ownerPrincipalId: '%s'", ownerPrincipalId));
		}
	}

}
