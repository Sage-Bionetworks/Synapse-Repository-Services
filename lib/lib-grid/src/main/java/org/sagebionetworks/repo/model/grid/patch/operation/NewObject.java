package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

public class NewObject implements Operation<NewObject> {

	private final LogicalTimestamp operationId;

	public  NewObject(LogicalTimestamp operationId) {
		ValidateArgument.required(operationId, "operationId");
		this.operationId = operationId;
	}

	@Override
	public OperationType getType() {
		return OperationType.new_obj;
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
		NewObject other = (NewObject) obj;
		return Objects.equals(operationId, other.operationId);
	}

	@Override
	public String toString() {
		return "NewObject [id=" + operationId + "]";
	}

}
