package org.sagebionetworks.repo.manager.trash;

import java.util.List;

import org.sagebionetworks.dynamo.dao.NodeTreeDao;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.NodeManagerImpl;
import org.sagebionetworks.repo.manager.NodeTreeQueryManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class TrashManagerImpl implements TrashManager {

	@Autowired
	private AuthorizationManager authorizationMgr;

	@Autowired
	private NodeInheritanceManager inheritanceManager;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private NodeTreeDao nodeTreeDao;

	@Override
	public void moveToTrash(UserInfo userInfo, String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		if (userInfo == null) {
			throw new IllegalArgumentException();
		}
		if (nodeId == null) {
			throw new IllegalArgumentException();
		}

		// Access control
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!this.authorizationMgr.canAccess(userInfo, nodeId, ACCESS_TYPE.DELETE)) {
			throw new UnauthorizedException(userName + " lacks change access to the requested object.");
		}

		// Whether it is too big for the trash can
		List<String> idList = this.nodeTreeDao.getDescendants(
				KeyFactory.stringToKey(nodeId).toString(), TrashConstants.MAX_TRASHABLE + 1, null);
		if (idList != null && idList.size() > TrashConstants.MAX_TRASHABLE) {
			throw new TooBigForTrashcanException(
					"Too big to fit into trashcan. Entity " + nodeId
					+ " has more than " + TrashConstants.MAX_TRASHABLE + " descendants.");
		}


		/*
		// Now lock this node
		String nextETag = nodeDao.lockNodeAndIncrementEtag(updatedNode.getId(), updatedNode.getETag());

		// clear node creation data.  this tells NodeDAO not to change the fields
		NodeManagerImpl.clearNodeCreationDataForUpdate(updatedNode);
		// Clear the modified data and fill it in with the new data
		NodeManagerImpl.validateNodeModifiedData(userIndividualGroupId, updatedNode);
		
		nodeDao.changeNodeParent(updatedNode.getId(), updatedNode.getParentId());
		nodeInheritanceManager.nodeParentChanged(updatedNode.getId(), updatedNode.getParentId());
		
		// Now make the actual update.
		nodeDao.updateNode(updatedNode);
		*/

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
