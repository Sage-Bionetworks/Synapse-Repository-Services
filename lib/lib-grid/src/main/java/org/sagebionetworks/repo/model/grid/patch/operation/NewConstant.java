package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * The new_con operation. See: <a href=
 * "https://jsonjoy.com/specs/json-crdt-patch/patch-document/operation-types">operation-types</a>
 */
public class NewConstant implements Operation<NewConstant> {

	private LogicalTimestamp operationId;
	private ConValue value;
	private boolean isTimestamp;

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

	public NewConstant setTimestamp(boolean isTimestamp) {
		this.isTimestamp = isTimestamp;
		return this;
	}

	public LogicalTimestamp getOperationId() {
		return operationId;
	}

	public NewConstant setOperationId(LogicalTimestamp operationId) {
		this.operationId = operationId;
		return this;
	}

	public ConValue getValue() {
		return value;
	}

	public NewConstant setValue(ConValue value) {
		this.value = value;
		return this;
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
