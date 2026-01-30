package org.sagebionetworks.repo.manager.grid;

import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class GridAuthorizationManagerImpl implements GridAuthorizationManager {

	private final GridDao gridDao;
	private final EntityAuthorizationManager entityAuthorizationManager;

	public GridAuthorizationManagerImpl(GridDao gridDao, EntityAuthorizationManager entityAuthorizationManager) {
		this.gridDao = gridDao;
		this.entityAuthorizationManager = entityAuthorizationManager;
	}

	@Override
	public UserInfo getRowLevelFilterUserInfo(UserInfo user, String gridSessionId) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(gridSessionId, "gridSessionId");

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

		return createFilterUser(ownerId);
	}

	private GridSource getGridSource(String gridSessionId) {
		return gridDao.getSessionSource(gridSessionId)
				.orElseThrow(() -> new NotFoundException("Grid does not have a source"));
	}

	private UserInfo createFilterUser(Long ownerId) {
		UserInfo filterUser = new UserInfo(false, ownerId);
		filterUser.setGroups(Set.of(ownerId, BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId(),
				BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId()));
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

		Long ownerId = getGridOwner(gridSessionId);
		if (!isAuthorizedUser(user, ownerId)) {
			return AuthorizationStatus.accessDenied("You are not authorized to access this resource.");
		}

		Optional<GridSource> sourceOp = gridDao.getSessionSource(gridSessionId);
		if (sourceOp.isPresent()) {
			return checkSourceAccess(user, sourceOp.get());
		}
		return AuthorizationStatus.authorized();
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
