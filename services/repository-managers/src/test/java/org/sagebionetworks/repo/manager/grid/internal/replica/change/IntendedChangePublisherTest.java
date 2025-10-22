package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;

@ExtendWith(MockitoExtension.class)
public class IntendedChangePublisherTest {

	@Mock
	private PatchBuilderPublisher mockPatchBuilderPublisher;

	private String sessionId = "sessionId";
	private Long replicaId = 1L;
	private String connectionId = "connectionId";

	private GridConnectionInfo connInfo;

	private Long maxClock;

	@BeforeEach
	public void beforeEach() {
		connInfo = new GridConnectionInfo()
			.setSessionId(sessionId)
			.setReplicaId(replicaId)
			.setConnectionId(connectionId);
		
		maxClock = 100L;
	}
	
	@Test
	public void testPublish() throws Exception {
		int count = 2;
		
		List<IntendedChange> changes = new ArrayList<>(count);
		
		for (int i = 0; i < count; i++) {
			IntendedChange change = Mockito.mock(IntendedChange.class);
			changes.add(change);
			// {"i":i} + 4 bytes of overhead = 11 bytes per change
			when(change.toJson()).thenReturn(new JSONObject().put("i", i));
		}
		
		// Fits perfectly in one change set
		int maxChangeSetSize = 11 * 2;
		
		try (IntendedChangePublisher publisher = new IntendedChangePublisher(connInfo, maxClock, mockPatchBuilderPublisher, maxChangeSetSize)) {
			changes.forEach(publisher::publish);
		}

		verify(mockPatchBuilderPublisher).sendChangesToPatchBuilder(
			new IntendedChangeSet()
				.setChanges(changes)
				.setClockSequenceMaximum(maxClock)
				.setSessionId(sessionId)
				.setReplicaId(replicaId)
				.setConnectionId(connectionId)
		);
	}
	
	@Test
	public void testPublishMultipleChangeSet() throws Exception {
		
		int count = 5;
		
		List<IntendedChange> changes = new ArrayList<>(count);
		
		for (int i = 0; i < count; i++) {
			IntendedChange change = Mockito.mock(IntendedChange.class);
			changes.add(change);
			// {"i":i} + 4 bytes of overhead = 11 bytes per change
			when(change.toJson()).thenReturn(new JSONObject().put("i", i));
		}
		
		int maxChangeSetSize = 11 * 3; // 3 changes per set
		
		try (IntendedChangePublisher publisher = new IntendedChangePublisher(connInfo, maxClock, mockPatchBuilderPublisher, maxChangeSetSize)) {
			changes.forEach(publisher::publish);
		}

		verify(mockPatchBuilderPublisher).sendChangesToPatchBuilder(
			new IntendedChangeSet()
				.setChanges(changes.subList(0, 3))
				.setClockSequenceMaximum(maxClock)
				.setSessionId(sessionId)
				.setReplicaId(replicaId)
				.setConnectionId(connectionId)
		);
		
		verify(mockPatchBuilderPublisher).sendChangesToPatchBuilder(
			new IntendedChangeSet()
				.setChanges(changes.subList(3, 5))
				.setClockSequenceMaximum(maxClock)
				.setSessionId(sessionId)
				.setReplicaId(replicaId)
				.setConnectionId(connectionId)
		);
	}
	
	@Test
	public void testPublishWithChangeTooBig() throws Exception {
		
		IntendedChange change = Mockito.mock(IntendedChange.class);
		
		// {"i":1} + 4 bytes of overhead = 11 bytes per change
		when(change.toJson()).thenReturn(new JSONObject().put("i", 1));
		
		int maxChangeSetSize = 10; // Does not fit
		
		try (IntendedChangePublisher publisher = new IntendedChangePublisher(connInfo, maxClock, mockPatchBuilderPublisher, maxChangeSetSize)) {
			assertEquals("A single change cannot be larger than " + maxChangeSetSize + " bytes.", assertThrows(IllegalArgumentException.class, () -> {
				publisher.publish(change);
			}).getMessage());
		}
	}
}
