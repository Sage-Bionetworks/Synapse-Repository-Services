package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Timespan;
import org.sagebionetworks.util.ValidateArgument;

public class Delete implements Operation {

	private final LogicalTimestamp operationId;
	private final LogicalTimestamp nodeId;
	private final List<Timespan> timespans;
	
	public Delete(LogicalTimestamp operationId, LogicalTimestamp nodeId, List<Timespan> timespans) {
		ValidateArgument.required(operationId, "operationId");
		ValidateArgument.required(nodeId, "nodeId");
		ValidateArgument.required(timespans, "timespans");
		
		this.operationId = operationId;
		this.nodeId = nodeId;
		this.timespans = timespans;
	}
	
	@Override
	public LogicalTimestamp getOperationId() {
		return operationId;
	}
	
	public LogicalTimestamp getNodeId() {
		return nodeId;
	}
	
	public List<Timespan> getTimespans() {
		return timespans;
	}
	
	@Override
	public OperationType getType() {
		return OperationType.del;
	}
	
	@Override
	public long getSpan() {
		return 1;
	}

	@Override
	public int hashCode() {
		return Objects.hash(operationId, timespans, nodeId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Delete)) {
			return false;
		}
		Delete other = (Delete) obj;
		return Objects.equals(operationId, other.operationId) && Objects.equals(timespans, other.timespans) && Objects.equals(nodeId, other.nodeId);
	}

	@Override
	public String toString() {
		return String.format("Delete [operationId=%s, nodeId=%s, timespans=%s]", operationId, nodeId, timespans);
	}
	
}
