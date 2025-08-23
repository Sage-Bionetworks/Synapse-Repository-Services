package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

/**
 * The new_con operation. See: <a href=
 * "https://jsonjoy.com/specs/json-crdt-patch/patch-document/operation-types">operation-types</a>
 */
public class NewConstant implements Operation {

	private final LogicalTimestamp operationId;
	private final ConValue value;
	private final boolean isTimestamp;

	public NewConstant(LogicalTimestamp operationId, ConValue value) {
		ValidateArgument.required(operationId, "operationId");
		this.operationId = operationId;
		this.value = value;
		this.isTimestamp = value != null && ConType.TIMESTAMP == value.getType();
	}

	@Override
	public OperationType getType() {
		return OperationType.new_con;
	}

	@Override
	public long getSpan() {
		return 1L;
	}

	public boolean isTimestamp() {
		return isTimestamp;
	}

	public LogicalTimestamp getOperationId() {
		return operationId;
	}

	public ConValue getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(operationId, isTimestamp, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NewConstant other = (NewConstant) obj;
		return Objects.equals(operationId, other.operationId) && isTimestamp == other.isTimestamp
				&& Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "NewConstant [isTimestamp=" + isTimestamp + ", id=" + operationId + ", value=" + value + "]";
	}

}
