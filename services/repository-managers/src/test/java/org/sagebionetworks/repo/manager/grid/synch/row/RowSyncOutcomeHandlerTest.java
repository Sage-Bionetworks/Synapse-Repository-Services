package org.sagebionetworks.repo.manager.grid.synch.row;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.DeleteArrayNodeChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.InsertRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.synch.core.SynchronizationLogic;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandler;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceWriter;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReference;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class RowSyncOutcomeHandlerTest {

	@Mock
	private IntendedChangePublisher mockPublisher;
	@Mock
	private CopyHandler mockCopyHandler;
	@Mock
	private GridHeader mockGridHeader;
	@Mock
	private SourceWriter mockSourceWriter;
	@Mock
	private RowSourceItemReference mockRowHeader;

	private SynchronizationLogic logic;
	private List<Column> finalSchema;
	private String rowKey;

	private ConValue c1;
	private ConValue c2;
	private ConValue c1d;
	private ConValue c3;
	private SynapseRow synRow;
	private LogicalTimestamp rowVectorId;
	private LogicalTimestamp rgaNodeId;
	private LogicalTimestamp metadataNodeId;
	private LogicalTimestamp rowsArrayId;
	private LogicalTimestamp lastRowId;

	@BeforeEach
	public void before() {
		logic = new SynchronizationLogic();
		finalSchema = List.of(new Column().setName("a").setVectorIndex(1), new Column().setName("b").setVectorIndex(0));
		rowKey = "syn123";
		c1 = new ConValue(ConType.STRING, "one");
		c1d = new ConValue(ConType.STRING, "two");
		c2 = new ConValue(ConType.BOOLEAN, true);
		c3 = new ConValue(ConType.LONG, 45L);
		synRow = new SynapseRow().setRowId(123L).setVersionNumber(0L).setEtag("e1");

		rowVectorId = new LogicalTimestamp().setReplicaId(555L).setSequenceNumber(3L);
		rgaNodeId = new LogicalTimestamp().setReplicaId(555L).setSequenceNumber(4L);
		metadataNodeId = new LogicalTimestamp().setReplicaId(555L).setSequenceNumber(5L);
		rowsArrayId = new LogicalTimestamp().setReplicaId(555L).setSequenceNumber(6L);
		lastRowId = new LogicalTimestamp().setReplicaId(555L).setSequenceNumber(7L);
	}

	RowSyncOutcomeHandler setupHandler() {
		return setupHandler(false);
	}

	RowSyncOutcomeHandler setupHandler(boolean preserveUserAttribution) {
		when(mockCopyHandler.getHeader()).thenReturn(mockGridHeader);
		when(mockGridHeader.getRowsId()).thenReturn(rowsArrayId);
		when(mockCopyHandler.getLastRowsRgaNodeId()).thenReturn(lastRowId);
		return new RowSyncOutcomeHandler(logic, mockPublisher, mockCopyHandler, mockSourceWriter,
				finalSchema, preserveUserAttribution);
	}

	RowCopyItemImpl copyItem(List<CellCopyItem> cells) {
		return new RowCopyItemImpl().setVectorNodeId(rowVectorId).setRgaNodeId(rgaNodeId)
				.setMetadataNodeId(metadataNodeId).setCells(cells).setSynapseRow(synRow);
	}

	@Test
	public void testStreamCopyItems() {
		List<RowCopyItem> rows = List.of(new RowCopyItemImpl().setSynapseRow(new SynapseRow().setRowId(111L)),
				new RowCopyItemImpl().setSynapseRow(new SynapseRow().setRowId(222L)));
		when(mockCopyHandler.getRows()).thenReturn(rows.iterator());
		RowSyncOutcomeHandler handler = setupHandler();

		// call under test
		List<RowCopyItem> results = handler.streamCopyItems().collect(Collectors.toList());

		assertEquals(rows, results);
	}

	@Test
	public void testOnCopyAndSourceMatch() {
		RowSyncOutcomeHandler handler = setupHandler();
		RowCopyItemImpl copyItem = new RowCopyItemImpl().setCells(
				List.of(new CellCopyItem().setName("a").setValue(c1), new CellCopyItem().setName("b").setValue(c2)));

		// call under test — a matched row still survives and is forwarded to the writer.
		handler.onCopyAndSourceMatch(copyItem, mockRowHeader);

		verify(mockSourceWriter).recordFinalRowState(Map.of("a", c1, "b", c2));
	}

	@Test
	public void testOnDeletedFromSource() {
		RowSyncOutcomeHandler handler = setupHandler();
		RowCopyItemImpl item = new RowCopyItemImpl().setRgaNodeId(rgaNodeId);

		// call under test — a row deleted from the source is removed from the grid.
		handler.onDeletedFromSource(item);

		verify(mockPublisher).publish(new DeleteArrayNodeChange(rowsArrayId, rgaNodeId));
	}

	@Test
	public void testOnNewSourceItem() {
		RowSyncOutcomeHandler handler = setupHandler();
		TreeMap<String, ConValue> data = new TreeMap<>(Map.of("a", c1, "b", c2));
		RowSourceItem sourceItem = new RowSourceItem(data, rowKey, synRow);
		when(mockRowHeader.fetchRow()).thenReturn(sourceItem);

		// call under test — a source-only row is inserted into the grid and reported.
		handler.onNewSourceItem(mockRowHeader);

		verify(mockPublisher).publish(new InsertRowChange(rowsArrayId, lastRowId, List.of(c1, c2),
				new Integer[] { 1, 0 }, synRow.toConValue()));
		verify(mockSourceWriter).recordFinalRowState(data);
	}

	@Test
	public void testOnNewCopyItemWhenSourceAcceptsRows() {
		RowSyncOutcomeHandler handler = setupHandler();
		RowCopyItemImpl item = copyItem(List.of(new CellCopyItem().setName("a").setValue(c1),
				new CellCopyItem().setName("b").setValue(c2)));
		when(mockSourceWriter.canAddRemoveRows()).thenReturn(true);

		// call under test — an added item is pushed to the source
		handler.onNewCopyItem(item, rowKey);

		RowSourceItem expectedSynchRow = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);
		verify(mockSourceWriter).addNewRowToSource(expectedSynchRow);
		verify(mockSourceWriter).recordFinalRowState(expectedSynchRow.getData());
	}

	@Test
	public void testOnNewCopyItemWhenSourceCannotAddRows() {
		RowSyncOutcomeHandler handler = setupHandler();
		RowCopyItemImpl item = copyItem(List.of(new CellCopyItem().setName("a").setValue(c1)));
		when(mockSourceWriter.canAddRemoveRows()).thenReturn(false);

		// call under test — source cannot accept the row, so it is dropped from the grid.
		handler.onNewCopyItem(item, rowKey);

		verify(mockPublisher).publish(new DeleteArrayNodeChange(rowsArrayId, rgaNodeId));
		verify(mockSourceWriter, never()).addNewRowToSource(any());
		verify(mockSourceWriter, never()).recordFinalRowState(any());
	}

	@Test
	public void testOnSourceOnlyItemDeletedByUserFromCopyWhenSourceCanRemove() {
		RowSyncOutcomeHandler handler = setupHandler();
		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1)), rowKey, synRow);
		when(mockRowHeader.fetchRow()).thenReturn(sourceItem);
		when(mockSourceWriter.canAddRemoveRows()).thenReturn(true);

		// call under test — the user's deletion is pushed to the source; nothing pulled.
		handler.onDeletedFromCopy(mockRowHeader);

		verify(mockSourceWriter).removeRow(sourceItem);
		verify(mockPublisher, never()).publish(any());
		verify(mockSourceWriter, never()).recordFinalRowState(any());
	}

	@Test
	public void testOnSourceOnlyItemDeletedByUserFromCopyWhenSourceCannotRemove() {
		RowSyncOutcomeHandler handler = setupHandler();
		TreeMap<String, ConValue> data = new TreeMap<>(Map.of("a", c1, "b", c2));
		RowSourceItem sourceItem = new RowSourceItem(data, rowKey, synRow);
		when(mockRowHeader.fetchRow()).thenReturn(sourceItem);
		when(mockSourceWriter.canAddRemoveRows()).thenReturn(false);

		// call under test — source cannot remove, so the row is pulled back into the grid.
		handler.onDeletedFromCopy(mockRowHeader);

		verify(mockPublisher).publish(new InsertRowChange(rowsArrayId, lastRowId, List.of(c1, c2),
				new Integer[] { 1, 0 }, synRow.toConValue()));
		verify(mockSourceWriter).recordFinalRowState(data);
	}

	void verifyUpdatePublished(Map<String, ConValue> mergedCells, Set<String> excludedCells, ConValue synapseRow) {
		ArgumentCaptor<IntendedChange> captor = ArgumentCaptor.forClass(IntendedChange.class);
		verify(mockPublisher).publish(captor.capture());
		UpdateRowChange change = (UpdateRowChange) captor.getValue();
		assertEquals(rowVectorId, change.getRowVectorId());
		assertEquals(Optional.ofNullable(metadataNodeId), change.getMetadataObjectId());
		assertEquals(Optional.ofNullable(synapseRow), change.getSynapseRow());
		assertEquals(expectedIndexed(mergedCells, excludedCells), actualIndexed(change));
	}

	Map<Integer, ConValue> expectedIndexed(Map<String, ConValue> mergedCells, Set<String> excludedCells) {
		Map<String, Column> colMap = finalSchema.stream().collect(Collectors.toMap(Column::getName, c -> c));
		Map<Integer, ConValue> out = new HashMap<>();
		mergedCells.forEach((name, value) -> {
			if (!excludedCells.contains(name)) {
				Column col = colMap.get(name);
				if (col != null) {
					out.put(col.getVectorIndex(), value);
				}
			}
		});
		return out;
	}

	Map<Integer, ConValue> actualIndexed(UpdateRowChange change) {
		Map<Integer, ConValue> out = new HashMap<>();
		Integer[] idx = change.getRowVectorIndex();
		List<ConValue> data = change.getRowData();
		for (int i = 0; i < idx.length; i++) {
			out.put(idx[i], data.get(i));
		}
		return out;
	}

	@Test
	public void testOnCopyAndSourceConflict() {
		RowSyncOutcomeHandler handler = setupHandler();
		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);
		RowCopyItemImpl copyItem = copyItem(
				List.of(new CellCopyItem().setName("a").setValue(c1).setWasChangedByUser(false),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)));
		when(mockRowHeader.getKey()).thenReturn(rowKey);
		when(mockRowHeader.fetchRow()).thenReturn(sourceItem);

		// call under test
		handler.onCopyAndSourceConflict(copyItem, mockRowHeader);

		verifyUpdatePublished(Map.of("a", c1, "b", c2), Set.of(), synRow.toConValue());
		verify(mockSourceWriter).recordFinalRowState(Map.of("a", c1, "b", c2));
		verify(mockSourceWriter, never()).applyCellChangesFromCopyToSource(anyString(), any());
	}

	@Test
	public void testOnCopyAndSourceConflictWithCellUpdatedInSource() {
		RowSyncOutcomeHandler handler = setupHandler();
		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1d, "b", c2)), rowKey, synRow);
		RowCopyItemImpl copyItem = copyItem(
				List.of(new CellCopyItem().setName("a").setValue(c1).setWasChangedByUser(false),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)));
		when(mockRowHeader.getKey()).thenReturn(rowKey);
		when(mockRowHeader.fetchRow()).thenReturn(sourceItem);

		// call under test — the source's newer value for "a" is pulled into the grid.
		handler.onCopyAndSourceConflict(copyItem, mockRowHeader);

		verifyUpdatePublished(Map.of("a", c1d, "b", c2), Set.of(), synRow.toConValue());
		verify(mockSourceWriter).recordFinalRowState(Map.of("a", c1d, "b", c2));
		verify(mockSourceWriter, never()).applyCellChangesFromCopyToSource(anyString(), any());
	}

	@Test
	public void testOnCopyAndSourceConflictWithCellUpdatedInCopy() {
		RowSyncOutcomeHandler handler = setupHandler();
		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);
		RowCopyItemImpl copyItem = copyItem(
				List.of(new CellCopyItem().setName("a").setValue(c1d).setWasChangedByUser(true),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)));
		when(mockRowHeader.getKey()).thenReturn(rowKey);
		when(mockRowHeader.fetchRow()).thenReturn(sourceItem);

		// call under test — the user-changed cell wins and is written back to the source.
		handler.onCopyAndSourceConflict(copyItem, mockRowHeader);

		verify(mockSourceWriter).applyCellChangesFromCopyToSource(rowKey, Map.of("a", c1d));
		verifyUpdatePublished(Map.of("a", c1d, "b", c2), Set.of(), synRow.toConValue());
		verify(mockSourceWriter).recordFinalRowState(Map.of("a", c1d, "b", c2));
	}

	@Test
	public void testOnCopyAndSourceConflictWithCellAddedToCopy() {
		finalSchema = List.of(new Column().setName("a").setVectorIndex(1), new Column().setName("b").setVectorIndex(0),
				new Column().setName("c").setVectorIndex(2));
		RowSyncOutcomeHandler handler = setupHandler();
		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);
		RowCopyItemImpl copyItem = copyItem(
				List.of(new CellCopyItem().setName("a").setValue(c1).setWasChangedByUser(false),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false),
						new CellCopyItem().setName("c").setValue(c3).setWasChangedByUser(true)));
		when(mockRowHeader.getKey()).thenReturn(rowKey);
		when(mockRowHeader.fetchRow()).thenReturn(sourceItem);

		// call under test
		handler.onCopyAndSourceConflict(copyItem, mockRowHeader);

		verify(mockSourceWriter).applyCellChangesFromCopyToSource(rowKey, Map.of("c", c3));
		verifyUpdatePublished(Map.of("a", c1, "b", c2, "c", c3), Set.of(), synRow.toConValue());
		verify(mockSourceWriter).recordFinalRowState(Map.of("a", c1, "b", c2, "c", c3));
	}

	@Test
	public void testOnCopyAndSourceConflictWithCellAddedToSource() {
		finalSchema = List.of(new Column().setName("a").setVectorIndex(1), new Column().setName("b").setVectorIndex(0),
				new Column().setName("c").setVectorIndex(2));
		RowSyncOutcomeHandler handler = setupHandler();
		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2, "c", c3)), rowKey, synRow);
		RowCopyItemImpl copyItem = copyItem(
				List.of(new CellCopyItem().setName("a").setValue(c1).setWasChangedByUser(false),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)));
		when(mockRowHeader.getKey()).thenReturn(rowKey);
		when(mockRowHeader.fetchRow()).thenReturn(sourceItem);

		// call under test
		handler.onCopyAndSourceConflict(copyItem, mockRowHeader);

		verifyUpdatePublished(Map.of("a", c1, "b", c2, "c", c3), Set.of(), synRow.toConValue());
		verify(mockSourceWriter).recordFinalRowState(Map.of("a", c1, "b", c2, "c", c3));
		verify(mockSourceWriter, never()).applyCellChangesFromCopyToSource(anyString(), any());
	}

	@Test
	public void testOnCopyAndSourceConflictWithNoSynapseRow() {
		RowSyncOutcomeHandler handler = setupHandler();
		this.synRow = null;
		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, (SynapseRow) null);
		RowCopyItemImpl copyItem = new RowCopyItemImpl().setVectorNodeId(rowVectorId).setRgaNodeId(rgaNodeId)
				.setMetadataNodeId(metadataNodeId)
				.setCells(List.of(new CellCopyItem().setName("a").setValue(c1).setWasChangedByUser(false),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)));
		when(mockRowHeader.getKey()).thenReturn(rowKey);
		when(mockRowHeader.fetchRow()).thenReturn(sourceItem);

		// call under test
		handler.onCopyAndSourceConflict(copyItem, mockRowHeader);

		verifyUpdatePublished(Map.of("a", c1, "b", c2), Set.of(), null);
		verify(mockSourceWriter).recordFinalRowState(Map.of("a", c1, "b", c2));
	}

	@Test
	public void testOnCopyAndSourceConflictWithUserChangedCellAndPreserveUserAttribution() {
		RowSyncOutcomeHandler handler = setupHandler(true);
		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c3)), rowKey, synRow);
		RowCopyItemImpl copyItem = copyItem(
				List.of(new CellCopyItem().setName("a").setValue(c1d).setWasChangedByUser(true),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)));
		when(mockRowHeader.getKey()).thenReturn(rowKey);
		when(mockRowHeader.fetchRow()).thenReturn(sourceItem);

		// call under test
		handler.onCopyAndSourceConflict(copyItem, mockRowHeader);

		verify(mockSourceWriter).applyCellChangesFromCopyToSource(rowKey, Map.of("a", c1d));
		// "a" is the user-winning cell -> omitted from the grid rewrite; "b" took source.
		verifyUpdatePublished(Map.of("a", c1d, "b", c3), Set.of("a"), synRow.toConValue());
		verify(mockSourceWriter).recordFinalRowState(Map.of("a", c1d, "b", c3));
	}

	@Test
	public void testOnCopyAndSourceConflictWithAllCellsUserChangedAndPreserveUserAttribution() {
		finalSchema = List.of(new Column().setName("a").setVectorIndex(1));
		RowSyncOutcomeHandler handler = setupHandler(true);
		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1)), rowKey, synRow);
		RowCopyItemImpl copyItem = copyItem(
				List.of(new CellCopyItem().setName("a").setValue(c1d).setWasChangedByUser(true)));
		when(mockRowHeader.getKey()).thenReturn(rowKey);
		when(mockRowHeader.fetchRow()).thenReturn(sourceItem);

		// call under test
		handler.onCopyAndSourceConflict(copyItem, mockRowHeader);

		verify(mockSourceWriter).applyCellChangesFromCopyToSource(rowKey, Map.of("a", c1d));
		verify(mockPublisher, never()).publish(any());
		verify(mockSourceWriter).recordFinalRowState(Map.of("a", c1d));
	}

	@Test
	public void testOnCopyAndSourceConflictWhenSourceWriteThrowsIllegalArgument() {
		RowSyncOutcomeHandler handler = setupHandler();
		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);
		RowCopyItemImpl copyItem = copyItem(
				List.of(new CellCopyItem().setName("a").setValue(c1d).setWasChangedByUser(true),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)));
		when(mockRowHeader.getKey()).thenReturn(rowKey);
		when(mockRowHeader.fetchRow()).thenReturn(sourceItem);
		doThrow(new IllegalArgumentException("bad")).when(mockSourceWriter)
				.applyCellChangesFromCopyToSource(rowKey, Map.of("a", c1d));

		// call under test
		handler.onCopyAndSourceConflict(copyItem, mockRowHeader);

		verify(mockPublisher, never()).publish(any());
		verify(mockSourceWriter, never()).recordFinalRowState(any());
	}

	@Test
	public void testOnCopyAndSourceConflictWhenSourceRowGone() {
		RowSyncOutcomeHandler handler = setupHandler();
		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);
		RowCopyItemImpl copyItem = copyItem(
				List.of(new CellCopyItem().setName("a").setValue(c1d).setWasChangedByUser(true),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)));
		when(mockRowHeader.getKey()).thenReturn(rowKey);
		when(mockRowHeader.fetchRow()).thenReturn(sourceItem);
		doThrow(new NotFoundException("gone")).when(mockSourceWriter)
				.applyCellChangesFromCopyToSource(rowKey, Map.of("a", c1d));

		// call under test
		handler.onCopyAndSourceConflict(copyItem, mockRowHeader);

		verify(mockPublisher).publish(new DeleteArrayNodeChange(rowsArrayId, rgaNodeId));
		verify(mockSourceWriter, never()).recordFinalRowState(any());
	}

}
