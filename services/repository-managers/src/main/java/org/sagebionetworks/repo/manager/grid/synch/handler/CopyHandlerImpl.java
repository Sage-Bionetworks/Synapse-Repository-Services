package org.sagebionetworks.repo.manager.grid.synch.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSupport;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.QueryElement;
import org.sagebionetworks.repo.manager.grid.synch.row.CellCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItemImpl;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridConstants;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.RGANode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.web.NotFoundException;

public class CopyHandlerImpl implements CopyHandler {

	private final GridReplicaViewManager gridReplicaViewManager;
	private final GridSource gridSource;
	private final GridConnectionInfo connection;
	private final GridHeader header;
	private final LogicalTimestamp lastRowsRgaNodeId;
	private final Map<Integer, String> indexToColumnMap;

	public CopyHandlerImpl(GridReplicaViewManager gridReplicaViewManager, GridReplicaSupport gridReplicaSupport,
			GridIndexDao gridIndexDao, GridManager gridManager, GridSession gridSession) {
		this.gridReplicaViewManager = gridReplicaViewManager;
		this.header = gridReplicaSupport.getGridHeaderOrThrow(gridSession);
		this.gridSource = getSourceOrThrow(gridManager, gridSession.getSessionId());
		this.connection = getConnectionOrThrow(gridManager, gridSession.getSessionId());
		this.lastRowsRgaNodeId = getLastRowsRgaNodeId(gridIndexDao, header);
		this.indexToColumnMap = buildIndexToColumnMap(header.getOrderedColumns());
	}

	GridSource getSourceOrThrow(GridManager gridManager, String sessionId) {
		return gridManager.getSessionSource(sessionId).orElseThrow(() -> new NotFoundException("Grid: " + sessionId));
	}

	GridConnectionInfo getConnectionOrThrow(GridManager gridManager, String sessionId) {
		return gridManager.getSingletonConnection(sessionId, EventSource.USER_SUPPORT)
				.orElseThrow(() -> new NotFoundException("Grid: " + sessionId));
	}

	LogicalTimestamp getLastRowsRgaNodeId(GridIndexDao gridIndexDao, GridHeader header) {
		return gridIndexDao.getArrayLastNodeId(header.getSessionId(), header.getReplicaId(), header.getRowsId());
	}

	private Map<Integer, String> buildIndexToColumnMap(List<Column> columns) {
		Map<Integer, String> map = new HashMap<>(columns.size());
		for (int i = 0; i < columns.size(); i++) {
			map.put(i, columns.get(i).getName());
		}
		return map;
	}

	@Override
	public void close() {
	}

	@Override
	public GridHeader getHeader() {
		return header;
	}

	@Override
	public GridConnectionInfo getConnectionInfo() {
		return connection;
	}

	@Override
	public Iterator<RowCopyItem> getRows() {
		Iterator<RowView> rowIterator = gridReplicaViewManager.getQueryIterator(header, new QueryElement());
		return new Iterator<RowCopyItem>() {

			@Override
			public RowCopyItem next() {
				RowView rowView = rowIterator.next();
				return createCopyRow(rowView);
			}

			@Override
			public boolean hasNext() {
				return rowIterator.hasNext();
			}
		};
	}

	private RowCopyItem createCopyRow(RowView rowView) {
		LogicalTimestamp vectorId = rowView.getRowObject().getData().getVectorId();
		List<CellCopyItem> cells = createCopyCells(rowView.getRowObject().getData().getNodes());
		return new RowCopyItemImpl().setCells(cells).setRgaNodeId(rowView.getArrNodeId()).setVectorNodeId(vectorId)
				.setMetadataNodeId(rowView.getRowMetadataNodeId()).setSynapseRow(rowView.getSynapseRow());
	}

	private List<CellCopyItem> createCopyCells(List<ConstantNode> nodes) {
		List<CellCopyItem> cells = new ArrayList<>(nodes.size());
		for (int i = 0; i < nodes.size(); i++) {
			ConstantNode node = nodes.get(i);
			boolean wasChangedByUser = GridConstants.isUserReplica(node.getId().getReplicaId());
			String columnName = indexToColumnMap.get(i);
			cells.add(new CellCopyItem().setName(columnName).setValue(node.getConValue())
					.setWasChangedByUser(wasChangedByUser));
		}
		return cells;
	}

	@Override
	public GridSource getGridSource() {
		return gridSource;
	}

	@Override
	public LogicalTimestamp getLastRowsRgaNodeId() {
		return lastRowsRgaNodeId;
	}

}
