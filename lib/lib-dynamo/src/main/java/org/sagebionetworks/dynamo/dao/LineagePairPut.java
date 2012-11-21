package org.sagebionetworks.dynamo.dao;

import org.apache.log4j.Logger;

import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * Puts a pair of node lineage.
 *
 * @author Eric Wu
 */
class LineagePairPut extends LineagePairWriteOperation {

	private final Logger logger = Logger.getLogger(LineagePairPut.class);

	private final DynamoDBMapper dynamoMapper;
	private final NodeLineagePair toPut;

	LineagePairPut(NodeLineagePair toPut, DynamoDBMapper dynamoMapper) {

		if (toPut == null) {
			throw new NullPointerException();
		}
		if (dynamoMapper == null) {
			throw new NullPointerException();
		}

		this.dynamoMapper = dynamoMapper;
		this.toPut = toPut;
	}

	@Override
	int getAncestorDepth() {
		return this.toPut.getAncestorDepth();
	}

	@Override
	int getDistance() {
		return this.toPut.getDistance();
	}

	@Override
	String getAncestorId() {
		return this.toPut.getAncestorId();
	}

	@Override
	String getDescendantId() {
		return this.toPut.getDescendantId();
	}

	@Override
	public boolean write(final int step) {
		// Enforce an order such that a2d is execute first before d2a
		DboNodeLineage a2d = this.toPut.getAncestor2Descendant();
		boolean a2dSuccess = this.putNodeLineage(a2d);
		DboNodeLineage d2a = this.toPut.getDescendant2Ancestor();
		boolean d2aSuccess = this.putNodeLineage(d2a);
		return a2dSuccess && d2aSuccess;
	}

	@Override
	public void restore(final int step) {
		//
		// No restore until we are sure:
		// 1) Restore does not write incorrect data
		// 2) Restore does end up with race conditions or deadlocks
		//
		// This restore shouldn't be triggered with the following measures in place:
		// 1) Writes are executed in predetermined order from the root
		// 2) Writing a child-to-parent pair requires a complete path from the root to the parent
		// We are logging it in case it happens
		//
		logger.info("Restoring at step " + step + " for node lineage pair " + this.toPut);
	}

	private boolean putNodeLineage(final DboNodeLineage toPut) {
		assert toPut != null;
		// As we are putting a new item, the put item's version number is null
		// Once put is done, the item's version is updated by 1.
		// If another thread has already put the item and the item exists with
		// some version number, this put will fail with ConditionalCheckFailedException.
		// That is the exception we catch and attempt a rolling back.
		try {
			this.dynamoMapper.save(toPut);
			return true;
		} catch (ConditionalCheckFailedException e) {
			logger.error("PUT failed for NodeLineage " + toPut
					+ ". Got error: " + e.getMessage());
			return false;
		}
	}
}
