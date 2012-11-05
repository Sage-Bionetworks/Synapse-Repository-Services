package org.sagebionetworks.dynamo.dao;

import org.apache.log4j.Logger;

import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;

/**
 * Creates a pair of node lineage.
 *
 * @author Eric Wu
 */
class LineagePairCreate extends LineagePairWriteOperation {

	private final Logger logger = Logger.getLogger(LineagePairCreate.class);

	private final DynamoDBMapper dynamoMapper;
	private final NodeLineagePair toSave;
	private NodeLineage a2dOriginal;
	private NodeLineage d2aOriginal;

	LineagePairCreate(NodeLineagePair toSave, DynamoDBMapper dynamoMapper) {

		if (toSave == null) {
			throw new NullPointerException();
		}
		if (dynamoMapper == null) {
			throw new NullPointerException();
		}

		this.dynamoMapper = dynamoMapper;
		this.toSave = toSave;
	}

	@Override
	public boolean write() {
		try {
			NodeLineage a2d = this.toSave.getAncestor2Descendant();
			this.a2dOriginal = this.createNodeLineage(a2d);
			NodeLineage d2a = this.toSave.getDescendant2Ancestor();
			this.d2aOriginal = this.createNodeLineage(d2a);
			return true;
		} catch (Throwable t) {
			logger.error("DynamoDB SAVE failed with exception. " + t);
			return false;
		}
	}

	@Override
	public void restore() {
		NodeLineage a2d = this.toSave.getAncestor2Descendant();
		this.deleteNodeLineage(this.a2dOriginal, a2d);
		NodeLineage d2a = this.toSave.getDescendant2Ancestor();
		this.deleteNodeLineage(this.d2aOriginal, d2a);
	}

	@Override
	int getAncestorDepth() {
		return this.toSave.getAncestorDepth();
	}

	@Override
	int getDistance() {
		return this.toSave.getDistance();
	}

	@Override
	String getAncestorId() {
		return this.toSave.getAncestorId();
	}

	@Override
	String getDescendantId() {
		return this.toSave.getDescendantId();
	}

	private NodeLineage createNodeLineage(NodeLineage toSave) {
		assert toSave != null;
		String hashKey = toSave.getHashKey();
		String rangeKey = toSave.getRangeKey();
		NodeLineage original = this.dynamoMapper.load(NodeLineage.class, hashKey, rangeKey);
		if (original == null) {
			this.dynamoMapper.save(toSave);
		}
		return original;
	}

	private void deleteNodeLineage(NodeLineage original, NodeLineage toSave) {
		assert toSave != null;
		if (original == null) {
			String hashKey = toSave.getHashKey();
			String rangeKey = toSave.getRangeKey();
			NodeLineage saved = this.dynamoMapper.load(NodeLineage.class, hashKey, rangeKey);
			if (saved != null) {
				this.dynamoMapper.delete(saved);
			}
		}
	}
}
