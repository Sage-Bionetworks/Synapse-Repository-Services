package org.sagebionetworks.grid.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.encoding.SnapshotFileIndex;
import org.sagebionetworks.repo.model.grid.encoding.SnapshotFileIndexBuilder;
import org.sagebionetworks.repo.model.grid.encoding.SnapshotFileIndex.NodePointer;
import org.sagebionetworks.repo.model.grid.encoding.IndexedNodeCodecMapper;
import org.sagebionetworks.repo.model.grid.encoding.SeekingNodeReader;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.RGANode;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
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

	@Mock
	private SnapshotFileIndexBuilder mockIndexBuilder;

	@Mock
	private SeekingNodeReaderProvider mockReaderProvider;

	@Mock
	private SnapshotFileIndex mockIndex;

	@Mock
	private SeekingNodeReader mockReader;

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

	@Test
	public void testApplySnapshotWithNullSessionId() {
		Path snapshotFile = Path.of("/tmp/snapshot.cbor");
		sessionId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.applySnapshot(sessionId, replicaId, snapshotFile);
		}).getMessage();
		assertEquals("sessionId is required.", message);
		verifyZeroInteractions(mockDao);
	}

	@Test
	public void testApplySnapshotWithNullReplicaId() {
		Path snapshotFile = Path.of("/tmp/snapshot.cbor");
		replicaId = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.applySnapshot(sessionId, replicaId, snapshotFile);
		}).getMessage();
		assertEquals("replicaId is required.", message);
		verifyZeroInteractions(mockDao);
	}

	@Test
	public void testApplySnapshotWithNullFile() {
		Path snapshotFile = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.applySnapshot(sessionId, replicaId, null);
		}).getMessage();
		assertEquals("snapshotFile is required.", message);
		verifyZeroInteractions(mockDao);
	}

	@Test
	public void testApplySnapshotWithAllNodeTypes() throws Exception {
		Path snapshotFile = Path.of("/tmp/snapshot.cbor");
		LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
		LogicalTimestamp constId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L);
		LogicalTimestamp objectId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(3L);
		LogicalTimestamp arrayId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(4L);
		LogicalTimestamp arrayElementId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(5L);
		LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(6L);
		LogicalTimestamp vectorConstId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(7L);
		ClockTable clockTable = new ClockTable(List.of(rootId));

		// Create nodes
		ConstantNode constantNode = new ConstantNode().setId(constId).setValue(new ConValue(ConType.LONG, 42L));
		ConstantNode vectorConstNode = new ConstantNode().setId(vectorConstId).setValue(new ConValue(ConType.STRING, "vec"));
		ObjectNode objectNode = new ObjectNode().setId(objectId).setValue(Map.of("key", constId));
		ValueNode valueNode = new ValueNode().setId(rootId).setValue(objectId);
		RGANode rgaElement = new RGANode()
				.setContainerId(arrayId)
				.setNodeId(arrayElementId)
				.setDataId(constId)
				.setIsDeleted(false);
		ArrayNode arrayNode = new ArrayNode().setId(arrayId).setElements(List.of(rgaElement));
		ConstantNode vectorConstStub = new ConstantNode().setId(vectorConstId);
		VectorNode vectorNode = new VectorNode().setId(vectorId).setValues(Map.of(0, vectorConstStub));

		// Create entries
		Map<LogicalTimestamp, NodePointer> constantEntries = new LinkedHashMap<>();
		constantEntries.put(constId, new NodePointer(100L, 50));
		constantEntries.put(vectorConstId, new NodePointer(150L, 50));

		when(mockIndexBuilder.build(snapshotFile)).thenReturn(mockIndex);
		when(mockIndex.getClockTable()).thenReturn(clockTable);
		when(mockIndex.getRootNodeId()).thenReturn(rootId);
		when(mockReaderProvider.create(snapshotFile, mockIndex)).thenReturn(mockReader);

		// readNodes is called once per type that has entries (constants, objects, values, arrays, vectors)
		when(mockReader.streamConstantNodes()).thenReturn(Stream.of(constantNode, vectorConstNode)); // constants
		when(mockReader.streamObjectNodes()).thenReturn(Stream.of(objectNode));                    	// objects
		when(mockReader.streamValueNodes()).thenReturn(Stream.of(valueNode));                      	// values
		when(mockReader.streamArrayNodes()).thenReturn(Stream.of(arrayNode));                    	// arrays
		when(mockReader.streamVectorNodes()).thenReturn(Stream.of(vectorNode));                   	// vectors
		when(mockReader.readNode(eq(IndexedNodeCodecMapper.CONSTANT), eq(vectorConstId))).thenReturn(vectorConstNode);
		when(mockDao.createReplicaIfNotExists(sessionId, replicaId)).thenReturn(true);

		// call under test
		manager.applySnapshot(sessionId, replicaId, snapshotFile);

		// Verify delete and re-create replica
		verify(mockDao).deleteReplica(sessionId, replicaId);
		verify(mockDao).createReplicaIfNotExists(sessionId, replicaId);
		verify(mockDao).saveIndex(sessionId, replicaId, IndexType.val, List.of(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L)));
		verify(mockDao).saveValues(sessionId, replicaId, List.of(new ValueNode().setId(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L))));

		// Verify replica setup
		verify(mockDao).deleteReplica(sessionId, replicaId);
		verify(mockDao).createReplicaIfNotExists(sessionId, replicaId);

		// Verify constants
		verify(mockDao).saveIndex(sessionId, replicaId, IndexType.con, new ArrayList<>(constantEntries.keySet()));
		verify(mockDao).saveNewConstants(sessionId, replicaId, List.of(constantNode, vectorConstNode));

		// Verify objects
		verify(mockDao).saveIndex(sessionId, replicaId, IndexType.obj, List.of(objectId));
		verify(mockDao).saveObjects(sessionId, replicaId, List.of(objectNode));

		// Verify values
		verify(mockDao).saveIndex(sessionId, replicaId, IndexType.val, List.of(rootId));
		verify(mockDao).saveValues(sessionId, replicaId, List.of(valueNode));

		// Verify arrays
		verify(mockDao).saveIndex(sessionId, replicaId, IndexType.arr, List.of(arrayId));
		verify(mockDao).createArrayBatch(sessionId, replicaId, List.of(arrayId));
		verify(mockDao).batchInsertRgaNodes(sessionId, replicaId, List.of(rgaElement));

		// Verify vectors
		verify(mockDao).saveIndex(sessionId, replicaId, IndexType.vec, List.of(vectorId));
		verify(mockDao).saveVectors(eq(sessionId), eq(replicaId), eq(List.of(new VectorNode().setId(vectorId).setValues(Map.of(0, vectorConstNode)))));

		// Verify update the root value node
		verify(mockDao).saveValues(sessionId, replicaId,
				List.of(new ValueNode().setId(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L)).setValue(rootId))
		);

		// Verify clocks
		verify(mockDao).setClocks(sessionId, replicaId, clockTable.getClocks());
		verify(mockReader).close();

		verifyNoMoreInteractions(mockDao, mockIndexBuilder, mockReaderProvider, mockReader);
	}

	@Test
	public void testImportSnapshotWithDecoderBuildFailure() throws Exception {
		Path snapshotFile = Path.of("/tmp/snapshot.cbor");

		when(mockIndexBuilder.build(snapshotFile)).thenThrow(new IOException("Failed to read file"));
		when(mockDao.createReplicaIfNotExists(sessionId, replicaId)).thenReturn(true);

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			manager.applySnapshot(sessionId, replicaId, snapshotFile);
		});
		assertTrue(ex.getMessage().contains("Failed to build snapshot index"));
		assertTrue(ex.getCause() instanceof IOException);
	}

	@Test
	public void testImportSnapshotWithReaderCreateFailure() throws Exception {
		Path snapshotFile = Path.of("/tmp/snapshot.cbor");
		LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
		ClockTable clockTable = new ClockTable(List.of(rootId));

		when(mockIndexBuilder.build(snapshotFile)).thenReturn(mockIndex);
		when(mockIndex.getClockTable()).thenReturn(clockTable);
		when(mockReaderProvider.create(snapshotFile, mockIndex)).thenThrow(new IOException("Failed to open file"));
		when(mockDao.createReplicaIfNotExists(sessionId, replicaId)).thenReturn(true);

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			manager.applySnapshot(sessionId, replicaId, snapshotFile);
		});
		assertTrue(ex.getMessage().contains("Failed to import snapshot"));
		assertTrue(ex.getCause() instanceof IOException);
	}

}
