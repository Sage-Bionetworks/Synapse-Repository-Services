package org.sagebionetworks.dynamo.dao.nodetree;

import org.sagebionetworks.dynamo.DynamoWriteOperation;

/**
 * Sorts write operations to make sure a list write operation are always
 * carried out in deterministic order.
 */
abstract class LineagePairWriteOperation implements DynamoWriteOperation {

	/**
	 * The depth of the ancestor node.
	 */
	abstract int getAncestorDepth();

	/**
	 * The distance from the ancestor node to the descendant node.
	 */
	abstract int getDistance();

	/**
	 * Ancestor node ID.
	 */
	abstract String getAncestorId();

	/**
	 * Descendant node ID.
	 */
	abstract String getDescendantId();

	@Override
	public int compareTo(DynamoWriteOperation lineagePairWriteOp) {

		if (lineagePairWriteOp == null) {
			throw new NullPointerException();
		}
		if (!(lineagePairWriteOp instanceof LineagePairWriteOperation)) {
			throw new IllegalArgumentException(lineagePairWriteOp.getClass().getName() +
					" cannot be compared against " + this.getClass().getName());
		}

		// Sort the write operations so that they are executed in a predetermined order down the tree
		LineagePairWriteOperation that = (LineagePairWriteOperation)lineagePairWriteOp;
		int sort = this.getAncestorDepth() - that.getAncestorDepth();
		if (sort == 0) {
			sort = this.getAncestorId().compareTo(that.getAncestorId());
		}
		if (sort == 0) {
			sort = this.getDistance() - that.getDistance();
		}
		if (sort == 0) {
			sort = this.getDescendantId().compareTo(that.getDescendantId());
		}
		return sort;
	}
}
