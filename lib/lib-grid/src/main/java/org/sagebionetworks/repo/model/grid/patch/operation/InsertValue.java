package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class InsertValue implements Operation<InsertValue> {

	private LogicalTimestamp operationId;
	private LogicalTimestamp referenceId;
	private LogicalTimestamp valueId;

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

	public InsertValue setReferenceId(LogicalTimestamp referenceId) {
		this.referenceId = referenceId;
		return this;
	}

	public LogicalTimestamp getValueId() {
		return valueId;
	}

	public InsertValue setValueId(LogicalTimestamp valueId) {
		this.valueId = valueId;
		return this;
	}

	public InsertValue setOperationId(LogicalTimestamp operationId) {
		this.operationId = operationId;
		return this;
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
