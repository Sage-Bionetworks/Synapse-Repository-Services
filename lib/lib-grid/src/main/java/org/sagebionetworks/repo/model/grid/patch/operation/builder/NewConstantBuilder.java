package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;

public final class NewConstantBuilder extends OperationBuilder<NewConstant> {
    private ConValue value;

    public NewConstantBuilder setValue(ConValue value) {
        this.value = value;
        return this;
    }

    @Override
    public NewConstant build(LogicalTimestamp operationId) {
        return new NewConstant(operationId, value);
    }
}
