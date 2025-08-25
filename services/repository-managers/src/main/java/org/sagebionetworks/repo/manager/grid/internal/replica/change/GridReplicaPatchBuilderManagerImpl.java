package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.grid.db.ConstantProvider;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.PatchUtils;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

@Service
public class GridReplicaPatchBuilderManagerImpl implements GridReplicaPatchBuilderManager {

	private static final Logger log = LogManager.getLogger(GridReplicaPatchBuilderManagerImpl.class);

	private final GridDao gridDao;
	private final GridIndexDao gridIndexDao;
	private final ConstantProvider constantProvider;
	private final PatchPublisher patchPublisher;
	private final Map<IntendedChangeType, ChangeHandler<?>> handlers;

    public GridReplicaPatchBuilderManagerImpl(GridDao gridDao, GridIndexDao gridIndexDao, PatchPublisher patchPublisher,
                                              List<ChangeHandler<?>> handlers, ConstantProvider constantProvider) {
        this.gridDao = gridDao;
        this.gridIndexDao = gridIndexDao;
        this.constantProvider = constantProvider;
		this.patchPublisher = patchPublisher;
		this.handlers = handlers.stream().collect(Collectors.toMap(ChangeHandler::getType, h -> h));
	}

	@Override
	public void buildPatch(IntendedChangeSet changeSet) throws IOException {
		validateChangeSet(changeSet);
		Optional<GridSession> session = gridDao.getGridSession(changeSet.getSessionId());
		if (session.isEmpty()) {
			log.info("No session found for: '{}' the message will be ignored", changeSet.getSessionId());
			return;
		}

		Optional<LogicalTimestamp> currentClock = getCurrentClockIfAllPatchesApplied(changeSet.getSessionId(), changeSet.getReplicaId());
		if (currentClock.isEmpty()) {
			throw new RecoverableMessageException(
					"Waiting for outstanding patches to be applied before building new ones");
		}

		GridConnectionInfo connection = createConnectionInfo(changeSet);

		try (ChangePatchBuilder builder = createChangePatchBuilder(connection, currentClock.get())) {
			processChanges(builder, changeSet.getChanges());
		}
	}

	ChangePatchBuilder createChangePatchBuilder(GridConnectionInfo connection, LogicalTimestamp currentClock) {
		// disable constant caching due to PLFM-9192
		boolean useCaching = false;
		return new ChangePatchBuilder(patchPublisher, constantProvider, connection, currentClock,
				PatchUtils.MAX_BYTES_PER_PATCH,useCaching);
	}

	void validateChangeSet(IntendedChangeSet changeSet) {
		ValidateArgument.required(changeSet, "changeset");
		ValidateArgument.required(changeSet.getSessionId(), "changeset.sessionId");
		ValidateArgument.required(changeSet.getReplicaId(), "changeset.replicaId");
		ValidateArgument.required(changeSet.getConnectionId(), "changeset.connectionId");
	}

	GridConnectionInfo createConnectionInfo(IntendedChangeSet changeSet) {
		return new GridConnectionInfo().setConnectionId(changeSet.getConnectionId())
				.setSessionId(changeSet.getSessionId()).setReplicaId(changeSet.getReplicaId());
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
		List<LogicalTimestamp> missingPatches = gridDao.listMissingPatchIdsForClock(sessionId,
				gridIndexDao.getClock(sessionId, replicaId), 1);
		if (!missingPatches.isEmpty()) {
			return Optional.empty();
		}
		Optional<Long> sequenceNumber = gridIndexDao.getClockSequenceNumber(sessionId, replicaId, replicaId);
		return Optional.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(sequenceNumber.orElse(0L)));
	}

}
