package org.sagebionetworks.grid.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;
import org.sagebionetworks.repo.model.grid.patch.operation.NewVector;

@ExtendWith(MockitoExtension.class)
public class GridIndexManagerImplTest {
	@Mock
	private GridIndexDao mockDao;

	@Mock
	private OperationDispatcher mockOperationDispatcher;

	@Spy
	@InjectMocks
	private GridIndexManagerImpl manager;

	private String sessionId;
	private Long replicaId;
	private Patch patch;
	private NewConstant newConstant;
	private NewVector newVector;
	private LogicalTimestamp rootValueId;

	@BeforeEach
	public void before() {
		sessionId = "sessionOne";
		replicaId = 123L;
		newConstant = new NewConstant(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
				new ConValue(ConType.BOOLEAN, true));
		newVector = new NewVector(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L));
		patch = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L))
				.setOperations(List.of(newConstant, newVector));
		rootValueId = new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L);
	}

	@Test
	public void testApplyPatch() {
		doReturn(false).when(manager).isPatchAlreadyApplied(sessionId, replicaId, patch.getPatchId());
		when(mockDao.createReplicaIfNotExists(sessionId, replicaId)).thenReturn(true);
		Map<IndexType, Set<LogicalTimestamp>> expected = Map.of(IndexType.con,
				Set.of(new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L)));
		when(mockOperationDispatcher.processAll(sessionId, replicaId, patch.getOperations())).thenReturn(expected);

		// call under test
		Map<IndexType, Set<LogicalTimestamp>> changes = manager.applyPatch(sessionId, replicaId, patch);
		assertEquals(expected, changes);
		verify(mockDao).createReplicaIfNotExists(sessionId, replicaId);
		verify(mockDao).saveIndex(sessionId, replicaId, IndexType.val, List.of(rootValueId));
		verify(mockDao).saveValues(sessionId, replicaId, List.of(new ValueNode().setId(rootValueId)));
		verify(mockOperationDispatcher).processAll(sessionId, replicaId, patch.getOperations());
		verify(mockDao).setClock(sessionId, replicaId, new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(8L));
	}

	@Test
	public void testApplyPatchWithNotFirst() {
		doReturn(false).when(manager).isPatchAlreadyApplied(sessionId, replicaId, patch.getPatchId());
		when(mockDao.createReplicaIfNotExists(sessionId, replicaId)).thenReturn(false);
		// call under test
		manager.applyPatch(sessionId, replicaId, patch);
		verify(mockDao).createReplicaIfNotExists(sessionId, replicaId);
		verify(mockDao, never()).saveIndex(sessionId, replicaId, IndexType.val, List.of(rootValueId));
		verify(mockDao, never()).saveValues(sessionId, replicaId, List.of(new ValueNode().setId(rootValueId)));
		verify(mockOperationDispatcher).processAll(sessionId, replicaId, patch.getOperations());
		verify(mockDao).setClock(sessionId, replicaId, new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(8L));
	}

	@Test
	public void testApplyPatchWithPatchFromThisReplica() {
		this.patch.getPatchId().setReplicaId(replicaId);
		doReturn(false).when(manager).isPatchAlreadyApplied(sessionId, replicaId, patch.getPatchId());
		when(mockDao.createReplicaIfNotExists(sessionId, replicaId)).thenReturn(true);
		// call under test
		manager.applyPatch(sessionId, replicaId, patch);
		verify(mockDao).createReplicaIfNotExists(sessionId, replicaId);
		verify(mockDao).saveIndex(sessionId, replicaId, IndexType.val, List.of(rootValueId));
		verify(mockDao).saveValues(sessionId, replicaId, List.of(new ValueNode().setId(rootValueId)));
		verify(mockOperationDispatcher).processAll(sessionId, replicaId, patch.getOperations());
		// only need one call to set clock for this case.
		verify(mockDao, times(1)).setClock(any(), any(), any());
		verify(mockDao).setClock(sessionId, replicaId,
				new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(8L));
	}

	@Test
	public void testApplyPatchWithAlreadyApplied() {
		doReturn(true).when(manager).isPatchAlreadyApplied(sessionId, replicaId, patch.getPatchId());
		when(mockDao.createReplicaIfNotExists(sessionId, replicaId)).thenReturn(false);
		// call under test
		Map<IndexType, Set<LogicalTimestamp>> changes = manager.applyPatch(sessionId, replicaId, patch);
		assertEquals(Collections.emptyMap(), changes);
		verifyZeroInteractions(mockOperationDispatcher);
		verify(mockDao).createReplicaIfNotExists(sessionId, replicaId);
		verify(mockDao, never()).setClock(any(), any(), any());
	}

	@Test
	public void testApplyPatchWithNullSessionId() {
		sessionId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.applyPatch(sessionId, replicaId, patch);
		}).getMessage();
		assertEquals("sessionId is required.", message);
		verifyZeroInteractions(mockOperationDispatcher, mockDao);
	}

	@Test
	public void testApplyPatchWithNullReplicaId() {
		replicaId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.applyPatch(sessionId, replicaId, patch);
		}).getMessage();
		assertEquals("replicaId is required.", message);
		verifyZeroInteractions(mockOperationDispatcher, mockDao);
	}

	@Test
	public void testApplyPatchWithNullPatch() {
		patch = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.applyPatch(sessionId, replicaId, patch);
		}).getMessage();
		assertEquals("patch is required.", message);
		verifyZeroInteractions(mockOperationDispatcher, mockDao);
	}

	@Test
	public void testApplyPatchWithNullPatchOperations() {
		patch.setOperations(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.applyPatch(sessionId, replicaId, patch);
		}).getMessage();
		assertEquals("patch.operations is required.", message);
		verifyZeroInteractions(mockOperationDispatcher, mockDao);
	}

	@Test
	public void testIsPatchAlreadyApplied() {
		when(mockDao.getClockSequenceNumber(sessionId, replicaId, patch.getPatchId().getReplicaId()))
				.thenReturn(Optional.empty());

		// call under test
		assertFalse(manager.isPatchAlreadyApplied(sessionId, replicaId, patch.getPatchId()));
	}

	@Test
	public void testIsPatchAlreadyAppliedWithSameRepica() {
		Long patchSequence = patch.getPatchId().getSequenceNumber();
		when(mockDao.getClockSequenceNumber(sessionId, replicaId, patch.getPatchId().getReplicaId()))
				.thenReturn(Optional.of(patchSequence));

		// call under test
		assertFalse(manager.isPatchAlreadyApplied(sessionId, replicaId, patch.getPatchId()));
	}

	@Test
	public void testIsPatchAlreadyAppliedWithOlderReplica() {
		Long patchSequence = patch.getPatchId().getSequenceNumber();
		when(mockDao.getClockSequenceNumber(sessionId, replicaId, patch.getPatchId().getReplicaId()))
				.thenReturn(Optional.of(patchSequence - 1L));

		// call under test
		assertFalse(manager.isPatchAlreadyApplied(sessionId, replicaId, patch.getPatchId()));
	}

	@Test
	public void testIsPatchAlreadyAppliedWithNewerReplica() {
		Long patchSequence = patch.getPatchId().getSequenceNumber();
		when(mockDao.getClockSequenceNumber(sessionId, replicaId, patch.getPatchId().getReplicaId()))
				.thenReturn(Optional.of(patchSequence + 1L));

		// call under test
		assertTrue(manager.isPatchAlreadyApplied(sessionId, replicaId, patch.getPatchId()));
	}

	@Test
	public void testStartMessageChain() {
		int nextId = 101;
		String methodName = "some-method";
		when(mockDao.createNextMessageId(sessionId, replicaId, GridIndexManagerImpl.MAX_MESSAGE_ID)).thenReturn(nextId);
		MessageChain chain = new MessageChain().setId(nextId).setSessionId(sessionId).setReplicaId(replicaId)
				.setMethod(methodName);
		when(mockDao.createMessageChain(chain, GridIndexManagerImpl.MAX_MESSAGE_DURATION)).thenReturn(chain);
		// call under test
		assertEquals(chain, manager.startMessageChain(sessionId, replicaId, methodName));
	}
	
	@Test
	public void testRefreshMessageChain() {
		int chainId = 111;
		when(mockDao.refreshMessageChain(sessionId, replicaId, chainId, GridIndexManagerImpl.MAX_MESSAGE_DURATION))
				.thenReturn(true);
		// call under test
		assertTrue(manager.refreshMessageChain(sessionId, replicaId, chainId));
	}

}
