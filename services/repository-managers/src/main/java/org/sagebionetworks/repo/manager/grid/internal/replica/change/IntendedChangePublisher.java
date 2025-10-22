package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.grid.GridConnectionInfo;

/**
 * Allows to automatically batch {@link IntendedChange}s into {@link IntendedChangeSet}s of optimal size.
 */
public class IntendedChangePublisher implements AutoCloseable {
	
	private static int getChangeSize(IntendedChange change) {
		return change.toJson().toString().getBytes(StandardCharsets.UTF_8).length + CHANGE_OVERHEAD_BYTES;
	}
	
	private static final int CHANGE_OVERHEAD_BYTES = 4; // Overhead for JSON array commas and brackets.

	private final GridConnectionInfo connInfo;
	private final Long maxClockSeq;
	private final PatchBuilderPublisher publisher;
	private final int maxChangeSetSize;
	
	private List<IntendedChange> currentChanges;
	private IntendedChangeSet currentChangeSet;
	private int currentSizeBytes;

	public IntendedChangePublisher(GridConnectionInfo connInfo, Long maxClockSeq, PatchBuilderPublisher publisher, int maxChangeSetSize) {
		this.connInfo = connInfo;
		this.maxClockSeq = maxClockSeq;
		this.publisher = publisher;
		this.maxChangeSetSize = maxChangeSetSize;
		this.resetCurrentChangeSet();
	}

	public void publish(IntendedChange change) {
		
		int changeSize = getChangeSize(change);

		if (currentSizeBytes + changeSize > this.maxChangeSetSize) {
			if (currentChanges.isEmpty()) {
				// A single change is too big to fit in a change set.
				throw new IllegalArgumentException("A single change cannot be larger than " + this.maxChangeSetSize + " bytes.");
			}
			// publish the current set
			publisher.sendChangesToPatchBuilder(currentChangeSet);
			// start a new set
			resetCurrentChangeSet();
		}

		currentChanges.add(change);
		currentSizeBytes += changeSize;
	}

	@Override
	public void close() throws Exception {
		if (currentChanges.isEmpty()) {
			return;
		}
		publisher.sendChangesToPatchBuilder(currentChangeSet);
	}

	private void resetCurrentChangeSet() {
		this.currentChanges = new ArrayList<>();
		this.currentChangeSet = new IntendedChangeSet()
			.setSessionId(connInfo.getSessionId())
			.setConnectionId(connInfo.getConnectionId())
			.setReplicaId(connInfo.getReplicaId())
			.setClockSequenceMaximum(maxClockSeq)
			.setChanges(currentChanges);
		this.currentSizeBytes = 0;
	}

}
