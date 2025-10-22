package org.sagebionetworks.repo.model.grid.patch.operation.builder;


import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewValue;

public final class NewValueBuilder extends OperationBuilder {

    @Override
    public NewValue build(LogicalTimestamp operationId) {
        return new NewValue(operationId);
    }
    
    @Override
	public boolean equals(Object obj) {
		return obj != null && this.getClass() == obj.getClass();
	}

	@Override
	public int hashCode() {
		return this.getClass().hashCode();
	}
}
