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
 *
 * @author Eric Wu
 */
public class DynamoRdsSynchronizer implements Runnable {

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

	@Override
	public void run() {
		// Call the original method.
		triggerFired();
	}
	
	public void triggerFired() {

		// Get a random node and its parent from RDS
		// This is returns a value between 0 (inclusive) and count (exclusive)
		// The value range is consistent with the MySQL OFFSET parameter
		int r = this.random.nextInt(this.count);
		QueryResults<NodeParentRelation> results = this.nodeDao.getParentRelations(r, 5);
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
		this.addMetric("TotalSync", list.size());
		for (NodeParentRelation childParent : list) {

			String childInRds = childParent.getId();
			String parentInRds = childParent.getParentId();
			String childKeyInRds = KeyFactory.stringToKey(childInRds).toString();
			String parentKeyInDynamo = this.nodeTreeQueryDao.getParent(childKeyInRds);
			if (parentKeyInDynamo == null) {
				// The child does not exist in DynamoDB yet
				this.addMetric("MissingNode", 1);
				this.nodeTreeUpdateManager.create(childInRds, parentInRds, date);
				return;
			}

			if (parentInRds == null) {
				// Check against the root
				if (!this.nodeTreeQueryDao.isRoot(childKeyInRds)) {
					this.addMetric("IncorrectRoot", 1);
					this.nodeTreeUpdateManager.update(childKeyInRds, childKeyInRds, date);
				}
				return;
			}

			String parentKeyInRds = KeyFactory.stringToKey(parentInRds).toString();
			if (!parentKeyInDynamo.equals(parentKeyInRds)) {
				// Implies that the child is pointing to the wrong parent
				this.addMetric("IncorrectParent", 1);;
				this.nodeTreeUpdateManager.update(childInRds, parentInRds, date);
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
