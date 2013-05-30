package org.sagebionetworks.dynamo.workers;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeQueryDao;
import org.sagebionetworks.dynamo.manager.NodeTreeUpdateManager;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeParentRelation;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

/**
 * Synchronizes RDS and DynamoDB on randomly selected nodes.
 *
 * @author Eric Wu
 */
public class DynamoRdsSynchronizer {

	private final Logger logger = Logger.getLogger(DynamoRdsSynchronizer.class);

	private final NodeDAO nodeDao;
	private final NodeTreeQueryDao nodeTreeQueryDao;
	private final NodeTreeUpdateManager nodeTreeUpdateManager;

	private final Random random;
	private int count = 1;

	public DynamoRdsSynchronizer(NodeDAO nodeDao, NodeTreeQueryDao nodeTreeQueryDao,
			NodeTreeUpdateManager nodeTreeUpdateManager) {

		if (nodeDao == null) {
			throw new NullPointerException("nodeDao cannot be null");
		}
		if (nodeTreeQueryDao == null) {
			throw new NullPointerException("nodeTreeQueryDao cannot be null");
		}
		if (nodeTreeUpdateManager == null) {
			throw new NullPointerException("nodeTreeUpdateManager cannot be null");
		}

		this.nodeDao = nodeDao;
		this.nodeTreeQueryDao = nodeTreeQueryDao;
		this.nodeTreeUpdateManager = nodeTreeUpdateManager;
		this.random = new SecureRandom();
	}

	public void triggerFired() {

		// Get a random node and its parent from RDS
		// This is returns a value between 0 (inclusive) and count (exclusive)
		// The value range is consistent with the MySQL OFFSET parameter
		int r = this.random.nextInt(this.count);
		QueryResults<NodeParentRelation> results = this.nodeDao.getParentRelations(r, 1);
		Date date = new Date();

		// Update the count to be used at next trigger
		// Occasionally count may be out-of-date and out-of-range, in which
		// case the check will be skipped
		this.count = (int)results.getTotalNumberOfResults();
		if (this.count == 0) {
			this.count = 1;
		}

		// Now cross check with DynamoDB
		List<NodeParentRelation> list = results.getResults();
		if (list != null && list.size() > 0) {

			NodeParentRelation childParent = list.get(0);
			String childInRds = childParent.getId();
			String parentInRds = childParent.getParentId();
			String childKeyInRds = KeyFactory.stringToKey(childInRds).toString();
			String parentKeyInDynamo = this.nodeTreeQueryDao.getParent(childKeyInRds);
			if (parentKeyInDynamo == null) {
				// The child does not exist in DynamoDB yet
				this.logger.info("Dynamo is missing " + childInRds);
				this.nodeTreeUpdateManager.create(childInRds, parentInRds, date);
				return;
			}

			if (parentInRds == null) {
				// Check against the root
				if (!this.nodeTreeQueryDao.isRoot(childKeyInRds)) {
					this.logger.info("RDS's root node " + childKeyInRds + " is missing in Dynamo.");
				}
				this.nodeTreeUpdateManager.create(childKeyInRds, childKeyInRds, date);
				return;
			}

			String parentKeyInRds = KeyFactory.stringToKey(parentInRds).toString();
			if (!parentKeyInDynamo.equals(parentKeyInRds)) {
				// Implies that the child is pointing to the wrong parent
				this.logger.info("Child " + childKeyInRds
						+ " is pointing to the wrong parent " + parentKeyInDynamo
						+ ". It should point to " + parentKeyInRds + " instead.");
				this.nodeTreeUpdateManager.update(childInRds, parentInRds, date);
			}
		}
	}
}
