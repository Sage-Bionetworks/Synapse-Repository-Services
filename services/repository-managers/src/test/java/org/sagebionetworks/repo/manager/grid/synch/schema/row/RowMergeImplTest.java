package org.sagebionetworks.repo.manager.grid.synch.schema.row;

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
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.synch.core.SynchronizationLogic;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandler;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;
import org.sagebionetworks.repo.manager.grid.synch.io.RowHeader;
import org.sagebionetworks.repo.manager.grid.synch.io.SynchRow;
import org.sagebionetworks.repo.manager.grid.synch.row.CellCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItemImpl;
import org.sagebionetworks.repo.manager.grid.synch.row.RowMergeImpl;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

@ExtendWith(MockitoExtension.class)
public class RowMergeImplTest {

	@Mock
	private SourceHandler mockSourceHandler;
	@Mock
	private IntendedChangePublisher mockIntendedChangePublisher;
	@Mock
	private CopyHandler mockCopyReader;
	@Mock
	private GridHeader mockGridHeader;

	@Mock
	private RowHeader mockRowHeader;

	private List<Column> finalSchema;
	private SynchronizationLogic logic;
	private String rowKey;
	private LogicalTimestamp rowsArrayId;

	@BeforeEach
	public void before() {
		finalSchema = List.of(new Column().setName("a").setVectorIndex(1), new Column().setName("b").setVectorIndex(0));
		logic = new SynchronizationLogic();
		rowKey = "syn123";
		rowsArrayId = new LogicalTimestamp().setReplicaId(555L).setSequenceNumber(3L);
	}

	RowMergeImpl setupRowMerge() {
		when(mockCopyReader.getHeader()).thenReturn(mockGridHeader);
		when(mockGridHeader.getRowsId()).thenReturn(rowsArrayId);
		return new RowMergeImpl(logic, mockSourceHandler, mockIntendedChangePublisher, mockCopyReader, finalSchema);
	}

	@Test
	public void testMerge() {

		RowMergeImpl merge = setupRowMerge();

		ConValue c1 = new ConValue(ConType.STRING, "one");
		ConValue c2 = new ConValue(ConType.BOOLEAN, true);
		SynapseRow synRow = new SynapseRow().setRowId(123L).setVersionNumber(0L).setEtag("e1");
		SynchRow synch = new SynchRow(new TreeMap<>(Map.of("a", c1, "b", c2)), rowKey, synRow);

		LogicalTimestamp rowVectorId = new LogicalTimestamp().setReplicaId(555L).setSequenceNumber(3L);
		LogicalTimestamp rgaNodeId = new LogicalTimestamp().setReplicaId(555L).setSequenceNumber(4L);

		RowCopyItemImpl copyItem = new RowCopyItemImpl().setVectorNodeId(rowVectorId).setRgaNodeId(rgaNodeId)
				.setCells(List.of(new CellCopyItem().setName("a").setValue(c1),
						new CellCopyItem().setName("b").setValue(c2)))
				.setSynapseRow(new SynapseRow().setRowId(123L).setVersionNumber(0L).setEtag("e2"));

		when(mockRowHeader.fetchRow()).thenReturn(synch);

		// all under test
		merge.merge(rowKey, copyItem, mockRowHeader);

		verify(mockIntendedChangePublisher)
				.publish(new UpdateRowChange(rowVectorId, List.of(c1, c2), new Integer[] { 1, 0 }));

		verifyNoMoreInteractionsWithAllMocks();
	}

	private void verifyNoMoreInteractionsWithAllMocks() {
		verifyNoMoreInteractions(mockCopyReader, mockGridHeader, mockRowHeader, mockSourceHandler,
				mockIntendedChangePublisher);
	}

}
