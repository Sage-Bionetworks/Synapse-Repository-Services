package org.sagebionetworks.dynamo.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.sagebionetworks.dynamo.DynamoWriteExecutor;
import org.sagebionetworks.dynamo.DynamoWriteOperation;

/**
 * Tries to maximize optimistic locking on a chain of write operations by running them
 * in particular order. The list of writes always starts from the top and progresses
 * toward bottom. Right after a node is written, it has the effect of "locking"
 * down the entire subtree rooted at the node.
 * <p>
 * Now imagine two threads are both updating from the top. Once the two lists of updates
 * diverge, there is 0 probability of triggering optimistic locking. If one does "lock" out
 * the other, the "locking-out" should happen near the top, where rollback is minimal.
 * <p>
 * Besides optimizing writes to maximize the use of optimistic locking. We could use DynamoDB
 * as a central place to keep track of locks for writes. One simple approach is to allow
 * at most one writing thread per DynamoDB table. A DynamoDB table could be set up to register
 * such threads for tables. This guarantees only one thread is updating the table. A much
 * more fine-grained approach is use read-write locks on records (whether to apply locks to
 * read-only operations is optional). This allows more than one thread to update the table
 * simultaneously. However, the table would require certain amount of normalization and care
 * must be given to avoid deadlocks and race conditions.
 *
 * @author Eric Wu
 */
class NodeLineageWriteExecutor implements DynamoWriteExecutor {

	private final Logger logger = Logger.getLogger(NodeLineageWriteExecutor.class);

	@Override
	public boolean execute(DynamoWriteOperation op) {

		if (op == null) {
			throw new NullPointerException();
		}

		List<DynamoWriteOperation> opList = new ArrayList<DynamoWriteOperation>(1);
		opList.add(op);
		return this.execute(opList);
	}

	@Override
	public boolean execute(List<DynamoWriteOperation> opList) {

		if (opList == null) {
			throw new NullPointerException();
		}

		// Put in the correct execution order so that operations on the root are executed first.
		Collections.sort(opList);

		// Typical rollback is implemented with a stack so that changes are rolled back in order
		// However, as we are using the root to do optimistic locking, we want to take the advantage
		// of it by restoring from the root.
		List<DynamoWriteOperation> restoreList =
				new ArrayList<DynamoWriteOperation>(opList.size());
		for (DynamoWriteOperation op : opList) {
			boolean executed = op.write();
			if (!executed) {
				restoreList.add(op);
				// When one op failed, try restoring from the beginning
				for (DynamoWriteOperation restore : restoreList) {
					try {
						restore.restore();
					} catch (Throwable t) {
						logger.error("Rollback failed with exception. ", t);
						return false;
					}
				}
				return false;
			}
			restoreList.add(op);
		}

		return true;
	}
}
