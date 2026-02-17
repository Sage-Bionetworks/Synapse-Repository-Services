package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.Collections;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;
import org.springframework.stereotype.Component;

@Component
public class AddColumnChangeHandler implements ChangeHandler<AddColumnChange> {

	@Override
	public IntendedChangeType getType() {
		return IntendedChangeType.add_column;
	}

	@Override
	public void handleChange(PatchBuilder builder, AddColumnChange change) {
		LogicalTimestamp indexConId = builder
				.addOperationBuilder(Operations.newConstant().setValue(change.getColumnIndex()));
		builder.addOperationBuilder(Operations.insertArray().setArrayId(change.getColumnOrderArrId())
				.setReferenceId(change.getInsertAfterId()).setElementIds(Collections.singletonList(indexConId)));

	}

}
