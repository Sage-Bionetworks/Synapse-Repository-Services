package org.sagebionetworks.grid.db.handler;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.OperationHandler;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObject;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.springframework.stereotype.Component;

@Component
public class InsertObjectHandler implements OperationHandler<InsertObject> {

	private final GridIndexDao gridDao;

	public InsertObjectHandler(GridIndexDao gridDao) {
		super();
		this.gridDao = gridDao;
	}

	@Override
	public OperationType getOperationType() {
		return OperationType.ins_obj;
	}

	@Override
	public Set<LogicalTimestamp> handleBatch(String sessionId, Long replicaId, List<InsertObject> batch) {
		Set<LogicalTimestamp> changes = new LinkedHashSet<LogicalTimestamp>();
		Map<LogicalTimestamp, ObjectNode> current = gridDao
				.getObjects(sessionId, replicaId,
						batch.stream().map(InsertObject::getObjectId).collect(Collectors.toList()))
				.stream().collect(Collectors.toMap(ObjectNode::getId, Function.identity()));

		List<ObjectNode> toChange = new ArrayList<>();
		batch.forEach(i -> {
			ObjectNode cur = current.get(i.getObjectId());
			if (cur == null) {
				throw new IllegalArgumentException("Cannot update an object that does not exist: " + i.getObjectId());
			}
			if (cur.attemptInsert(i)) {
				toChange.add(cur);
				changes.add(cur.getId());
			}
		});
		gridDao.saveObjects(sessionId, replicaId, toChange);
		return changes;
	}

}
