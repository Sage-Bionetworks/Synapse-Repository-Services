package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.InsertObjectBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.NewConstantBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.NewObjectBuilder;

@ExtendWith(MockitoExtension.class)
public class UpdateMetadataChangeHandlerTest {

	@Mock
	private PatchBuilder mockPatchBuilder;

	private JSONObject validationState;
	private UpdateMetadataChange change;

	@BeforeEach
	public void before() {
		validationState = new JSONObject("{\"isValid\":true}");
		change = new UpdateMetadataChange()
				.setRowObjectId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L))
				.setRowMetadataId(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L))
				.setValidationState(validationState);
	}

	@Test
	public void testHandleChange() {

		LogicalTimestamp stateId = new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L);
		when(mockPatchBuilder.addOperationBuilder(
				new NewConstantBuilder().setValue(new ConValue(ConType.JSON_OBJECT, change.getValidationState()))))
				.thenReturn(stateId);
		when(mockPatchBuilder.addOperationBuilder(new InsertObjectBuilder().setObjectId(change.getRowMetadataId())
				.setMap(Map.of("rowValidation", stateId))))
				.thenReturn(new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L));

		UpdateMetadataChangeHandler handler = new UpdateMetadataChangeHandler();
		// call under test
		handler.handleChange(mockPatchBuilder, change);

	}

	@Test
	public void testHandleChangeWithNullMetadataId() {
		change.setRowMetadataId(null);
		LogicalTimestamp metadataId = new LogicalTimestamp().setReplicaId(11L).setSequenceNumber(12L);
		when(mockPatchBuilder.addOperationBuilder(eq(new NewObjectBuilder()))).thenReturn(metadataId);
		when(mockPatchBuilder.addOperationBuilder(
				new InsertObjectBuilder().setObjectId(change.getRowObjectId()).setMap(Map.of("metadata", metadataId))))
				.thenReturn(new LogicalTimestamp().setReplicaId(13L).setSequenceNumber(14L));

		LogicalTimestamp stateId = new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L);
		when(mockPatchBuilder.addOperationBuilder(
				new NewConstantBuilder().setValue(new ConValue(ConType.JSON_OBJECT, change.getValidationState()))))
				.thenReturn(stateId);
		when(mockPatchBuilder.addOperationBuilder(
				new InsertObjectBuilder().setObjectId(metadataId).setMap(Map.of("rowValidation", stateId))))
				.thenReturn(new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L));

		UpdateMetadataChangeHandler handler = new UpdateMetadataChangeHandler();
		// call under test
		handler.handleChange(mockPatchBuilder, change);

	}

}
