package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class NewBinary implements Operation<NewBinary> {

	private LogicalTimestamp operationId;

	@Override
	public OperationType getType() {
		return OperationType.new_bin;
	}

	@Override
	public LogicalTimestamp getOperationId() {
		return operationId;
	}

	@Override
	public long getSpan() {
		return 1L;
	}

	public NewBinary setOperationId(LogicalTimestamp id) {
		this.operationId = id;
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
		NewBinary other = (NewBinary) obj;
		return Objects.equals(operationId, other.operationId);
	}

	@Override
	public String toString() {
		return "NewBinary [id=" + operationId + "]";
	}


}
