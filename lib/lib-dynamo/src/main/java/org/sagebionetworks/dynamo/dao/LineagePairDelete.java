package org.sagebionetworks.dynamo.dao;

import org.apache.log4j.Logger;

import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;

/**
 * Deletes a pair of node lineage.
 *
 * @author Eric Wu
 */
class LineagePairDelete extends LineagePairWriteOperation {

	private final Logger logger = Logger.getLogger(LineagePairDelete.class);

	private final DynamoDBMapper dynamoMapper;
	private final NodeLineagePair toDelete;
	private NodeLineage a2dOriginal;
	private NodeLineage d2aOriginal;

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
	public boolean write() {
		try {
			NodeLineage a2d = this.toDelete.getAncestor2Descendant();
			this.a2dOriginal = this.deleteNodeLineage(a2d);
			NodeLineage d2a = this.toDelete.getDescendant2Ancestor();
			this.d2aOriginal = this.deleteNodeLineage(d2a);
			return true;
		} catch (Throwable t) {
			logger.error("DynamoDB DELETE failed with exception. " + t);
			return false;
		}
	}

	@Override
	public void restore() {
		NodeLineage a2d = this.toDelete.getAncestor2Descendant();
		this.createNodeLineage(this.a2dOriginal, a2d);
		NodeLineage d2a = this.toDelete.getDescendant2Ancestor();
		this.createNodeLineage(this.d2aOriginal, d2a);
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

	private NodeLineage deleteNodeLineage(NodeLineage toDelete) {
		assert toDelete != null;
		String hashKey = toDelete.getHashKey();
		String rangeKey = toDelete.getRangeKey();
		NodeLineage original = this.dynamoMapper.load(NodeLineage.class, hashKey, rangeKey);
		if (original != null) {
			// Not already deleted
			this.dynamoMapper.delete(original);
		}
		return original;
	}

	private void createNodeLineage(NodeLineage original, NodeLineage toDelete) {
		assert toDelete != null;
		if (original != null) {
			String hashKey = toDelete.getHashKey();
			String rangeKey = toDelete.getRangeKey();
			NodeLineage deleted = this.dynamoMapper.load(NodeLineage.class, hashKey, rangeKey);
			if (deleted == null) {
				this.dynamoMapper.save(original);
			}
		}
	}
}
