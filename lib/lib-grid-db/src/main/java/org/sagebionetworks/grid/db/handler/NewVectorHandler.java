package org.sagebionetworks.grid.db.handler;

import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.OperationHandler;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.operation.NewVector;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class NewVectorHandler implements OperationHandler<NewVector> {

	private final GridIndexDao dao;

	public NewVectorHandler(GridIndexDao dao) {
		super();
		this.dao = dao;
	}

	@Override
	public OperationType getOperationType() {
		return OperationType.new_vec;
	}

	@Override
	public void handleBatch(String sessionId, Long replicaId, List<NewVector> batch) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicId");
		ValidateArgument.required(batch, "batch");
		dao.saveIndex(sessionId, replicaId, IndexType.vec,
				batch.stream().map(NewVector::getOperationId).collect(Collectors.toList()));
		dao.saveVectors(sessionId, replicaId,
				batch.stream().map(n -> new VectorNode().setId(n.getOperationId())).collect(Collectors.toList()));
	}

}
