package org.sagebionetworks.repo.manager.grid.synch.row;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.DeleteArrayNodeChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.synch.core.SynchronizationLogic;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandler;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReference;
import org.sagebionetworks.repo.manager.grid.synch.row.CellCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItemImpl;
import org.sagebionetworks.repo.manager.grid.synch.row.RowMergeImpl;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class RowMergeImplTest {

	@Mock
	private SourceHandler mockSourceHandler;
	@Mock
	private IntendedChangePublisher mockIntendedChangePublisher;
	@Mock
	private CopyHandler mockCopyHandler;
	@Mock
	private GridHeader mockGridHeader;

	@Mock
	private RowSourceItemReference mockRowSourceItemReference;

	private List<Column> finalSchema;
	private SynchronizationLogic logic;
	private String rowKey;

	private ConValue c1;
	private ConValue c2;
	private ConValue c1d;
	private ConValue c3;
	private SynapseRow synRow;
	private LogicalTimestamp rowsArrayId;
	private LogicalTimestamp rowVectorId;
	private LogicalTimestamp rgaNodeId;
	private LogicalTimestamp metadataNodeId;

	@BeforeEach
	public void before() {
		finalSchema = List.of(new Column().setName("a").setVectorIndex(1), new Column().setName("b").setVectorIndex(0));
		logic = new SynchronizationLogic();
		rowKey = "syn123";
		c1 = new ConValue(ConType.STRING, "one");
		c1d = new ConValue(ConType.STRING, "two");
		c2 = new ConValue(ConType.BOOLEAN, true);
		c3 = new ConValue(ConType.LONG, 45L);
		synRow = new SynapseRow().setRowId(123L).setVersionNumber(0L).setEtag("e1");

		rowsArrayId = new LogicalTimestamp().setReplicaId(555L).setSequenceNumber(3L);
		rowVectorId = new LogicalTimestamp().setReplicaId(555L).setSequenceNumber(3L);
		rgaNodeId = new LogicalTimestamp().setReplicaId(555L).setSequenceNumber(4L);
		metadataNodeId = new LogicalTimestamp().setReplicaId(555L).setSequenceNumber(5L);

	}

	RowMergeImpl setupRowMerge() {
		when(mockCopyHandler.getHeader()).thenReturn(mockGridHeader);
		when(mockGridHeader.getRowsId()).thenReturn(rowsArrayId);
		return new RowMergeImpl(logic, mockSourceHandler, mockIntendedChangePublisher, mockCopyHandler, finalSchema);
	}

	@Test
	public void testMerge() {

		RowMergeImpl merge = setupRowMerge();

		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);

		RowCopyItemImpl copyItem = new RowCopyItemImpl().setVectorNodeId(rowVectorId).setRgaNodeId(rgaNodeId)
				.setMetadataNodeId(metadataNodeId)
				.setCells(List.of(new CellCopyItem().setName("a").setValue(c1).setWasChangedByUser(false),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)))
				.setSynapseRow(synRow);

		when(mockRowSourceItemReference.fetchRow()).thenReturn(sourceItem);

		// call under test
		merge.merge(rowKey, copyItem, mockRowSourceItemReference);

		verify(mockIntendedChangePublisher).publish(new UpdateRowChange(rowVectorId, List.of(c1, c2),
				new Integer[] { 1, 0 }, metadataNodeId, synRow.toConValue()));

		verifyNoMoreInteractionsWithAllMocks();
	}

	@Test
	public void testMergeWithCellUpdtedInSource() {

		RowMergeImpl merge = setupRowMerge();

		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1d, "b", c2)), rowKey, synRow);

		RowCopyItemImpl copyItem = new RowCopyItemImpl().setVectorNodeId(rowVectorId).setRgaNodeId(rgaNodeId)
				.setMetadataNodeId(metadataNodeId)
				.setCells(List.of(new CellCopyItem().setName("a").setValue(c1).setWasChangedByUser(false),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)))
				.setSynapseRow(synRow);

		when(mockRowSourceItemReference.fetchRow()).thenReturn(sourceItem);

		// call under test
		merge.merge(rowKey, copyItem, mockRowSourceItemReference);

		verify(mockIntendedChangePublisher).publish(new UpdateRowChange(rowVectorId, List.of(c1d, c2),
				new Integer[] { 1, 0 }, metadataNodeId, synRow.toConValue()));

		verifyNoMoreInteractionsWithAllMocks();
	}

	@Test
	public void testMergeWithCellUpdatedInCopy() {

		RowMergeImpl merge = setupRowMerge();

		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);

		RowCopyItemImpl copyItem = new RowCopyItemImpl().setVectorNodeId(rowVectorId).setRgaNodeId(rgaNodeId)
				.setMetadataNodeId(metadataNodeId)
				.setCells(List.of(new CellCopyItem().setName("a").setValue(c1d).setWasChangedByUser(true),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)))
				.setSynapseRow(synRow);

		when(mockRowSourceItemReference.fetchRow()).thenReturn(sourceItem);

		// call under test
		merge.merge(rowKey, copyItem, mockRowSourceItemReference);

		verify(mockIntendedChangePublisher).publish(new UpdateRowChange(rowVectorId, List.of(c1d, c2),
				new Integer[] { 1, 0 }, metadataNodeId, synRow.toConValue()));

		verify(mockSourceHandler).applyCellChangesFromCopyToSource(rowKey, Map.of("a", c1d));

		verifyNoMoreInteractionsWithAllMocks();
	}

	@Test
	public void testMergeWithCellUpdatedInCopyAndApplyCellsThrowsIllegalArgumentException() {

		RowMergeImpl merge = setupRowMerge();

		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);

		RowCopyItemImpl copyItem = new RowCopyItemImpl().setVectorNodeId(rowVectorId).setRgaNodeId(rgaNodeId)
				.setMetadataNodeId(metadataNodeId)
				.setCells(List.of(new CellCopyItem().setName("a").setValue(c1d).setWasChangedByUser(true),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)))
				.setSynapseRow(synRow);

		when(mockRowSourceItemReference.fetchRow()).thenReturn(sourceItem);

		doThrow(new IllegalArgumentException("not right")).when(mockSourceHandler)
				.applyCellChangesFromCopyToSource(rowKey, Map.of("a", c1d));

		// call under test
		merge.merge(rowKey, copyItem, mockRowSourceItemReference);

		verify(mockIntendedChangePublisher, never()).publish(any());

		verify(mockSourceHandler).applyCellChangesFromCopyToSource(rowKey, Map.of("a", c1d));

		verifyNoMoreInteractionsWithAllMocks();
	}
	
	@Test
	public void testMergeWithCellUpdatedInCopyAndApplyCellsNotFoundException() {

		RowMergeImpl merge = setupRowMerge();

		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);

		RowCopyItemImpl copyItem = new RowCopyItemImpl().setVectorNodeId(rowVectorId).setRgaNodeId(rgaNodeId)
				.setMetadataNodeId(metadataNodeId)
				.setCells(List.of(new CellCopyItem().setName("a").setValue(c1d).setWasChangedByUser(true),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)))
				.setSynapseRow(synRow);

		when(mockRowSourceItemReference.fetchRow()).thenReturn(sourceItem);

		doThrow(new NotFoundException("does not exist")).when(mockSourceHandler)
				.applyCellChangesFromCopyToSource(rowKey, Map.of("a", c1d));

		// call under test
		merge.merge(rowKey, copyItem, mockRowSourceItemReference);

		verify(mockIntendedChangePublisher).publish(new DeleteArrayNodeChange(rowsArrayId, rgaNodeId));

		verify(mockSourceHandler).applyCellChangesFromCopyToSource(rowKey, Map.of("a", c1d));

		verifyNoMoreInteractionsWithAllMocks();
	}
	
	@Test
	public void testMergeWithCellUpdatedInCopyAndApplyCellsUnauthorizedException() {

		RowMergeImpl merge = setupRowMerge();

		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);

		RowCopyItemImpl copyItem = new RowCopyItemImpl().setVectorNodeId(rowVectorId).setRgaNodeId(rgaNodeId)
				.setMetadataNodeId(metadataNodeId)
				.setCells(List.of(new CellCopyItem().setName("a").setValue(c1d).setWasChangedByUser(true),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)))
				.setSynapseRow(synRow);

		when(mockRowSourceItemReference.fetchRow()).thenReturn(sourceItem);

		doThrow(new UnauthorizedException("not allowed")).when(mockSourceHandler)
				.applyCellChangesFromCopyToSource(rowKey, Map.of("a", c1d));

		// call under test
		merge.merge(rowKey, copyItem, mockRowSourceItemReference);

		verify(mockIntendedChangePublisher).publish(new DeleteArrayNodeChange(rowsArrayId, rgaNodeId));

		verify(mockSourceHandler).applyCellChangesFromCopyToSource(rowKey, Map.of("a", c1d));

		verifyNoMoreInteractionsWithAllMocks();
	}

	@Test
	public void testMergeWithCellAddedToCopy() {
		finalSchema = List.of(new Column().setName("a").setVectorIndex(1), new Column().setName("b").setVectorIndex(0),
				new Column().setName("c").setVectorIndex(2));
		RowMergeImpl merge = setupRowMerge();

		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);

		RowCopyItemImpl copyItem = new RowCopyItemImpl().setVectorNodeId(rowVectorId).setRgaNodeId(rgaNodeId)
				.setMetadataNodeId(metadataNodeId)
				.setCells(List.of(new CellCopyItem().setName("a").setValue(c1).setWasChangedByUser(false),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false),
						new CellCopyItem().setName("c").setValue(c3).setWasChangedByUser(true)))
				.setSynapseRow(synRow);

		when(mockRowSourceItemReference.fetchRow()).thenReturn(sourceItem);

		// call under test
		merge.merge(rowKey, copyItem, mockRowSourceItemReference);

		verify(mockIntendedChangePublisher).publish(new UpdateRowChange(rowVectorId, List.of(c1, c2, c3),
				new Integer[] { 1, 0, 2 }, metadataNodeId, synRow.toConValue()));

		verify(mockSourceHandler).applyCellChangesFromCopyToSource(rowKey, Map.of("c", c3));

		verifyNoMoreInteractionsWithAllMocks();
	}

	@Test
	public void testMergeWithCellAddedToSource() {
		finalSchema = List.of(new Column().setName("a").setVectorIndex(1), new Column().setName("b").setVectorIndex(0),
				new Column().setName("c").setVectorIndex(2));
		RowMergeImpl merge = setupRowMerge();

		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2, "c", c3)), rowKey, synRow);

		RowCopyItemImpl copyItem = new RowCopyItemImpl().setVectorNodeId(rowVectorId).setRgaNodeId(rgaNodeId)
				.setMetadataNodeId(metadataNodeId)
				.setCells(List.of(new CellCopyItem().setName("a").setValue(c1).setWasChangedByUser(false),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)))
				.setSynapseRow(synRow);

		when(mockRowSourceItemReference.fetchRow()).thenReturn(sourceItem);

		// call under test
		merge.merge(rowKey, copyItem, mockRowSourceItemReference);

		verify(mockIntendedChangePublisher).publish(new UpdateRowChange(rowVectorId, List.of(c1, c2, c3),
				new Integer[] { 1, 0, 2 }, metadataNodeId, synRow.toConValue()));

		verifyNoMoreInteractionsWithAllMocks();
	}

	@Test
	public void testMergeWithCellDeletedFromSource() {
		finalSchema = List.of(new Column().setName("a").setVectorIndex(1), new Column().setName("b").setVectorIndex(0));
		RowMergeImpl merge = setupRowMerge();

		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);

		RowCopyItemImpl copyItem = new RowCopyItemImpl().setVectorNodeId(rowVectorId).setRgaNodeId(rgaNodeId)
				.setMetadataNodeId(metadataNodeId)
				.setCells(List.of(new CellCopyItem().setName("a").setValue(c1).setWasChangedByUser(false),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false),
						new CellCopyItem().setName("c").setValue(c3).setWasChangedByUser(false)))
				.setSynapseRow(synRow);

		when(mockRowSourceItemReference.fetchRow()).thenReturn(sourceItem);

		// call under test
		merge.merge(rowKey, copyItem, mockRowSourceItemReference);

		verify(mockIntendedChangePublisher).publish(new UpdateRowChange(rowVectorId, List.of(c1, c2),
				new Integer[] { 1, 0 }, metadataNodeId, synRow.toConValue()));

		verifyNoMoreInteractionsWithAllMocks();
	}


	@Test
	public void testMergeWithNoSynapseRow() {

		RowMergeImpl merge = setupRowMerge();

		SynapseRow synRow = null;
		RowSourceItem sourceItem = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);

		RowCopyItemImpl copyItem = new RowCopyItemImpl().setVectorNodeId(rowVectorId).setRgaNodeId(rgaNodeId)
				.setMetadataNodeId(metadataNodeId)
				.setCells(List.of(new CellCopyItem().setName("a").setValue(c1).setWasChangedByUser(false),
						new CellCopyItem().setName("b").setValue(c2).setWasChangedByUser(false)))
				.setSynapseRow(synRow);

		when(mockRowSourceItemReference.fetchRow()).thenReturn(sourceItem);

		// call under test
		merge.merge(rowKey, copyItem, mockRowSourceItemReference);

		verify(mockIntendedChangePublisher).publish(
				new UpdateRowChange(rowVectorId, List.of(c1, c2), new Integer[] { 1, 0 }, metadataNodeId, null));

		verifyNoMoreInteractionsWithAllMocks();
	}

	private void verifyNoMoreInteractionsWithAllMocks() {
		verifyNoMoreInteractions(mockCopyHandler, mockGridHeader, mockRowSourceItemReference, mockSourceHandler,
				mockIntendedChangePublisher);
	}

}
