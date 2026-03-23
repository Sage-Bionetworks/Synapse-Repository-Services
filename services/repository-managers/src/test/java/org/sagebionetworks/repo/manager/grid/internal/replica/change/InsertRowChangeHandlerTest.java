package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;

@ExtendWith(MockitoExtension.class)
public class InsertRowChangeHandlerTest {

	@Mock
	private PatchBuilder mockPatchBuilder;

	private InsertRowChangeHandler handler;

	private InsertRowChange change;
	private InsertRowChange changeWithSynapseRow;

	@BeforeEach
	public void before() {
		handler = new InsertRowChangeHandler();

		LogicalTimestamp arrId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		LogicalTimestamp nodeId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(45L);
		List<ConValue> rowData = List.of(new ConValue(ConType.STRING, "a"), new ConValue(ConType.STRING, "b"),
				new ConValue(ConType.STRING, "c"));
		Integer[] rowVectorIndex = new Integer[] { 2, 0, 1 };

		change = new InsertRowChange(arrId, nodeId, rowData, rowVectorIndex);
		changeWithSynapseRow = new InsertRowChange(arrId, nodeId, rowData, rowVectorIndex,
				new SynapseRow().setRowId(1L).setVersionNumber(2L).setEtag("e1").toConValue());
	}

	@Test
	public void testHandleChange() {
		Long repId = 5L;

		LogicalTimestamp rowObjId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(5L);

		when(mockPatchBuilder.addOperationBuilder(Operations.newObject())).thenReturn(rowObjId);

		LogicalTimestamp rowVecId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(6L);

		when(mockPatchBuilder.addOperationBuilder(Operations.newVector())).thenReturn(rowVecId);

		Map<Integer, LogicalTimestamp> constMap = new HashMap<>();

		for (int i = 0; i < change.getRowData().size(); i++) {
			ConValue value = change.getRowData().get(i);
			LogicalTimestamp constId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(7L + i);

			when(mockPatchBuilder.addOperationBuilder(Operations.newConstant().setValue(value))).thenReturn(constId);

			constMap.put(change.getRowVectorIndex()[i], constId);
		}

		when(mockPatchBuilder.addOperationBuilder(Operations.insertVector().setVectorId(rowVecId).setMap(constMap)))
				.thenReturn(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(30L));

		when(mockPatchBuilder
				.addOperationBuilder(Operations.insertObject().setObjectId(rowObjId).setMap(Map.of("data", rowVecId))))
				.thenReturn(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(50L));

		when(mockPatchBuilder.addOperationBuilder(Operations.insertArray().setArrayId(change.getRowsArrayId())
				.setReferenceId(change.getNodeRefId()).setElementIds(List.of(rowObjId))))
				.thenReturn(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(60L));

		// call under test
		handler.handleChange(mockPatchBuilder, change);

		verifyNoMoreInteractions(mockPatchBuilder);

	}

	@Test
	public void testHandleChangeWithSynapseRow() {
		Long repId = 5L;
		LogicalTimestamp rowObjId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(5L);
		LogicalTimestamp metadataObjectForRowRef = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(105L);
		when(mockPatchBuilder.addOperationBuilder(Operations.newObject())).thenReturn(rowObjId, metadataObjectForRowRef);

		LogicalTimestamp rowVecId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(6L);
		when(mockPatchBuilder.addOperationBuilder(Operations.newVector())).thenReturn(rowVecId);
		Map<String, LogicalTimestamp> rowObjectMap = new LinkedHashMap<>();
		rowObjectMap.put("data", rowVecId);

		Map<Integer, LogicalTimestamp> constMap = new HashMap<>();

		for (int i = 0; i < changeWithSynapseRow.getRowData().size(); i++) {
			ConValue value = changeWithSynapseRow.getRowData().get(i);
			LogicalTimestamp constId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(7L + i);

			when(mockPatchBuilder.addOperationBuilder(Operations.newConstant().setValue(value))).thenReturn(constId);

			constMap.put(changeWithSynapseRow.getRowVectorIndex()[i], constId);
		}
		when(mockPatchBuilder.addOperationBuilder(Operations.insertVector().setVectorId(rowVecId).setMap(constMap)))
				.thenReturn(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(30L));
		
		// Synapse row.
		LogicalTimestamp synConstant = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(106L);
		when(mockPatchBuilder
				.addOperationBuilder(Operations.newConstant().setValue(changeWithSynapseRow.getSynapseRow().get())))
				.thenReturn(synConstant);
		when(mockPatchBuilder.addOperationBuilder(
				Operations.insertObject().setObjectId(metadataObjectForRowRef).setMap(Map.of("synapseRow", synConstant))))
				.thenReturn(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(106L));
		rowObjectMap.put("metadata", metadataObjectForRowRef);
	
		// insert object
		when(mockPatchBuilder.addOperationBuilder(Operations.insertObject().setObjectId(rowObjId)
				.setMap(rowObjectMap)))
				.thenReturn(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(50L));

		when(mockPatchBuilder
				.addOperationBuilder(Operations.insertArray().setArrayId(changeWithSynapseRow.getRowsArrayId())
						.setReferenceId(changeWithSynapseRow.getNodeRefId()).setElementIds(List.of(rowObjId))))
				.thenReturn(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(60L));

		// call under test
		handler.handleChange(mockPatchBuilder, changeWithSynapseRow);

		verifyNoMoreInteractions(mockPatchBuilder);

	}

}