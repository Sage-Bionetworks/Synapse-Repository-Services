package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class NewConstantBuilder implements OperationBuilder<NewConstant> {

	private ConValue value;
	private boolean isTimestamp;

	@Override
	public NewConstant build(LogicalTimestamp operationId) {
		return new NewConstant().setOperationId(operationId).setTimestamp(isTimestamp).setValue(value);
	}

	public ConValue getValue() {
		return value;
	}

	public NewConstantBuilder setValue(ConValue value) {
		this.value = value;
		return this;
	}

	public boolean isTimestamp() {
		return isTimestamp;
	}

	public NewConstantBuilder setTimestamp(boolean isTimestamp) {
		this.isTimestamp = isTimestamp;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(isTimestamp, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NewConstantBuilder other = (NewConstantBuilder) obj;
		return isTimestamp == other.isTimestamp && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "NewConstantBuilder [value=" + value + ", isTimestamp=" + isTimestamp + "]";
	}

}
