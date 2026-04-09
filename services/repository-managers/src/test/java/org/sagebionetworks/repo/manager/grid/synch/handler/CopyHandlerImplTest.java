package org.sagebionetworks.repo.manager.grid.synch.handler;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSupport;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowMetadata;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.QueryElement;
import org.sagebionetworks.repo.manager.grid.synch.row.CellCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItemImpl;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridConstants;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.RGANode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

@ExtendWith(MockitoExtension.class)
public class CopyHandlerImplTest {

	@Mock
	private GridReplicaViewManager mockGridReplicaViewManager;
	@Mock
	private GridSource mockGridSource;
	@Mock
	private GridConnectionInfo mockConnection;
	@Mock
	private GridHeader mockHeader;
	@Mock
	private GridReplicaSupport mockGridReplicaSupport;
	@Mock
	private GridIndexDao mockGridIndexDao;
	@Mock
	private GridManager mockGridManager;

	private String sessionId;
	private GridSession gridSession;
	private List<Column> columns;
	private GridSource gridSource;

	private Long internalReplicaId;
	private Long userReplicaId;
	private LogicalTimestamp lastRowsRgaNodeId;
	private LogicalTimestamp rowsId;

	@BeforeEach
	public void before() {
		sessionId = "123";
		internalReplicaId = GridConstants.START_REPLICA_ID_SERVICE;
		userReplicaId = GridConstants.START_REPLICA_ID_CLIENT;
		gridSession = new GridSession().setSessionId(sessionId);
		lastRowsRgaNodeId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L);
		rowsId = new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(4L);
		columns = List.of(new Column().setName("a").setVectorIndex(1), new Column().setName("b").setVectorIndex(0));
		gridSource = new GridSource(222L, EntityType.entityview);
	}

	CopyHandlerImpl setupHandler() {
		when(mockGridReplicaSupport.getGridHeaderOrThrow(gridSession)).thenReturn(mockHeader);
		when(mockHeader.getOrderedColumns()).thenReturn(columns);
		when(mockGridManager.getSessionSource(sessionId)).thenReturn(Optional.of(gridSource));
		when(mockGridManager.getSingletonConnection(sessionId, EventSource.USER_SUPPORT))
				.thenReturn(Optional.of(mockConnection));
		when(mockHeader.getRowsId()).thenReturn(rowsId);
		when(mockGridIndexDao.getArrayLastNodeId(sessionId, internalReplicaId, rowsId)).thenReturn(lastRowsRgaNodeId);
		when(mockHeader.getSessionId()).thenReturn(sessionId);
		when(mockHeader.getReplicaId()).thenReturn(internalReplicaId);
		return new CopyHandlerImpl(mockGridReplicaViewManager, mockGridReplicaSupport, mockGridIndexDao,
				mockGridManager, gridSession);
	}

	@Test
	public void testCreateHandler() {
		try (CopyHandlerImpl handler = setupHandler()) {
			assertEquals(lastRowsRgaNodeId, handler.getLastRowsRgaNodeId());
			assertEquals(mockConnection, handler.getConnectionInfo());
			assertEquals(mockHeader, handler.getHeader());
			assertEquals(gridSource, handler.getGridSource());
		}
		verifyNoMoreInteracationOnAllMocks();
	}

	private void verifyNoMoreInteracationOnAllMocks() {
		verifyNoMoreInteractions(mockConnection, mockGridIndexDao, mockGridManager, mockGridReplicaViewManager,
				mockGridSource, mockHeader);
	}

	@Test
	public void testGetRows() {
		try (CopyHandlerImpl handler = setupHandler()) {

			List<RowView> rowViews = List.of(
					// 111
					new RowView()
							.setArrNodeId(new LogicalTimestamp().setReplicaId(
									internalReplicaId).setSequenceNumber(
											6L))
							.setRowObject(
									new RowObject()
											.setData(
													new RowData().setVectorId(
															new LogicalTimestamp().setReplicaId(internalReplicaId)
																	.setSequenceNumber(7L))
															.setNodes(List.of(new ConstantNode()
																	.setId(new LogicalTimestamp()
																			.setReplicaId(internalReplicaId)
																			.setSequenceNumber(8L))
																	.setValue(new ConValue(ConType.STRING, "foo")))))
											.setMetadata(
													new RowMetadata().setSynapseRow(new SynapseRow().setRowId(111L)))),
					// 222
					new RowView()
							.setArrNodeId(new LogicalTimestamp().setReplicaId(
									internalReplicaId).setSequenceNumber(
											9L))
							.setRowObject(
									new RowObject()
											.setData(
													new RowData().setVectorId(
															new LogicalTimestamp().setReplicaId(internalReplicaId)
																	.setSequenceNumber(10L))
															.setNodes(List.of(new ConstantNode()
																	.setId(new LogicalTimestamp()
																			.setReplicaId(userReplicaId)
																			.setSequenceNumber(11L))
																	.setValue(new ConValue(ConType.STRING, "bar")))))
											.setMetadata(
													new RowMetadata().setSynapseRow(new SynapseRow().setRowId(222L)))));

			when(mockGridReplicaViewManager.getQueryIterator(mockHeader, new QueryElement()))
					.thenReturn(rowViews.iterator());
			// call under test
			Iterator<RowCopyItem> it = handler.getRows();
			assertNotNull(it);
			List<RowCopyItem> results = new ArrayList<>();
			while (it.hasNext()) {
				results.add(it.next());
			}

			List<RowCopyItem> expected = List.of(
					// 111
					new RowCopyItemImpl().setSynapseRow(new SynapseRow().setRowId(111L))
							.setRgaNodeId(new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(6L))
							.setVectorNodeId(
									new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(7L))
							.setCells(List.of(new CellCopyItem().setName("a").setValue(new ConValue(ConType.STRING, "foo"))
									.setWasChangedByUser(false))),
					// 222
					new RowCopyItemImpl().setSynapseRow(new SynapseRow().setRowId(222L))
							.setRgaNodeId(new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(9L))
							.setVectorNodeId(
									new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(10L))
							.setCells(List.of(new CellCopyItem().setName("a").setValue(new ConValue(ConType.STRING, "bar"))
									.setWasChangedByUser(true)))

			);

			assertEquals(expected, results);
		}
		verifyNoMoreInteracationOnAllMocks();
	}

	RowView createRowView(Long rowId, LogicalTimestamp rowRgaNodeId, LogicalTimestamp rowVectorId,
			List<ConstantNode> nodes) {
		return new RowView().setArrNodeId(rowRgaNodeId)
				.setRowObject(new RowObject().setData(new RowData().setVectorId(rowVectorId).setNodes(nodes))
						.setMetadata(new RowMetadata().setSynapseRow(new SynapseRow().setRowId(rowId))));
	}

}
