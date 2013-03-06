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
	public void moveToTrash(String currentUserId, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo currentUser = userManager.getUserInfo(currentUserId);
		trashManager.moveToTrash(currentUser, entityId);
	}

	@Override
	public void restoreFromTrash(String currentUserId, String entityId, String newParentId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo currentUser = userManager.getUserInfo(currentUserId);
		trashManager.restoreFromTrash(currentUser, entityId, newParentId);
	}

	@Override
	public PaginatedResults<TrashedEntity> viewTrashForUser(String currentUserId, String userId,
			Long offset, Long limit, HttpServletRequest request)
			throws DatastoreException, NotFoundException, UnauthorizedException {

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

		UserInfo currentUser = userManager.getUserInfo(currentUserId);
		UserInfo user = userManager.getUserInfo(userId);
		QueryResults<TrashedEntity> trashEntities = trashManager.viewTrashForUser(
				currentUser, user, offset, limit);
		String url = request.getRequestURL() == null ? "" : request.getRequestURL().toString();
		return new PaginatedResults<TrashedEntity>(url, trashEntities.getResults(),
				trashEntities.getTotalNumberOfResults(), offset, limit, null, false);
	}

	@Override
	public PaginatedResults<TrashedEntity> viewTrash(String currentUserId,
			Long offset, Long limit, HttpServletRequest request)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo currentUser = userManager.getUserInfo(currentUserId);
		QueryResults<TrashedEntity> trashEntities = trashManager.viewTrash(
				currentUser, offset, limit);
		String url = request.getRequestURL() == null ? "" : request.getRequestURL().toString();
		return new PaginatedResults<TrashedEntity>(url, trashEntities.getResults(),
				trashEntities.getTotalNumberOfResults(), offset, limit, null, false);
	}

	@Override
	public void purgeTrashForUser(String currentUserId, String entityId) throws DatastoreException,
			NotFoundException {
		UserInfo currentUser = userManager.getUserInfo(currentUserId);
		trashManager.purgeTrashForUser(currentUser, entityId);
	}

	@Override
	public void purgeTrashForUser(String currentUserId) throws DatastoreException,
			NotFoundException {
		UserInfo currentUser = userManager.getUserInfo(currentUserId);
		trashManager.purgeTrashForUser(currentUser);
	}

	@Override
	public void purgeTrash(String currentUserId) throws DatastoreException,
			NotFoundException, UnauthorizedException {
		UserInfo currentUser = userManager.getUserInfo(currentUserId);
		trashManager.purgeTrash(currentUser);
	}
}
