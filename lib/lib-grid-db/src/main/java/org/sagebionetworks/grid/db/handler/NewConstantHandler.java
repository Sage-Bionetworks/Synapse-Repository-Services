package org.sagebionetworks.grid.db.handler;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.OperationHandler;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Component;

@Component
public class NewConstantHandler implements OperationHandler<NewConstant> {

	private final GridIndexDao dao;

	public NewConstantHandler(GridIndexDao dao) {
		super();
		this.dao = dao;
	}

	@Override
	public OperationType getOperationType() {
		return OperationType.new_con;
	}

	@Override
	public Set<LogicalTimestamp> handleBatch(String sessionId, Long replicaId, List<NewConstant> batch) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicId");
		List<LogicalTimestamp> conIds = batch.stream().map(NewConstant::getOperationId).collect(Collectors.toList());
		dao.saveIndex(sessionId, replicaId, IndexType.con, conIds);
		dao.saveNewConstants(sessionId, replicaId,
				batch.stream().map(c -> new ConstantNode().setId(c.getOperationId()).setValue(c.getValue().getValue()))
						.collect(Collectors.toList()));
		return new LinkedHashSet<>(conIds);
	}

}
