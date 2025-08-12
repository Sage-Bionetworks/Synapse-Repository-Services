package org.sagebionetworks.grid.db.handler;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.OperationHandler;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewObject;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Component;

@Component
public class NewObjectHandler implements OperationHandler<NewObject> {

	private final GridIndexDao dao;

	public NewObjectHandler(GridIndexDao dao) {
		super();
		this.dao = dao;
	}

	@Override
	public OperationType getOperationType() {
		return OperationType.new_obj;
	}

	@Override
	public Set<LogicalTimestamp> handleBatch(String sessionId, Long replicaId, List<NewObject> batch) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicId");
		ValidateArgument.required(batch, "batch");
		List<LogicalTimestamp> objIds = batch.stream().map(NewObject::getOperationId).collect(Collectors.toList());
		dao.saveIndex(sessionId, replicaId, IndexType.obj, objIds);
		dao.saveObjects(sessionId, replicaId,
				batch.stream().map(o -> new ObjectNode().setId(o.getOperationId())).collect(Collectors.toList()));
		return new LinkedHashSet<>(objIds);
	}

}
