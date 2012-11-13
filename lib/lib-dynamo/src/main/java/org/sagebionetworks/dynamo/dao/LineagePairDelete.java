package org.sagebionetworks.dynamo.dao;

import org.apache.log4j.Logger;

import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * Deletes a pair of node lineage.
 *
 * @author Eric Wu
 */
class LineagePairDelete extends LineagePairWriteOperation {

	private final Logger logger = Logger.getLogger(LineagePairDelete.class);

	private final DynamoDBMapper dynamoMapper;
	private final NodeLineagePair toDelete;

	LineagePairDelete(NodeLineagePair toDelete, DynamoDBMapper dynamoMapper) {

		if (toDelete == null) {
			throw new NullPointerException();
		}
		if (dynamoMapper == null) {
			throw new NullPointerException();
		}

		this.dynamoMapper = dynamoMapper;
		this.toDelete = toDelete;
	}

	@Override
	int getAncestorDepth() {
		return this.toDelete.getAncestorDepth();
	}

	@Override
	int getDistance() {
		return this.toDelete.getDistance();
	}

	@Override
	String getAncestorId() {
		return this.toDelete.getAncestorId();
	}

	@Override
	String getDescendantId() {
		return this.toDelete.getDescendantId();
	}

	@Override
	public boolean write(final int step) {
		// Enforce an order such that a2d is execute first before d2a
		DboNodeLineage a2d = this.toDelete.getAncestor2Descendant();
		boolean a2dSuccess = this.deleteNodeLineage(a2d);
		DboNodeLineage d2a = this.toDelete.getDescendant2Ancestor();
		boolean d2aSuccess = this.deleteNodeLineage(d2a);
		return a2dSuccess && d2aSuccess;
	}

	@Override
	public void restore(final int step) {
		//
		// No restore until we are sure:
		// 1) Restore does not write incorrect data
		// 2) Restore does cause race conditions or deadlocks
		//
		// This restore shouldn't be triggered with the following measures in place:
		// 1) Writes are executed in predetermined order from the root
		// 2) Writing a child-to-parent pair requires a complete path from the root to the parent
		// We are logging it in case it happens
		//
		logger.info("Restoring at step " + step + " for node lineage pair " + this.toDelete);
	}

	private boolean deleteNodeLineage(final DboNodeLineage toDelete) {
		assert toDelete != null;
		// TODO: What happens when another thread already deletes the record?
		try {
			this.dynamoMapper.delete(toDelete);
			return true;
		} catch (ConditionalCheckFailedException e) {
			logger.error("DELETE failed for NodeLineage " + toDelete);
			return false;
		}
	}
}
