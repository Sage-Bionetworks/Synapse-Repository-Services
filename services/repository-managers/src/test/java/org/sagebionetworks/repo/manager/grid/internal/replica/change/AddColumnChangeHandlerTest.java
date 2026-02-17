package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;

@ExtendWith(MockitoExtension.class)
public class AddColumnChangeHandlerTest {

	@Mock
	private PatchBuilder mockPatchBuilder;

	private AddColumnChangeHandler handler;

	private AddColumnChange change;

	@BeforeEach
	public void before() {
		handler = new AddColumnChangeHandler();

		LogicalTimestamp columnOrderArrId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		LogicalTimestamp nodeRefId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(2L);
		Long vectorIndex = 5L;

		change = new AddColumnChange(columnOrderArrId, nodeRefId, vectorIndex);
	}

	@Test
	public void testHandleChange() {
		Long repId = 5L;

		LogicalTimestamp newConstantId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(10L);

		when(mockPatchBuilder.addOperationBuilder(
			Operations.newConstant().setValue(change.getColumnIndex()))
		).thenReturn(newConstantId);

		when(mockPatchBuilder.addOperationBuilder(
			Operations.insertArray()
				.setArrayId(change.getColumnOrderArrId())
				.setReferenceId(change.getInsertAfterId())
				.setElementIds(List.of(newConstantId))
		)).thenReturn(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(20L));

		// call under test
		handler.handleChange(mockPatchBuilder, change);

		verifyNoMoreInteractions(mockPatchBuilder);
	}
}
