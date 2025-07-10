package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class InsertArray implements Operation<InsertArray> {

	private LogicalTimestamp operationId;
	private LogicalTimestamp arrayId;
	private LogicalTimestamp referenceId;
	private List<LogicalTimestamp> elementIds;

	@Override
	public OperationType getType() {
		return OperationType.ins_arr;
	}

	@Override
	public LogicalTimestamp getOperationId() {
		return operationId;
	}

	@Override
	public long getSpan() {
		return elementIds.size();
	}

	public LogicalTimestamp getArrayId() {
		return arrayId;
	}

	public InsertArray setArrayId(LogicalTimestamp arrayId) {
		this.arrayId = arrayId;
		return this;
	}

	public LogicalTimestamp getReferenceId() {
		return referenceId;
	}

	public InsertArray setReferenceId(LogicalTimestamp referenceId) {
		this.referenceId = referenceId;
		return this;
	}

	public List<LogicalTimestamp> getElementIds() {
		return elementIds;
	}

	public InsertArray setElementIds(List<LogicalTimestamp> elementIds) {
		this.elementIds = elementIds;
		return this;
	}

	public InsertArray setOperationId(LogicalTimestamp operationId) {
		this.operationId = operationId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(arrayId, elementIds, operationId, referenceId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InsertArray other = (InsertArray) obj;
		return Objects.equals(arrayId, other.arrayId) && Objects.equals(elementIds, other.elementIds)
				&& Objects.equals(operationId, other.operationId) && Objects.equals(referenceId, other.referenceId);
	}

	@Override
	public String toString() {
		return "InsertArray [operationId=" + operationId + ", arrayId=" + arrayId + ", referenceId=" + referenceId
				+ ", elementIds=" + elementIds + "]";
	}

}
