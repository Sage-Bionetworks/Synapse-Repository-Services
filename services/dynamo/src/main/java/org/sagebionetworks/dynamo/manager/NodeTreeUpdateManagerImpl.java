package org.sagebionetworks.dynamo.manager;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.sagebionetworks.dynamo.dao.IncompletePathException;
import org.sagebionetworks.dynamo.dao.NodeTreeDao;
import org.sagebionetworks.dynamo.dao.ObsoleteChangeException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Default implementation of the node tree update manager.
 *
 * @author Eric Wu
 */
public class NodeTreeUpdateManagerImpl implements NodeTreeUpdateManager {

	private final Logger logger = Logger.getLogger(NodeTreeUpdateManagerImpl.class);

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private NodeTreeDao nodeTreeDao;

	@Override
	public void create(String childId, String parentId, Date timestamp) {

		if (childId == null) {
			throw new NullPointerException();
		}
		if (parentId == null) {
			// The root
			parentId = childId;
		}

		try {
			String cId = KeyFactory.stringToKey(childId).toString();
			String pId = KeyFactory.stringToKey(parentId).toString();
			boolean success = this.nodeTreeDao.create(cId, pId, timestamp);
			if (!success) {
				String childParent = "("+ childId + ", " + parentId + ")";
				// This is due to optimistic locking which should rarely happen
				// Retry just once
				this.logger.info("Locking detected. Retry creating child-parent pair " + childParent);
				success = this.nodeTreeDao.create(cId, pId, timestamp);
				if (!success) {
					throw new RuntimeException("Create failed for child-parent pair " + childParent);
				}
			}
		} catch (IncompletePathException e) {
			this.logger.info("Node " + childId + " path is incomplete. Now rebuilding the path.");
			this.rebuildPath(childId);
		} catch (ObsoleteChangeException e) {
			this.logger.info("Creating node " + childId +
					" failed because of obsolete timestamp [" + timestamp + "]. Now rebuilding the path.");
			this.rebuildPath(childId);
		}
	}

	@Override
	public void update(String childId, String parentId,  Date timestamp) {

		if (childId == null) {
			throw new NullPointerException();
		}
		if (parentId == null) {
			// The root
			parentId = childId;
		}

		try {
			String cId = KeyFactory.stringToKey(childId).toString();
			String pId = KeyFactory.stringToKey(parentId).toString();
			boolean success = this.nodeTreeDao.update(cId, pId, timestamp);
			if (!success) {
				String childParent = "("+ childId + ", " + parentId + ")";
				// This is due to optimistic locking which should rarely happen
				// Retry just once
				this.logger.info("Locking detected. Retry updating child-parent pair " + childParent);
				success = this.nodeTreeDao.update(cId, pId, timestamp);
				if (!success) {
					throw new RuntimeException("Update failed for child-parent pair " + childParent);
				}
			}
		} catch (IncompletePathException e) {
			this.logger.info("Node " + childId + " path is incomplete. Now rebuilding the path.");
			this.rebuildPath(childId);
		} catch (ObsoleteChangeException e) {
			this.logger.info("Updating node " + childId +
					" failed because of obsolete timestamp [" + timestamp + "]. Now rebuilding the path.");
			this.rebuildPath(childId);
		}
	}

	@Override
	public void delete(String nodeId,  Date timestamp) {
		try {
			String id = KeyFactory.stringToKey(nodeId).toString();
			boolean success = this.nodeTreeDao.delete(id, timestamp);
			if (!success) {
				// This is due to optimistic locking which should rarely happen
				// Retry just once
				this.logger.info("Locking detected. Retry deleting node" + nodeId);
				success = this.nodeTreeDao.delete(id, timestamp);
				if (!success) {
					throw new RuntimeException("DELETE failed for node " + nodeId);
				}
			}
		} catch (ObsoleteChangeException e) {
			this.logger.info("Deleting node " + nodeId +
					" failed because of obsolete timestamp [" + timestamp + "]");
			this.retryDelete(nodeId);
		}
	}

	/**
	 * Calls RDS to re-construct the path from the root to the specified child node.
	 */
	private void rebuildPath(String childId) throws DatastoreException {
		try {
			this.logger.info("Rebuilding path for node " + childId);
			List<EntityHeader> path = this.nodeDao.getEntityPath(childId);
			for (int i = 0; i < path.size(); i++) {
				if (i == 0) {
					// Handling the root
					String rootId = path.get(i).getId();
					String rId = KeyFactory.stringToKey(rootId).toString();
					this.nodeTreeDao.create(rId, rId, new Date());
				} else {
					EntityHeader cEntity = path.get(i);
					EntityHeader pEntity = path.get(i - 1);
					String cId = KeyFactory.stringToKey(cEntity.getId()).toString();
					String pId = KeyFactory.stringToKey(pEntity.getId()).toString();
					this.nodeTreeDao.create(cId, pId, new Date());
				}
			}
		} catch (NotFoundException e) {
			// We are getting a broken path anyways
			// Log an error and drop the message
			this.logger.error("Node " + childId + " does not have a complete path. Message dropped.", e);
		}
	}

	/**
	 * Calls RDS to verify and retry delete.
	 */
	private void retryDelete(String childId) throws DatastoreException {
		try {
			this.logger.info("Now retry deleting node " + childId);
			this.nodeDao.getNode(childId);
			this.logger.info("Node " + childId + " exists. Aborting the delete message.");
		} catch (NotFoundException e) {
			String cId = KeyFactory.stringToKey(childId).toString();
			Date timestamp = new Date();
			this.logger.info("Node " + childId + " does not exist. Deleting it again with timestamp [" + timestamp + "].");
			this.nodeTreeDao.delete(cId, timestamp);
			this.logger.info("Node " + childId + " successfully deleted.");
		}
	}
}
