package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.sagebionetworks.dynamo.DynamoWriteExecution;
import org.sagebionetworks.dynamo.DynamoWriteExecutor;
import org.sagebionetworks.dynamo.DynamoWriteOperation;

/**
 * Executes a list of write operations on the {@link DboNodeLineage} table.
 */
class NodeLineageWriteExecutor implements DynamoWriteExecutor {

	private final Logger logger = LogManager.getLogger(NodeLineageWriteExecutor.class);

	@Override
	public boolean execute(DynamoWriteExecution execution) {

		if (execution == null) {
			throw new NullPointerException();
		}

		List<DynamoWriteOperation> opList = execution.getWriteOpList();
		// Typical rollback is implemented with a stack so that changes are rolled back
		// in the reverse order of they are executed. However, to avoid incomplete restores,
		// we are restoring the changes in the same order.
		List<DynamoWriteOperation> restoreList = new ArrayList<DynamoWriteOperation>(opList.size());
		for (int i = 0; i < opList.size(); i++) {
			DynamoWriteOperation op = opList.get(i);
			boolean executed = op.write(i);
			if (!executed) {
				restoreList.add(op);
				// When one op failed, try restoring the list from the beginning
				for (int j = 0; j < restoreList.size(); j++) {
					DynamoWriteOperation restore = restoreList.get(j);
					try {
						restore.restore(j);
					} catch (Throwable t) {
						logger.error("Restore failed at restore step " + j + " with exception. ", t);
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
