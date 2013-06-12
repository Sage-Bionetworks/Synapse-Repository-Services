package org.sagebionetworks.dynamo.workers;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeQueryDao;
import org.sagebionetworks.repo.manager.dynamo.NodeTreeUpdateManager;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeParentRelation;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Synchronizes RDS and DynamoDB on randomly selected nodes.
 */
public class DynamoRdsSynchronizer {

	/** How many nodes to synchronize in one batch */
	static final long BATCH_SIZE = 30L;

	@Autowired
	private Consumer consumer;

	private final NodeDAO nodeDao;
	private final NodeTreeQueryDao nodeTreeQueryDao;
	private final NodeTreeUpdateManager nodeTreeUpdateManager;

	private final Random random;
	private int count = 1;

	public DynamoRdsSynchronizer(NodeDAO nodeDao, NodeTreeQueryDao nodeTreeQueryDao,
			NodeTreeUpdateManager nodeTreeUpdateManager) {

		if (nodeDao == null) {
			throw new IllegalArgumentException("nodeDao cannot be null");
		}
		if (nodeTreeQueryDao == null) {
			throw new IllegalArgumentException("nodeTreeQueryDao cannot be null");
		}
		if (nodeTreeUpdateManager == null) {
			throw new IllegalArgumentException("nodeTreeUpdateManager cannot be null");
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
		final int r = random.nextInt(count);
		final QueryResults<NodeParentRelation> results = nodeDao.getParentRelations(r, BATCH_SIZE);
		final Date date = new Date();

		// Update the count to be used at next trigger
		// Occasionally count may be out-of-date and out-of-range, in which
		// case the check will be skipped
		count = (int)results.getTotalNumberOfResults();
		if (count == 0) {
			count = 1;
		}

		// Now cross check with DynamoDB
		List<NodeParentRelation> list = results.getResults();
		addMetric("TotalSync", list.size());
		for (NodeParentRelation childParent : list) {

			String childInRds = childParent.getId();
			String parentInRds = childParent.getParentId();
			String childKeyInRds = KeyFactory.stringToKey(childInRds).toString();
			String parentKeyInDynamo = nodeTreeQueryDao.getParent(childKeyInRds);
			if (parentKeyInDynamo == null) {
				// The child does not exist in DynamoDB yet
				addMetric("MissingNode", 1);
				nodeTreeUpdateManager.create(childInRds, parentInRds, date);
				return;
			}

			if (parentInRds == null) {
				// Check against the root
				if (!nodeTreeQueryDao.isRoot(childKeyInRds)) {
					addMetric("IncorrectRoot", 1);
					nodeTreeUpdateManager.update(childKeyInRds, childKeyInRds, date);
				}
				return;
			}

			String parentKeyInRds = KeyFactory.stringToKey(parentInRds).toString();
			if (!parentKeyInDynamo.equals(parentKeyInRds)) {
				// Implies that the child is pointing to the wrong parent
				addMetric("IncorrectParent", 1);;
				nodeTreeUpdateManager.update(childInRds, parentInRds, date);
			}
		}
	}

	private void addMetric(String name, long count) {
		ProfileData profileData = new ProfileData();
		profileData.setNamespace("DynamoRdsSynchronizer");
		profileData.setName(name); // Method name
		profileData.setLatency(count);
		profileData.setUnit("Count");
		profileData.setTimestamp(new Date());
		consumer.addProfileData(profileData);
	}
}
