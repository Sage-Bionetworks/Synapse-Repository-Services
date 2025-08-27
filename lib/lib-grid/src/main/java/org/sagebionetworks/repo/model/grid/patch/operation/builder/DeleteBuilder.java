package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Timespan;
import org.sagebionetworks.repo.model.grid.patch.operation.Delete;
import org.sagebionetworks.repo.model.grid.patch.operation.Operation;

public class DeleteBuilder extends OperationBuilder {
	
	private LogicalTimestamp nodeId;
	private List<Timespan> timeSpans;
	
	public DeleteBuilder setNodeId(LogicalTimestamp nodeId) {
		this.nodeId = nodeId;
		return this;
	}
	
	public DeleteBuilder setTimespans(List<Timespan> timeSpans) {
		this.timeSpans = timeSpans;
		return this;
	}
	
	@Override
	public Operation build(LogicalTimestamp operationId) {
		return new Delete(operationId, nodeId, timeSpans);
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodeId, timeSpans);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DeleteBuilder)) {
			return false;
		}
		DeleteBuilder other = (DeleteBuilder) obj;
		return Objects.equals(nodeId, other.nodeId) && Objects.equals(timeSpans, other.timeSpans);
	}
	
}
