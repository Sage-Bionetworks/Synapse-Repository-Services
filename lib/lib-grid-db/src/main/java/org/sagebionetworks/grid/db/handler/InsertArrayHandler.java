package org.sagebionetworks.grid.db.handler;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.OperationHandler;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertArray;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.springframework.stereotype.Component;

/**
 * This handler follows the <a href=
 * "https://jsonjoy.com/specs/json-crdt/model-document/crdt-algorithms#RGA-Insertion-Routine">RGA
 * Insertion Routine</a>.
 */
@Component
public class InsertArrayHandler implements OperationHandler<InsertArray> {

	private final GridIndexDao gridDao;

	public InsertArrayHandler(GridIndexDao gridDao) {
		super();
		this.gridDao = gridDao;
	}

	@Override
	public OperationType getOperationType() {
		return OperationType.ins_arr;
	}

	@Override
	public Set<LogicalTimestamp> handleBatch(String sessionId, Long replicaId, List<InsertArray> batch) {
		List<ArrayNode> flatBatch = expandInsertArrays(batch);
		Set<LogicalTimestamp> changes = new LinkedHashSet<LogicalTimestamp>();
		flatBatch.forEach(a -> {
			/*
			 * Find the location in the RGA that this node should be inserted following the
			 * RGA algorithm.
			 */
			gridDao.findArrayInsertLocation(sessionId, replicaId, a).ifPresent(r -> {
				a.setReferenceNodeId(r);
				gridDao.insertIntoArray(sessionId, replicaId, a);
				changes.add(a.getId());
			});
		});
		return changes;
	}

	/**
	 * An InsertArray can contain more than one elementId. This is an optimization
	 * that allows multiple elements to be inserted into the array at the same
	 * position. However, if we later need to insert a value between two of these
	 * elements, then we would need to first split the ArrayNode into multiple nodes
	 * before executing the new insert. To avoid the complexity of dynamically
	 * splitting nodes, we automatically add a node for each element at the start.
	 * 
	 * @param batch
	 * @return
	 */
	public List<ArrayNode> expandInsertArrays(List<InsertArray> batch) {
		return batch.stream().flatMap(insert -> {
			LogicalTimestamp referenceId = insert.getReferenceId();
			List<LogicalTimestamp> elementIds = insert.getElementIds();
			List<ArrayNode> nodes = new ArrayList<>(elementIds.size());
			for (int i = 0; i < elementIds.size(); i++) {
				LogicalTimestamp nodeId = LogicalTimestamp.newIncrement(insert.getOperationId(), i);
				ArrayNode node = new ArrayNode().setNodeId(nodeId).setArrayId(insert.getArrayId())
						.setReferenceNodeId(referenceId).setDataId(elementIds.get(i));
				nodes.add(node);
				// the next node will reference this node.
				referenceId = nodeId;
			}
			return nodes.stream();
		}).collect(Collectors.toList());
	}

}
