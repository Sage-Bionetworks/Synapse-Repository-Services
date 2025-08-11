package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

public class InsertValue implements Operation<InsertValue> {

	private final LogicalTimestamp operationId;
	private final LogicalTimestamp valueId;
	private final LogicalTimestamp referenceId;

	public InsertValue(LogicalTimestamp operationId,  LogicalTimestamp valueId, LogicalTimestamp referenceId) {
		ValidateArgument.required(operationId, "operationId");
		ValidateArgument.required(valueId, "valueId");
		ValidateArgument.required(referenceId, "referenceId");

		this.operationId = operationId;
		this.valueId = valueId;
		this.referenceId = referenceId;
	}

	@Override
	public OperationType getType() {
		return OperationType.ins_val;
	}

	@Override
	public LogicalTimestamp getOperationId() {
		return operationId;
	}

	public LogicalTimestamp getReferenceId() {
		return referenceId;
	}


	public LogicalTimestamp getValueId() {
		return valueId;
	}

	@Override
	public long getSpan() {
		return 1L;
	}

	@Override
	public int hashCode() {
		return Objects.hash(operationId, referenceId, valueId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InsertValue other = (InsertValue) obj;
		return Objects.equals(operationId, other.operationId) && Objects.equals(referenceId, other.referenceId)
				&& Objects.equals(valueId, other.valueId);
	}

	@Override
	public String toString() {
		return "InsertValue [id=" + operationId + ", referenceId=" + referenceId + ", valueId=" + valueId + "]";
	}

}
