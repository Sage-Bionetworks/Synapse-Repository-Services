package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ServiceConstants;
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
	public PaginatedResults<TrashedEntity> viewTrash(String userId, Integer offset, Integer limit,
			HttpServletRequest request) throws DatastoreException, NotFoundException {

		if(offset == null){
			offset = 1;
		}
		if(limit == null){
			limit = 10;
		}
		ServiceConstants.validatePaginationParams((long)offset, (long)limit);

		UserInfo userInfo = userManager.getUserInfo(userId);
		QueryResults<TrashedEntity> trashEntities = trashManager.viewTrash(userInfo, offset, limit);
		String url = request.getRequestURL() == null ? "" : request.getRequestURL().toString();
		return new PaginatedResults<TrashedEntity>(url, trashEntities.getResults(),
				trashEntities.getTotalNumberOfResults(), offset, limit, null, false);
	}
}
