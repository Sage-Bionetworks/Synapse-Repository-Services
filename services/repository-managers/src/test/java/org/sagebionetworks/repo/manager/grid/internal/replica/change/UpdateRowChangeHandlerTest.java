package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.DocumentConstants;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;

@ExtendWith(MockitoExtension.class)
public class UpdateRowChangeHandlerTest {

	@Mock
	private PatchBuilder mockPatchBuilder;

	private UpdateRowChangeHandler handler;

	private UpdateRowChange change;
	private UpdateRowChange changeWithSynaspeRow;

	private LogicalTimestamp metadataObjectId;
	private ConValue synapseRow;

	@BeforeEach
	public void before() {
		handler = new UpdateRowChangeHandler();

		LogicalTimestamp vecId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		List<ConValue> rowData = List.of(new ConValue(ConType.STRING, "a"), new ConValue(ConType.STRING, "b"),
				new ConValue(ConType.STRING, "c"));
		Integer[] rowVectorIndex = new Integer[] { 2, 0, 1 };

		change = new UpdateRowChange(vecId, rowData, rowVectorIndex);

		metadataObjectId = new LogicalTimestamp().setReplicaId(101L).setSequenceNumber(102L);
		synapseRow = new SynapseRow().setRowId(111L).setVersionNumber(2L).setEtag("e1").toConValue();
		changeWithSynaspeRow = new UpdateRowChange(vecId, rowData, rowVectorIndex, metadataObjectId, synapseRow);
	}

	@Test
	public void testHandleChange() {
		Long repId = 5L;

		Map<Integer, LogicalTimestamp> constMap = new HashMap<>();

		for (int i = 0; i < change.getRowData().size(); i++) {
			ConValue value = change.getRowData().get(i);

			LogicalTimestamp constId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(20L + i);

			when(mockPatchBuilder.addOperationBuilder(Operations.newConstant().setValue(value))).thenReturn(constId);

			constMap.put(change.getRowVectorIndex()[i], constId);
		}

		when(mockPatchBuilder
				.addOperationBuilder(Operations.insertVector().setVectorId(change.getRowVectorId()).setMap(constMap)))
				.thenReturn(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(30L));

		// call under test
		handler.handleChange(mockPatchBuilder, change);

		verifyNoMoreInteractions(mockPatchBuilder);

	}

	@Test
	public void testHandleChangeWithSynaspeRow() {
		Long repId = 5L;

		Map<Integer, LogicalTimestamp> constMap = new HashMap<>();

		for (int i = 0; i < changeWithSynaspeRow.getRowData().size(); i++) {
			ConValue value = changeWithSynaspeRow.getRowData().get(i);

			LogicalTimestamp constId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(20L + i);

			when(mockPatchBuilder.addOperationBuilder(Operations.newConstant().setValue(value))).thenReturn(constId);

			constMap.put(changeWithSynaspeRow.getRowVectorIndex()[i], constId);
		}

		when(mockPatchBuilder.addOperationBuilder(
				Operations.insertVector().setVectorId(changeWithSynaspeRow.getRowVectorId()).setMap(constMap)))
				.thenReturn(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(30L));

		LogicalTimestamp synConId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(404L);
		when(mockPatchBuilder.addOperationBuilder(Operations.newConstant().setValue(synapseRow))).thenReturn(synConId);
		when(mockPatchBuilder.addOperationBuilder(Operations.insertObject().setObjectId(metadataObjectId)
				.setMap(Map.of(DocumentConstants.SYNAPSE_ROW, synConId))))
				.thenReturn(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(405L));

		// call under test
		handler.handleChange(mockPatchBuilder, changeWithSynaspeRow);

		verifyNoMoreInteractions(mockPatchBuilder);

	}

}
