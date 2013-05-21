package org.sagebionetworks.dynamo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A write execution wraps around a list of write operations.
 */
public class DynamoWriteExecution {
	
	public DynamoWriteExecution(String id, DynamoWriteOperation writeOp) {

		if (id == null) {
			throw new NullPointerException();
		}
		if (writeOp == null) {
			throw new NullPointerException();
		}

		this.id = id;
		List<DynamoWriteOperation> opList = new ArrayList<DynamoWriteOperation>(1);
		opList.add(writeOp);
		this.opList = Collections.unmodifiableList(opList);
	}

	public DynamoWriteExecution(String id, List<DynamoWriteOperation> writeOpList) {

		if (id == null) {
			throw new NullPointerException();
		}
		if (writeOpList == null) {
			throw new NullPointerException();
		}

		this.id = id;
		List<DynamoWriteOperation> opList = new ArrayList<DynamoWriteOperation>(writeOpList.size());
		opList.addAll(writeOpList);
		// The ops are to be executed in predetermined order.
		Collections.sort(opList);
		this.opList = Collections.unmodifiableList(opList);
	}

	public String getId() {
		return this.id;
	}

	public List<DynamoWriteOperation> getWriteOpList() {
		return this.opList;
	}

	private final String id;
	private final List<DynamoWriteOperation> opList;
}
