package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.InsertRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangeSet;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.PatchBuilderPublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridCsvImportResponse;
import org.sagebionetworks.repo.model.grid.node.RGANode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.table.ColumnType;

@ExtendWith(MockitoExtension.class)
public class JoinedRowChangePublisherTest {

	@Mock
	private PatchBuilderPublisher mockPatchBuilderPublisher;
	@Mock
	private GridIndexDao mockGridIndexDao;	
	@Mock
	private GridConnectionInfo mockConnInfo;
	@Mock
	private LogicalTimestamp mockTimestamp;
	
	@InjectMocks
	private JoinedRowChangePublisher changePublisher;
	
	private GridHeader header;
	private Long repId = 123L;
	private List<JoinedRow> rows;
	private ColumnMapping[] mapping;
	private Integer[] vecIndex;

	@BeforeEach
	void setUp() {
		
		header = new GridHeader()
			.setSessionId("sessionId")
			.setReplicaId(repId)
			.setClockSequenceMaximum(234L)
			.setRowsId(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(33L))
			.setOrderedColumns(List.of(
				new Column().setName("b").setVectorIndex(3),
				new Column().setName("a").setVectorIndex(4),
				new Column().setName("c").setVectorIndex(5)
			));
		
		rows = List.of(
			new JoinedRow(List.of(new ConValue(ConType.LONG, 1), new ConValue(ConType.LONG, 2), new ConValue(ConType.LONG, 3)), new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(1L)),
			new JoinedRow(List.of(new ConValue(ConType.LONG, 2), new ConValue(ConType.LONG, 3), new ConValue(ConType.LONG, 4)), new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(2L)),
			new JoinedRow(List.of(new ConValue(ConType.LONG, 5), new ConValue(ConType.LONG, 6), new ConValue(ConType.LONG, 7)), null)
		);
		
		mapping = new ColumnMapping[] {
			new ColumnMapping("a", ColumnType.INTEGER, 0, 1, true),
			new ColumnMapping("b", ColumnType.INTEGER, 1, 0, false),
			new ColumnMapping("c", ColumnType.INTEGER, 2, 2, false)
		};
		
		vecIndex = new Integer[] { 4, 3, 5 };
	}

	@Test
	public void testProcessJoinedRows() {
		when(mockGridIndexDao.getRgaLastNode(header.getSessionId(), header.getReplicaId(), header.getRowsId()))
			.thenReturn(Optional.of(new RGANode().setNodeId(mockTimestamp)));
		
		when(mockConnInfo.getSessionId()).thenReturn(header.getSessionId());
		when(mockConnInfo.getConnectionId()).thenReturn("connId");
		when(mockConnInfo.getReplicaId()).thenReturn(789L);
		
		
		// Call under test
		GridCsvImportResponse result = changePublisher.processJoinedRows(header, mockConnInfo, rows.iterator(), mapping);
	
		assertEquals(new GridCsvImportResponse()
			.setSessionId(header.getSessionId())
			.setTotalCount(3L)
			.setUpdatedCount(2L)
			.setCreatedCount(1L), 
			result
		);
		
		List<IntendedChange> changes = rows.stream().map(row -> {
			if (row.getGridRowVecId() != null) {
				return new UpdateRowChange(row.getGridRowVecId(), row.getCsvData(), vecIndex);
			} else {
				return new InsertRowChange(header.getRowsId(), mockTimestamp, row.getCsvData(), vecIndex);
			}
		}).collect(Collectors.toList());
		
		verify(mockPatchBuilderPublisher).sendChangesToPatchBuilder(new IntendedChangeSet()
			.setSessionId(header.getSessionId())
			.setReplicaId(789L)
			.setConnectionId("connId")
			.setClockSequenceMaximum(header.getClockSequenceMaximum())
			.setChanges(changes)
		);
		
		
	}
}