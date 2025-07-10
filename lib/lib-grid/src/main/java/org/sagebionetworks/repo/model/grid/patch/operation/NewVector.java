package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class NewVector implements Operation<NewVector> {

	private LogicalTimestamp operationId;

	@Override
	public OperationType getType() {
		return OperationType.new_vec;
	}

	@Override
	public LogicalTimestamp getOperationId() {
		return operationId;
	}

	@Override
	public long getSpan() {
		return 1L;
	}

	public NewVector setOperationId(LogicalTimestamp operationId) {
		this.operationId = operationId;
		return this;
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
		NewVector other = (NewVector) obj;
		return Objects.equals(operationId, other.operationId);
	}

	@Override
	public String toString() {
		return "NewVector [id=" + operationId + "]";
	}

}
