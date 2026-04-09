package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.Collections;

import org.sagebionetworks.repo.model.grid.patch.Timespan;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;
import org.springframework.stereotype.Component;

@Component
public class DeleteArrayNodeChangeHandler implements ChangeHandler<DeleteArrayNodeChange> {

	@Override
	public IntendedChangeType getType() {
		return IntendedChangeType.delete_array_node;
	}

	@Override
	public void handleChange(PatchBuilder builder, DeleteArrayNodeChange change) {
		builder.addOperationBuilder(Operations.delete().setNodeId(change.getArrId())
				.setTimespans(Collections.singletonList(new Timespan(change.getRgaNodeId(), 1L))));
	}

}
