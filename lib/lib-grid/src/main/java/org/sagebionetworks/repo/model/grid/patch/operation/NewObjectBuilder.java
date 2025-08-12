package org.sagebionetworks.repo.model.grid.patch.operation;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class NewObjectBuilder implements OperationBuilder<NewObject> {

	@Override
	public NewObject build(LogicalTimestamp operationId) {
		return new NewObject().setOperationId(operationId);
	}

}
