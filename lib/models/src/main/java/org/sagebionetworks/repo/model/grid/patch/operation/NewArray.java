package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class NewArray implements Operation<NewArray> {

	private LogicalTimestamp operationId;

	@Override
	public OperationType getType() {
		return OperationType.new_arr;
	}

	@Override
	public LogicalTimestamp getOperationId() {
		return operationId;
	}

	public NewArray setOperationId(LogicalTimestamp operationId) {
		this.operationId = operationId;
		return this;
	}

	@Override
	public long getSpan() {
		return 1L;
	}

	@Override
	public int hashCode() {
		return Objects.hash(operationId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NewArray other = (NewArray) obj;
		return Objects.equals(operationId, other.operationId);
	}

	@Override
	public String toString() {
		return "NewArray [operationId=" + operationId + "]";
	}

}
