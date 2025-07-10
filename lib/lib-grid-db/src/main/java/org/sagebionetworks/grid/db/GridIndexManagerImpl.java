package org.sagebionetworks.grid.db;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, transactionManager = "gridTransactionManager")
public class GridIndexManagerImpl implements GridIndexManager {

	private static final Logger log = LogManager.getLogger(GridIndexManagerImpl.class);

	private final GridIndexDao dao;
	private final OperationDispatcher operationDispatcher;

	public GridIndexManagerImpl(GridIndexDao dao, OperationDispatcher operationDispatcher) {
		super();
		this.dao = dao;
		this.operationDispatcher = operationDispatcher;
	}

	@Transactional(readOnly = false)
	@Override
	public void applyPatch(String sessionId, Long replicaId, Patch patch) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");
		ValidateArgument.required(patch, "patch");
		ValidateArgument.required(patch.getOperations(), "patch.operations");

		if (dao.createReplicaIfNotExists(sessionId, replicaId)) {
			// this is the first patch of a replica.
			LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L);
			// create the root value of the document.
			dao.saveIndex(sessionId, replicaId, IndexType.val, List.of(rootId));
			dao.saveValues(sessionId, replicaId, List.of(new ValueNode().setId(rootId)));
		}

		if (isPatchAlreadyApplied(sessionId, replicaId, patch.getPatchId())) {
			log.info("Patch: {}.{} has already been applied to session: {} replica: {}",
					patch.getPatchId().getReplicaId(), patch.getPatchId().getSequenceNumber(), sessionId, replicaId);
			return;
		}

		// Operations are batched and processed by type.
		operationDispatcher.processAll(sessionId, replicaId, patch.getOperations());

		LogicalTimestamp patchClock = LogicalTimestamp.newIncrement(patch.getPatchId(), patch.getSpan());

		// unconditionally increment this replica's clock by the new sequence.
		dao.setClock(sessionId, replicaId,
				new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(patchClock.getSequenceNumber()));
		if (patchClock.getReplicaId() != replicaId) {
			// The patch is from another replica, so add it to this replica's clock.
			dao.setClock(sessionId, replicaId, patchClock);
		}
	}

	/**
	 * Has the given patch already been applied to this replica?
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param patchId
	 * @return
	 */
	boolean isPatchAlreadyApplied(String sessionId, Long replicaId, LogicalTimestamp patchId) {
		return dao.getClockSequenceNumber(sessionId, replicaId, patchId.getReplicaId())
				.map(seq -> patchId.getSequenceNumber() <= seq).orElse(false);
	}

}
