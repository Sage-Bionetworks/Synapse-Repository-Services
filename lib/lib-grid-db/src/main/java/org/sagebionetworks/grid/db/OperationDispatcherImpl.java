package org.sagebionetworks.grid.db;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.Operation;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationDispatcherImpl implements OperationDispatcher {

	private final Map<OperationType, OperationHandler<?>> operationHandlers;

	public OperationDispatcherImpl(List<OperationHandler<?>> handlers) {
		operationHandlers = handlers.stream()
				.collect(Collectors.toMap(OperationHandler::getOperationType, Function.identity()));
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	@Override
	public Map<IndexType, Set<LogicalTimestamp>> processAll(String sessionId, Long replicaId, List<Operation> operations) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");
		ValidateArgument.required(operations, "operations");
		Map<OperationType, List<Operation>> batches = operations.stream()
				.collect(Collectors.groupingBy(Operation::getType));

		Map<IndexType, Set<LogicalTimestamp>> allChanges = new LinkedHashMap<>();
		/*
		 * By processing batches in the order defined by the enumeration, we can ensure
		 * that 'new' operations will be processed before 'insert' operations.
		 */
		for (OperationType type : OperationType.values()) {
			List<Operation> batch = batches.get(type);
			if (batch != null) {
				OperationHandler<?> handler = operationHandlers.get(type);
				if (handler == null) {
					throw new IllegalStateException("Unknown type: " + type);
				}
				Set<LogicalTimestamp> changes = dispatchToHandler(sessionId, replicaId, handler, batch);
				// Combine changes into allChanges map
				if (changes != null && !changes.isEmpty()) {
					IndexType indexType = type.getIndexType();
					if (indexType != null) { // Handle nop case which has null IndexType
						allChanges.computeIfAbsent(indexType, k -> new LinkedHashSet<>()).addAll(changes);
					}
				}
			}
		}
		return allChanges;
	}

	/**
	 * Compiler helper to match the provided batch to the handler's type.
	 * 
	 * @param <O>       The type is matched by the enumeration
	 * @param sessionId
	 * @param replicaId
	 * @param handler
	 * @param batch
	 */
	@SuppressWarnings("unchecked") <O extends Operation> Set<LogicalTimestamp> dispatchToHandler(String sessionId, Long replicaId, OperationHandler<O> handler, List<Operation> batch) {
		List<O> oBatch = batch.stream().map(op -> (O) op).collect(Collectors.toList());
		return handler.handleBatch(sessionId, replicaId, oBatch);
	}
}
