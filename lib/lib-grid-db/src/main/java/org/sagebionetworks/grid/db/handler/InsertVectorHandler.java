package org.sagebionetworks.grid.db.handler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.OperationHandler;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertVector;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.springframework.stereotype.Component;

@Component
public class InsertVectorHandler implements OperationHandler<InsertVector> {

	private final GridIndexDao gridDao;

	public InsertVectorHandler(GridIndexDao gridDao) {
		super();
		this.gridDao = gridDao;
	}

	@Override
	public OperationType getOperationType() {
		return OperationType.ins_vec;
	}

	@Override
	public Set<LogicalTimestamp> handleBatch(String sessionId, Long replicaId, List<InsertVector> batch) {
		Set<LogicalTimestamp> changeIds = new LinkedHashSet<>();

		// load the constants for each new values
		Map<LogicalTimestamp, ConstantNode> constants = gridDao
				.getConstants(sessionId, replicaId,
						batch.stream().flatMap(iv -> iv.getMap().values().stream()).distinct()
								.collect(Collectors.toList()))
				.stream().collect(Collectors.toMap(ConstantNode::getId, Function.identity()));

		// combine each insert with constants within its map.
		List<VectorNode> changes = batch.stream().map(i -> {
			VectorNode n = new VectorNode().setId(i.getVectorId()).setValues(new LinkedHashMap<>());
			for (Entry<Integer, LogicalTimestamp> e : i.getMap().entrySet()) {
				n.getValues().put(e.getKey(), constants.get(e.getValue()));
			}
			return n;
		}).collect(Collectors.toList());

		// load the current vectors for each change.
		Map<LogicalTimestamp, VectorNode> current = gridDao
				.getVectors(sessionId, replicaId,
						batch.stream().map(InsertVector::getVectorId).collect(Collectors.toList()))
				.stream().collect(Collectors.toMap(VectorNode::getId, Function.identity()));

		List<VectorNode> toChange = new ArrayList<>();
		changes.forEach(i -> {
			VectorNode cur = current.get(i.getId());
			if (cur == null) {
				throw new IllegalArgumentException("Cannot update a vector that does not exist: " + i.getId());
			}
			if (cur.attemptInsert(i)) {
				toChange.add(cur);
				changeIds.add(cur.getId());
			}
		});
		gridDao.saveVectors(sessionId, replicaId, toChange);
		return changeIds;
	}

}
