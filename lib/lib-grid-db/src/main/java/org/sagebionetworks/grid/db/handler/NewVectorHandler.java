package org.sagebionetworks.grid.db.handler;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.OperationHandler;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewVector;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Component;

@Component
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
	public Set<LogicalTimestamp> handleBatch(String sessionId, Long replicaId, List<NewVector> batch) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicId");
		ValidateArgument.required(batch, "batch");
		List<LogicalTimestamp> vecIds = batch.stream().map(NewVector::getOperationId).collect(Collectors.toList());
		dao.saveIndex(sessionId, replicaId, IndexType.vec, vecIds);
		dao.saveVectors(sessionId, replicaId,
				batch.stream().map(n -> new VectorNode().setId(n.getOperationId())).collect(Collectors.toList()));
		return new LinkedHashSet<>(vecIds);
	}

}
