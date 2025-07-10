package org.sagebionetworks.grid.db.handler;

import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.OperationHandler;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.patch.operation.NewObject;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Repository;

@Repository
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
	public void handleBatch(String sessionId, Long replicaId, List<NewObject> batch) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicId");
		ValidateArgument.required(batch, "batch");
		dao.saveIndex(sessionId, replicaId, IndexType.obj,
				batch.stream().map(NewObject::getOperationId).collect(Collectors.toList()));
		dao.saveObjects(sessionId, replicaId,
				batch.stream().map(o -> new ObjectNode().setId(o.getOperationId())).collect(Collectors.toList()));
	}

}
