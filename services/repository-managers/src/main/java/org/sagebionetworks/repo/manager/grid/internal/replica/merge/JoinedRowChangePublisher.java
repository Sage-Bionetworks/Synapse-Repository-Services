package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import java.util.Arrays;
import java.util.Iterator;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.PatchUtils;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.InsertRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.PatchBuilderPublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridCsvImportResponse;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.springframework.stereotype.Service;

/**
 * Helper to process a stream of joined rows and publish the corresponding row changes
 */
@Service
public class JoinedRowChangePublisher {

	private final PatchBuilderPublisher patchBuilderPublisher;
	private final GridIndexDao gridIndexDao;
	
	public JoinedRowChangePublisher(PatchBuilderPublisher patchBuilderPublisher, GridIndexDao gridIndexDao) {
		this.patchBuilderPublisher = patchBuilderPublisher;
		this.gridIndexDao = gridIndexDao;
	}
	
	public GridCsvImportResponse processJoinedRows(GridHeader header, GridConnectionInfo connInfo, Iterator<JoinedRow> joinedRowStream, ColumnMapping[] columnMapping) {
		
		long rowCount = 0;
		long updatedCount = 0;
		long createdCount = 0;
		
		// Maps each column to the actual vector index defined in the grid
		Integer[] gridVectorIndex = Arrays.stream(columnMapping)
			.map(m -> header.getOrderedColumns().get(m.getGridIndex()).getVectorIndex())
			.toArray(Integer[]::new);
		
		try (IntendedChangePublisher changePublisher = new IntendedChangePublisher(connInfo, header.getClockSequenceMaximum(), patchBuilderPublisher, PatchUtils.MAX_CHANGE_SET_SIZE)) {
			LogicalTimestamp rowsArrayId = header.getRowsId();
			
			// The iterator of joined rows is sorted by upsert key descending, this allows us to use the current last row id
			// as the insert position for all new rows (since they will be inserted in reverse order)
			LogicalTimestamp lastRowId = gridIndexDao.getArrayLastNode(header.getSessionId(), header.getReplicaId(), rowsArrayId)
				.map(ArrayNode::getNodeId)
				.orElse(null);
			
			while (joinedRowStream.hasNext()) {
				JoinedRow joinedRow = joinedRowStream.next();
				
				IntendedChange change;
				
				if (joinedRow.getGridRowVecId() == null) {
					change = new InsertRowChange(rowsArrayId, lastRowId, joinedRow.getCsvData(), gridVectorIndex);
					createdCount++;
				} else {
					change = new UpdateRowChange(joinedRow.getGridRowVecId(), joinedRow.getCsvData(), gridVectorIndex);
					updatedCount++;
				}
				
				changePublisher.publish(change);
				
				rowCount++;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
		
		return new GridCsvImportResponse()
			.setTotalCount(rowCount)
			.setUpdatedCount(updatedCount)
			.setCreatedCount(createdCount)
			.setSessionId(header.getSessionId());
	}
	
}
