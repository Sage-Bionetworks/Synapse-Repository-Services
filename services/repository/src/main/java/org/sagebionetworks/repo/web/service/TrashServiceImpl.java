package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class TrashServiceImpl implements TrashService {

	@Autowired
	private UserManager userManager;

	@Autowired
	private TrashManager trashManager;

	@Override
	public void moveToTrash(String userId, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		trashManager.moveToTrash(userInfo, entityId);
	}

	@Override
	public void restoreFromTrash(String userId, String entityId, String newParentId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		trashManager.restoreFromTrash(userInfo, entityId, newParentId);
	}

	@Override
	public PaginatedResults<TrashedEntity> viewTrash(String userId, Long offset, Long limit,
			HttpServletRequest request) throws DatastoreException, NotFoundException {

		if (offset == null){
			offset = Long.valueOf(0L);
		}
		if (limit == null){
			limit = Long.valueOf(10L);
		}
		if (offset < 0) {
			throw new IllegalArgumentException(
					"pagination offset must be 0 or greater");
		}
		if (limit < 0) {
			throw new IllegalArgumentException(
					"pagination limit must be 0 or greater");
		}

		UserInfo userInfo = userManager.getUserInfo(userId);
		QueryResults<TrashedEntity> trashEntities = trashManager.viewTrashForUser(userInfo, userInfo, offset, limit);
		String url = request.getRequestURL() == null ? "" : request.getRequestURL().toString();
		return new PaginatedResults<TrashedEntity>(url, trashEntities.getResults(),
				trashEntities.getTotalNumberOfResults(), offset, limit, null, false);
	}

	@Override
	public void purge(String userId, String nodeId) throws DatastoreException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		trashManager.purgeNodeForUser(userInfo, nodeId);
	}

	@Override
	public void purge(String userId) throws DatastoreException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		trashManager.purgeAllForUser(userInfo);
	}
}
