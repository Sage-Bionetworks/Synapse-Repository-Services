package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.Map;

import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObjectBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstantBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.NewObjectBuilder;
import org.springframework.stereotype.Component;

@Component
public class UpdateMetadataChangeHandler implements ChangeHandler<UpdateMetadataChange> {

	@Override
	public IntendedChangeType getType() {
		return IntendedChangeType.update_row_metadata;
	}

	@Override
	public void handleChange(PatchBuilder builder, UpdateMetadataChange change) {
		LogicalTimestamp metadataObjectId = change.getRowMetadataId() != null ? change.getRowMetadataId()
				: builder.addOperationBuilder(new NewObjectBuilder());
		LogicalTimestamp stateId = builder.addOperationBuilder(
				new NewConstantBuilder().setValue(new ConValue(ConType.JSON_OBJECT, change.getValidationState())));
		builder.addOperationBuilder(
				new InsertObjectBuilder().setObjectId(metadataObjectId).setMap(Map.of("rowValidation", stateId)));
	}

}
