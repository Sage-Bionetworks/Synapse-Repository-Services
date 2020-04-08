package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class TrashServiceImpl implements TrashService {

	@Autowired
	private TrashManager trashManager;

	@Override
	public void moveToTrash(UserInfo userInfo, String entityId, boolean priorityPurge)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		trashManager.moveToTrash(userInfo, entityId, priorityPurge);
	}

	@Override
	public void restoreFromTrash(UserInfo currentUser, String entityId, String newParentId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		trashManager.restoreFromTrash(currentUser, entityId, newParentId);
	}

	@Override
	public PaginatedResults<TrashedEntity> viewTrashForUser(UserInfo userInfo,
			Long offset, Long limit)
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

		List<TrashedEntity> trashEntities = trashManager.listTrashedEntities(
				userInfo, userInfo.getId().toString(), offset, limit);
		return PaginatedResults.createWithLimitAndOffset(trashEntities, limit, offset);
	}

	@WriteTransaction
	@Override
	public void flagForPurge(UserInfo currentUser, String entityId) throws DatastoreException, NotFoundException {
		trashManager.flagForPurge(currentUser, entityId);
	}

}
