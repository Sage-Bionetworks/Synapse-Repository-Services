package org.sagebionetworks.grid.db.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.OperationHandler;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertValue;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.springframework.stereotype.Repository;

@Repository
public class InsertValueHandler implements OperationHandler<InsertValue> {

	private final GridIndexDao gridDao;

	public InsertValueHandler(GridIndexDao gridDao) {
		super();
		this.gridDao = gridDao;
	}

	@Override
	public OperationType getOperationType() {
		return OperationType.ins_val;
	}

	@Override
	public void handleBatch(String sessionId, Long replicaId, List<InsertValue> batch) {
		Map<LogicalTimestamp, ValueNode> current = gridDao
				.getValues(sessionId, replicaId,
						batch.stream().map(InsertValue::getValueId).collect(Collectors.toList()))
				.stream().collect(Collectors.toMap(ValueNode::getId, Function.identity()));

		List<ValueNode> toChange = new ArrayList<>();
		batch.forEach(i -> {
			ValueNode cur = current.get(i.getValueId());
			if (cur == null) {
				throw new IllegalArgumentException("Cannot update a value that does not exist: " + i.getValueId());
			}
			if (cur.attemptInsert(i)) {
				toChange.add(cur);
			}
		});
		gridDao.saveValues(sessionId, replicaId, toChange);
	}

}
