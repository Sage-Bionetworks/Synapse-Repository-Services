package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;

@ExtendWith(MockitoExtension.class)
public class UpdateColumnNamesChangeHandlerTest {

	@Mock
	private PatchBuilder mockPatchBuilder;

	private UpdateColumnNamesChangeHandler handler;

	private UpdateColumnNamesChange change;

	@BeforeEach
	public void before() {
		handler = new UpdateColumnNamesChangeHandler();

		LogicalTimestamp vecId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		Map<Integer, String> indexToNameMap = new LinkedHashMap<>();
		indexToNameMap.put(0, "column_a");
		indexToNameMap.put(1, "column_b");
		indexToNameMap.put(2, "column_c");

		change = new UpdateColumnNamesChange(vecId, indexToNameMap);
	}

	@Test
	public void testHandleChange() {
		Long repId = 5L;

		Map<Integer, LogicalTimestamp> constMap = new LinkedHashMap<>();

		for (Map.Entry<Integer, ConValue> entry : change.getIndexToNameMap().entrySet()) {
			LogicalTimestamp constId = new LogicalTimestamp().setReplicaId(repId)
					.setSequenceNumber(10L + entry.getKey());

			when(mockPatchBuilder.addOperationBuilder(Operations.newConstant().setValue(entry.getValue())))
					.thenReturn(constId);

			constMap.put(entry.getKey(), constId);
		}

		when(mockPatchBuilder.addOperationBuilder(
				Operations.insertVector().setVectorId(change.getColunNamesVecId()).setMap(constMap)))
				.thenReturn(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(20L));

		// call under test
		handler.handleChange(mockPatchBuilder, change);

		verifyNoMoreInteractions(mockPatchBuilder);
	}

	@Test
	public void testHandleChangeWithEmptyMap() {
		Long repId = 5L;

		LogicalTimestamp vecId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		UpdateColumnNamesChange emptyChange = new UpdateColumnNamesChange(vecId, Map.of());

		when(mockPatchBuilder.addOperationBuilder(
				Operations.insertVector().setVectorId(emptyChange.getColunNamesVecId()).setMap(Map.of())))
				.thenReturn(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(20L));

		// call under test
		handler.handleChange(mockPatchBuilder, emptyChange);

		verifyNoMoreInteractions(mockPatchBuilder);
	}
}
