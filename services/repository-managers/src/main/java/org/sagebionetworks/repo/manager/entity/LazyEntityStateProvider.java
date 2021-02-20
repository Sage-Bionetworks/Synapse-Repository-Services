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
	private List<Long> entityIds;
	private UserInfo userInfo;
	private List<UserEntityPermissionsState> userEntityPermissionsState;
	private Map<Long, UsersRestrictionStatus> usersRestrictionStatus;

	public LazyEntityStateProvider(AccessRestrictionStatusDao accessRestrictionStatusDao,
			UsersEntityPermissionsDao usersEntityPermissionsDao, UserInfo userInfo, List<Long> entityIds) {
		super();
		this.accessRestrictionStatusDao = accessRestrictionStatusDao;
		this.usersEntityPermissionsDao = usersEntityPermissionsDao;
		this.entityIds = entityIds;
		this.userInfo = userInfo;
	}

	@Override
	public List<UserEntityPermissionsState> getUserEntityPermissionsState() {
		if (userEntityPermissionsState == null) {
			userEntityPermissionsState = usersEntityPermissionsDao.getEntityPermissions(this.userInfo.getGroups(),
					entityIds);
		}
		return userEntityPermissionsState;
	}

	@Override
	public UsersRestrictionStatus getUserRestrictionStatus(Long entityId) {
		if (usersRestrictionStatus == null) {
			usersRestrictionStatus = accessRestrictionStatusDao.getEntityStatusAsMap(entityIds, userInfo.getId());
		}
		return usersRestrictionStatus.get(entityId);
	}

}
