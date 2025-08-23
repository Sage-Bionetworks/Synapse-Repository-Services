package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

public class NewValue implements Operation {

	private final LogicalTimestamp operationId;

	public NewValue(LogicalTimestamp operationId) {
		ValidateArgument.required(operationId, "operationId");
		this.operationId = operationId;
	}

	@Override
	public OperationType getType() {
		return OperationType.new_val;
	}

	@Override
	public LogicalTimestamp getOperationId() {
		return operationId;
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
		NewValue other = (NewValue) obj;
		return Objects.equals(operationId, other.operationId);
	}

	@Override
	public String toString() {
		return "NewValue [id=" + operationId + "]";
	}
	
}
