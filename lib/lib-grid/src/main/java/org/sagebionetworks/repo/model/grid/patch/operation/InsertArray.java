package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

public class InsertArray implements Operation {

	private final LogicalTimestamp operationId;
	private final LogicalTimestamp arrayId;
	private final LogicalTimestamp referenceId;
	private final List<LogicalTimestamp> elementIds;

	public InsertArray(LogicalTimestamp operationId, LogicalTimestamp arrayId, LogicalTimestamp referenceId,
			List<LogicalTimestamp> elementIds) {
		ValidateArgument.required(operationId, "operationId");
		ValidateArgument.required(arrayId, "arrayId");
		ValidateArgument.required(referenceId, "referenceId");
		ValidateArgument.required(elementIds, "elementIds");


		this.operationId = operationId;
		this.arrayId = arrayId;
		this.referenceId = referenceId;
		this.elementIds = elementIds;
	}

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

	public LogicalTimestamp getReferenceId() {
		return referenceId;
	}

	public List<LogicalTimestamp> getElementIds() {
		return elementIds;
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
