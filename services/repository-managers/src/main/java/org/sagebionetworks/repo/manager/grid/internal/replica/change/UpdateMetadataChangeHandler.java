package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.Map;

import org.sagebionetworks.repo.manager.grid.DocumentConstants;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.InsertObjectBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.NewConstantBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.NewObjectBuilder;
import org.springframework.stereotype.Component;

@Component
public class UpdateMetadataChangeHandler implements ChangeHandler<UpdateMetadataChange> {

	@Override
	public IntendedChangeType getType() {
		return IntendedChangeType.update_row_metadata;
	}

	@Override
	public void handleChange(PatchBuilder builder, UpdateMetadataChange change) {
		LogicalTimestamp metadataObjectId = change.getRowMetadataId();
		if (metadataObjectId == null) {
			metadataObjectId = builder.addOperationBuilder(new NewObjectBuilder());
			builder.addOperationBuilder(new InsertObjectBuilder().setObjectId(change.getRowObjectId())
					.setMap(Map.of(DocumentConstants.METADATA, metadataObjectId)));
		}
		LogicalTimestamp stateId = builder.addOperationBuilder(
				new NewConstantBuilder().setValue(new ConValue(ConType.JSON_OBJECT, change.getValidationState())));
		builder.addOperationBuilder(new InsertObjectBuilder().setObjectId(metadataObjectId)
				.setMap(Map.of(DocumentConstants.ROW_VALIDATION, stateId)));
	}

}
