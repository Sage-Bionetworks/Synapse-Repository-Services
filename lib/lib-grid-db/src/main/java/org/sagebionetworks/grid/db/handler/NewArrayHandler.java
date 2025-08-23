package org.sagebionetworks.grid.db.handler;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.OperationHandler;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewArray;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Component;

@Component
public class NewArrayHandler implements OperationHandler<NewArray> {

	private final GridIndexDao dao;

	public NewArrayHandler(GridIndexDao dao) {
		super();
		this.dao = dao;
	}

	@Override
	public OperationType getOperationType() {
		return OperationType.new_arr;
	}

	@Override
	public Set<LogicalTimestamp> handleBatch(String sessionId, Long replicaId, List<NewArray> batch) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicId");
		ValidateArgument.required(batch, "batch");
		List<LogicalTimestamp> arrayIds = batch.stream().map(NewArray::getOperationId).collect(Collectors.toList());
		dao.saveIndex(sessionId, replicaId, IndexType.arr, arrayIds);
		dao.createArrayBatch(sessionId, replicaId, arrayIds);
		return new LinkedHashSet<>(arrayIds);
	}

}
