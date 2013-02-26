package org.sagebionetworks.dynamo.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.dynamo.dao.nodetree.IncompletePathException;
import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeDao;
import org.sagebionetworks.dynamo.dao.nodetree.ObsoleteChangeException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Default implementation of the node tree update manager.
 *
 * @author Eric Wu
 */
public class NodeTreeUpdateManagerImpl implements NodeTreeUpdateManager {

	private final Logger logger = Logger.getLogger(NodeTreeUpdateManagerImpl.class);

	private final NodeTreeDao nodeTreeDao;
	private final NodeDAO nodeDao;

	public NodeTreeUpdateManagerImpl(NodeTreeDao nodeTreeDao, NodeDAO nodeDao) {

		if (nodeTreeDao == null) {
			throw new NullPointerException("nodeTreeDao cannot be null");
		}
		if (nodeDao == null) {
			throw new NullPointerException("nodeDao cannot be null");
		}

		this.nodeTreeDao = nodeTreeDao;
		this.nodeDao = nodeDao;
	}

	@Override
	public void create(String childId, String parentId, Date timestamp) {

		if (childId == null) {
			throw new NullPointerException("childId cannot be null");
		}
		// Verify with RDS. This is mainly to skip fake messages created by other tests
		if (!isProd() && !this.nodeExists(childId)) {
			this.logger.info("The child " + childId + " does not exist in RDS. Message to be dropped.");
			this.nodeTreeDao.delete(KeyFactory.stringToKey(childId).toString(), timestamp);
			return;
		}
		if (parentId == null) {
			// The root
			parentId = childId;
		}
		if (timestamp == null) {
			throw new NullPointerException("timestamp cannot be null");
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
			this.rebuildPath(childId, parentId);
		} catch (ObsoleteChangeException e) {
			this.logger.info("Creating node " + childId +
					" failed because of obsolete timestamp [" + timestamp + "]. Now rebuilding the path.");
			this.rebuildPath(childId, parentId);
		}
	}

	@Override
	public void update(String childId, String parentId,  Date timestamp) {

		if (childId == null) {
			throw new NullPointerException("childId cannot be null");
		}
		// Verify with RDS. This is mainly to skip fake messages created by other tests
		if (!isProd() && !this.nodeExists(childId)) {
			this.logger.info("The child " + childId + " does not exist in RDS. Message to be dropped.");
			this.nodeTreeDao.delete(KeyFactory.stringToKey(childId).toString(), timestamp);
			return;
		}
		if (parentId == null) {
			// The root
			parentId = childId;
		}
		if (timestamp == null) {
			throw new NullPointerException("timestamp cannot be null");
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
			this.rebuildPath(childId, parentId);
		} catch (ObsoleteChangeException e) {
			this.logger.info("Updating node " + childId +
					" failed because of obsolete timestamp [" + timestamp + "]. Now rebuilding the path.");
			this.rebuildPath(childId, parentId);
		}
	}

	@Override
	public void delete(String nodeId,  Date timestamp) {

		if (nodeId == null) {
			throw new NullPointerException("nodeId cannot be null");
		}
		if (timestamp == null) {
			throw new NullPointerException("timestamp cannot be null");
		}

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
	private void rebuildPath(String childId, String parentId) throws DatastoreException {
		try {
			this.logger.info("Rebuilding path for node " + childId);
			// Get the path from the root to the parent
			List<EntityHeader> ehList = this.nodeDao.getEntityPath(childId);
			List<String> path = new ArrayList<String>(ehList.size());
			for (EntityHeader eh : ehList) {
				path.add(eh.getId());
			}
			// Add the child to the path
			path.add(childId);
			for (int i = 0; i < path.size(); i++) {
				if (i == 0) {
					// Handling the root
					String rootId = path.get(i);
					String rId = KeyFactory.stringToKey(rootId).toString();
					this.nodeTreeDao.create(rId, rId, new Date());
				} else {
					String cId = KeyFactory.stringToKey(path.get(i)).toString();
					String pId = KeyFactory.stringToKey(path.get(i - 1)).toString();
					this.nodeTreeDao.create(cId, pId, new Date());
				}
			}
		} catch (NotFoundException e) {
			// We are getting a broken path anyways
			// Log an error and drop the message
			this.logger.error("Node " + parentId + " is not on have a complete path. Message dropped.", e);
		}
	}

	/**
	 * Calls RDS to verify and retry delete.
	 */
	private void retryDelete(String nodeId) throws DatastoreException {
		try {
			this.logger.info("Now retry deleting node " + nodeId);
			this.nodeDao.getNode(nodeId);
			this.logger.info("Node " + nodeId + " exists. Aborting the delete message.");
		} catch (NotFoundException e) {
			String cId = KeyFactory.stringToKey(nodeId).toString();
			Date timestamp = new Date();
			this.logger.info("Node " + nodeId + " does not exist. Deleting it again with timestamp [" + timestamp + "].");
			this.nodeTreeDao.delete(cId, timestamp);
			this.logger.info("Node " + nodeId + " successfully deleted.");
		}
	}

	/**
	 * Whether the node exists in RDS
	 */
	private boolean nodeExists(String node) {
		Long nodeId = KeyFactory.stringToKey(node);
		return this.nodeDao.doesNodeExist(nodeId);
	}

	/**
	 * Is this production?
	 */
	private boolean isProd() {
		String stack = StackConfiguration.getStack();
		if ("prod".equalsIgnoreCase(stack)) {
			return true;
		}
		return false;
	}
}
