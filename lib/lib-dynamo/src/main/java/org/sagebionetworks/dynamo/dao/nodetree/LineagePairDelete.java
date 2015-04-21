package org.sagebionetworks.dynamo.dao.nodetree;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;

/**
 * Deletes a pair of node lineage.
 */
class LineagePairDelete extends LineagePairWriteOperation {

	private final Logger logger = LogManager.getLogger(LineagePairDelete.class);

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
		// Both the integrity check of the tree (complete path to the root) and the pairing
		// of the node pointers rely on the upward ancestor pointers. Hence we are enforcing
		// a write order here such that the downward pointer a2d is always deleted before
		// the upward pointer d2a. This ensures the higher priority of the upward ancestor
		// pointers. In case of failures, either the upward ancestor pointer is there or both
		// the upward and the downward pointer do not exist at all.
		DboNodeLineage a2d = this.toDelete.getAncestor2Descendant();
		boolean success = this.deleteNodeLineage(a2d);
		if (!success) {
			return false;
		}
		DboNodeLineage d2a = this.toDelete.getDescendant2Ancestor();
		success = this.deleteNodeLineage(d2a);
		return success;
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
		try {
			this.dynamoMapper.delete(toDelete);
			return true;
		} catch (ConditionalCheckFailedException e) {
			DboNodeLineage dbo = this.dynamoMapper.load(DboNodeLineage.class,
					toDelete.getHashKey(), toDelete.getRangeKey());
			if (dbo == null) {
				// Note: If the record is already deleted, it also throws
				// ConditionalCheckFailedException, in which case we should ignore it
				return true;
			}
			logger.error("DELETE failed for NodeLineage " + toDelete
					+ ". Got error: " + e.getMessage());
			return false;
		}
	}
}
