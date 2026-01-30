package org.sagebionetworks.grid.db.handler;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.OperationHandler;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.Delete;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.springframework.stereotype.Component;

@Component
public class DeleteHandler implements OperationHandler<Delete> {

	private GridIndexDao gridDao;
	
	public DeleteHandler(GridIndexDao gridDao) {
		this.gridDao = gridDao;
	}

	@Override
	public OperationType getOperationType() {
		return OperationType.del;
	}

	@Override
	public Set<LogicalTimestamp> handleBatch(String sessionId, Long replicaId, List<Delete> batch) {
		
		Set<LogicalTimestamp> changes = new LinkedHashSet<>();
		
		batch.forEach(d -> {
			gridDao.deleteRgaNodes(sessionId, replicaId, d.getNodeId(), d.getTimespans());
			changes.add(d.getNodeId());
		});
		
		return changes;
	}

}
