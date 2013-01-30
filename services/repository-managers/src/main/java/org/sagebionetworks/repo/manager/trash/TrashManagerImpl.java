package org.sagebionetworks.repo.manager.trash;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.NodeTreeQueryManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class TrashManagerImpl implements TrashManager {

	@Autowired
	private AuthorizationManager authorizationMgr;

	@Autowired
	private NodeInheritanceManager inheritanceManager;

	@Autowired
	private NodeTreeQueryManager nodeTreeMgr;

	@Override
	public void moveToTrash(UserInfo userInfo, String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		if (userInfo == null) {
			throw new IllegalArgumentException();
		}
		if (nodeId == null) {
			throw new IllegalArgumentException();
		}

		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!this.authorizationMgr.canAccess(userInfo, nodeId, ACCESS_TYPE.DELETE)) {
			throw new UnauthorizedException(userName + " lacks change access to the requested object.");
		}

		// TODO: Pass in administrator
		EntityIdList idList = this.nodeTreeMgr.getDescendants(
				userName, nodeId, TrashConstants.MAX_TRASHABLE + 1, null);
		if (idList != null && idList.getIdList().size() > TrashConstants.MAX_TRASHABLE) {
			throw new TooBigForTrashcanException(
					"Too big to fit into trashcan. Entity " + nodeId
					+ " has more than " + TrashConstants.MAX_TRASHABLE + " descendants.");
		}

	}

	@Override
	public void restoreFromTrash(UserInfo userInfo, String nodeId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public QueryResults<TrashedEntity> viewTrash(UserInfo userInfo,
			Integer offset, Integer limit) {
		// TODO Auto-generated method stub
		return null;
	}

}
