package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;

import java.util.Objects;

public final class NewConstantBuilder extends OperationBuilder {
    private ConValue value;

    public NewConstantBuilder setValue(ConValue value) {
        this.value = value;
        return this;
    }

    public ConValue getValue() {
        return value;
    }

    @Override
    public NewConstant build(LogicalTimestamp operationId) {
        return new NewConstant(operationId, value);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NewConstantBuilder that = (NewConstantBuilder) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

	@Override
	public String toString() {
		return "NewConstantBuilder [value=" + value + "]";
	}
    
}
