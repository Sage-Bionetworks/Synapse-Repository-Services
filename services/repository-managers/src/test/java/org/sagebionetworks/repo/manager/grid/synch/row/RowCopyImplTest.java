package org.sagebionetworks.repo.manager.grid.synch.row;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.DeleteArrayNodeChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.InsertRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandler;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReference;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyImpl;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItemImpl;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

@ExtendWith(MockitoExtension.class)
public class RowCopyImplTest {

	@Mock
	private IntendedChangePublisher mockIntendedChangePublisher;
	@Mock
	private CopyHandler mockCopyHandler;
	@Mock
	private GridHeader mockHeader;
	@Mock
	private RowSourceItemReference mockRowHeader;

	private List<Column> finalSchema;
	private LogicalTimestamp lastRowsRgaNodeId;
	private LogicalTimestamp rowsArrayId;

	@BeforeEach
	public void before() {
		finalSchema = List.of(new Column().setName("a").setVectorIndex(1), new Column().setName("b").setVectorIndex(0));
		rowsArrayId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L);
		lastRowsRgaNodeId = new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L);
	}

	RowCopyImpl setupCopy() {
		when(mockHeader.getRowsId()).thenReturn(rowsArrayId);
		when(mockCopyHandler.getHeader()).thenReturn(mockHeader);
		when(mockCopyHandler.getLastRowsRgaNodeId()).thenReturn(lastRowsRgaNodeId);
		return new RowCopyImpl(finalSchema, mockIntendedChangePublisher, mockCopyHandler);
	}

	@Test
	public void testStreamItems() {
		List<RowCopyItem> rows = List.of(new RowCopyItemImpl().setSynapseRow(new SynapseRow().setRowId(111L)));
		when(mockCopyHandler.getRows()).thenReturn(rows.iterator());
		RowCopyImpl copy = setupCopy();
		// call under test
		List<RowCopyItem> results = copy.streamItems().collect(Collectors.toList());
		assertEquals(rows, results);

		verifyNoMoreInteractionsWithAllMocks();
	}

	private void verifyNoMoreInteractionsWithAllMocks() {
		verifyNoMoreInteractions(mockCopyHandler, mockHeader, mockIntendedChangePublisher);
	}

	@Test
	public void testRemoveItem() {
		RowCopyImpl copy = setupCopy();

		copy.removeItem(
				new RowCopyItemImpl().setRgaNodeId(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L)));

		verify(mockIntendedChangePublisher).publish(
				new DeleteArrayNodeChange(rowsArrayId, new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L)));
		verifyNoMoreInteractionsWithAllMocks();
	}

	@Test
	public void testAddItem() {
		RowCopyImpl copy = setupCopy();
		SynapseRow synRow = new SynapseRow().setRowId(123L).setVersionNumber(0L).setEtag("e1");
		ConValue firstCellValue = new ConValue(ConType.LONG, 222L);
		ConValue secondCellValue = new ConValue(ConType.STRING, "other");
		when(mockRowHeader.fetchRow()).thenReturn(
				new RowSourceItem(new TreeMap<>(Map.of("a", firstCellValue, "b", new ConValue(ConType.STRING, "other"))),
						"syn123", synRow));

		// call under test
		copy.addItem(mockRowHeader);
		verify(mockIntendedChangePublisher).publish(new InsertRowChange(rowsArrayId, lastRowsRgaNodeId,
				List.of(firstCellValue, secondCellValue), new Integer[] { 1, 0 }, synRow.toConValue()));
	}
	
	@Test
	public void testAddItemWithNullSynapseRow() {
		RowCopyImpl copy = setupCopy();
		SynapseRow synRow = null;
		ConValue firstCellValue = new ConValue(ConType.LONG, 222L);
		ConValue secondCellValue = new ConValue(ConType.STRING, "other");
		when(mockRowHeader.fetchRow()).thenReturn(
				new RowSourceItem(new TreeMap<>(Map.of("a", firstCellValue, "b", new ConValue(ConType.STRING, "other"))),
						"syn123", synRow));

		// call under test
		copy.addItem(mockRowHeader);
		verify(mockIntendedChangePublisher).publish(new InsertRowChange(rowsArrayId, lastRowsRgaNodeId,
				List.of(firstCellValue, secondCellValue), new Integer[] { 1, 0 }, null));
	}

}
