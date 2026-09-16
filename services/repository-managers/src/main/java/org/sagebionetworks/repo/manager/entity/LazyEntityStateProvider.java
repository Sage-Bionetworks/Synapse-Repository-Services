package org.sagebionetworks.repo.manager.entity;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.AccessRestrictionStatusDao;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.repo.model.dbo.entity.UsersEntityPermissionsDao;

/**
 * A lazy loading/caching implementation of EntityStateProvider. The data is
 * only loaded from the database once, as needed. Note: This object is
 * state-full and should be used for a single user request and then discarded.
 *
 */
public class LazyEntityStateProvider implements EntityStateProvider {

	private AccessRestrictionStatusDao accessRestrictionStatusDao;
	private UsersEntityPermissionsDao usersEntityPermissionsDao;
	private ConditionalAccessRequirementResolver conditionalAccessRequirementResolver;
	private List<Long> entityIds;
	private UserInfo userInfo;
	private Map<Long, UserEntityPermissionsState> userEntityPermissionsState;
	private Map<Long, UsersRestrictionStatus> usersRestrictionStatus;

	public LazyEntityStateProvider(AccessRestrictionStatusDao accessRestrictionStatusDao,
			UsersEntityPermissionsDao usersEntityPermissionsDao,
			ConditionalAccessRequirementResolver conditionalAccessRequirementResolver, UserInfo userInfo,
			List<Long> entityIds) {
		super();
		this.accessRestrictionStatusDao = accessRestrictionStatusDao;
		this.usersEntityPermissionsDao = usersEntityPermissionsDao;
		this.conditionalAccessRequirementResolver = conditionalAccessRequirementResolver;
		this.entityIds = entityIds;
		this.userInfo = userInfo;
	}

	@Override
	public UserEntityPermissionsState getPermissionsState(Long entityId) {
		if (userEntityPermissionsState == null) {
			userEntityPermissionsState = usersEntityPermissionsDao.getEntityPermissionsAsMap(this.userInfo,
					entityIds);
		}
		return userEntityPermissionsState.get(entityId);
	}

	@Override
	public UsersRestrictionStatus getRestrictionStatus(Long entityId) {
		if (usersRestrictionStatus == null) {
			usersRestrictionStatus = accessRestrictionStatusDao.getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
			// The query reports a conditional requirement as unmet, since it is not met by an approval it
			// could find. Resolving here, once for the whole batch, means every caller sees the
			// requirement as met when this caller has satisfied its condition.
			conditionalAccessRequirementResolver.resolveUnmetConditions(userInfo, usersRestrictionStatus);
		}
		return usersRestrictionStatus.get(entityId);
	}

	@Override
	public List<Long> getEntityIds() {
		return entityIds;
	}

}
