package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewVector;

public final class NewVectorBuilder extends OperationBuilder {
    
	@Override
    public NewVector build(LogicalTimestamp operationId) {
        return new NewVector(operationId);
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
