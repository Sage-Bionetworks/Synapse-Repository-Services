package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.PatchUtils;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.PatchInfo;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class GridReplicaPatchBuilderManagerImpl implements GridReplicaPatchBuilderManager {

	private static final Logger log = LogManager.getLogger(GridReplicaPatchBuilderManagerImpl.class);

	private final GridDao gridDao;
	private final GridIndexDao gridIndexDao;
	private final PatchPublisher patchPublisher;
	private final Map<IntendedChangeType, ChangeHandler<?>> handlers;

	public GridReplicaPatchBuilderManagerImpl(GridDao gridDao, GridIndexDao gridIndexDao, PatchPublisher patchPublisher,
			List<ChangeHandler<?>> handlers) {
		this.gridDao = gridDao;
		this.gridIndexDao = gridIndexDao;
		this.patchPublisher = patchPublisher;
		this.handlers = handlers.stream().collect(Collectors.toMap(ChangeHandler::getType, h -> h));
	}

	@Override
	public void buildPatch(IntendedChangeSet changeSet) throws IOException {
		validateChangeSet(changeSet);
		Optional<GridConnectionInfo> connectionOp = gridDao.getConnection(changeSet.getConnectionId());
		if (connectionOp.isEmpty()) {
			log.info("No connection found for: '{}' the message will be ignored", changeSet.getConnectionId());
			return;
		}

		/*
		 * The new IDs must be greater than everything this writer has observed (the header maximum of the replica it
		 * read, Lamport's receive rule) and greater than every ID it has already issued (its persisted high-water
		 * mark). The high-water mark lives in the main database because the index database starts empty on each new
		 * stack.
		 */
		Long highWater = gridDao.getReplicaSequenceHighWater(changeSet.getSessionId(), changeSet.getReplicaId())
				.orElse(0L);
		Long patchSequence = Math.max(changeSet.getClockSequenceMaximum(), highWater);
		LogicalTimestamp patchId = new LogicalTimestamp().setReplicaId(changeSet.getReplicaId())
				.setSequenceNumber(patchSequence);

		BufferedPatchPublisher buffer = createNewBufferedPatchPublisher();
		try (ChangePatchBuilder builder = createChangePatchBuilder(buffer, connectionOp.get(), patchId)) {
			processChanges(builder, changeSet.getChanges());
		}
		if (buffer.getTotalPatchSpan() < 1) {
			return;
		}
		// Reserve the range before any patch is published: a patch that never reaches the hub only leaves a gap.
		gridDao.setReplicaSequenceHighWater(changeSet.getSessionId(), changeSet.getReplicaId(),
				patchSequence + buffer.getTotalPatchSpan());
		buffer.publishAll(patchPublisher);
	}

	BufferedPatchPublisher createNewBufferedPatchPublisher() {
		return new BufferedPatchPublisher();
	}

	ChangePatchBuilder createChangePatchBuilder(PatchPublisher patchPublisher, GridConnectionInfo connection,
			LogicalTimestamp currentClock) {
		// disable constant caching due to PLFM-9192
		boolean useCaching = false;
		return new ChangePatchBuilder(patchPublisher, connection, currentClock, PatchUtils.MAX_BYTES_PER_PATCH);
	}

	void validateChangeSet(IntendedChangeSet changeSet) {
		ValidateArgument.required(changeSet, "changeset");
		ValidateArgument.required(changeSet.getSessionId(), "changeset.sessionId");
		ValidateArgument.required(changeSet.getReplicaId(), "changeset.replicaId");
		ValidateArgument.required(changeSet.getConnectionId(), "changeset.connectionId");
		ValidateArgument.required(changeSet.getClockSequenceMaximum(), "changeset.clockSequenceMaximum");
	}


	void processChanges(ChangePatchBuilder builder, List<IntendedChange> changes) {
		for (IntendedChange change : changes) {
			ChangeHandler<?> handler = handlers.get(change.getType());
			if (handler == null) {
				throw new IllegalArgumentException("No handler for: " + change.getType());
			}
			((ChangeHandler<IntendedChange>) handler).handleChange(builder, change);
		}
	}

	@Override
	public Optional<LogicalTimestamp> getCurrentClockIfAllPatchesApplied(String sessionId, Long replicaId) {
		List<PatchInfo> missingPatches = gridDao.listMissingPatchInfoForClock(sessionId, gridIndexDao.getClock(sessionId, replicaId), 1);
		if (!missingPatches.isEmpty()) {
			return Optional.empty();
		}
		Optional<Long> sequenceNumber = gridIndexDao.getClockSequenceNumber(sessionId, replicaId, replicaId);
		return Optional.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(sequenceNumber.orElse(0L)));
	}

	/**
	 * Holds the patches of a change set, so the total span is known before any of them is published.
	 */
	static class BufferedPatchPublisher implements PatchPublisher {

		record BufferedPatch(GridConnectionInfo connection, JSONArray patchBody, Long patchSpan) {
		}

		private final List<BufferedPatch> patches = new ArrayList<>();

		private long totalPatchSpan = 0L;

		@Override
		public void publishPatch(GridConnectionInfo connection, JSONArray patchBody, Long patchSpan) {
			patches.add(new BufferedPatch(connection, patchBody, patchSpan));
			totalPatchSpan += patchSpan;
		}

		public long getTotalPatchSpan() {
			return totalPatchSpan;
		}

		/**
		 * Publish the buffered patches, in the order they were built, to the given publisher.
		 */
		public void publishAll(PatchPublisher publisher) {
			patches.forEach(p -> publisher.publishPatch(p.connection(), p.patchBody(), p.patchSpan()));
		}

	}

}
