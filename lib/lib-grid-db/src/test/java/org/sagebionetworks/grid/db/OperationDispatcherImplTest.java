package org.sagebionetworks.grid.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObject;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;
import org.sagebionetworks.repo.model.grid.patch.operation.NewObject;
import org.sagebionetworks.repo.model.grid.patch.operation.NewVector;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OperationDispatcherImplTest {

	@Mock
	private OperationHandler<NewObject> mockNewObjectHandler;

	@Mock
	private OperationHandler<NewVector> mockNewVectorHandler;

	@Mock
	private OperationHandler<InsertObject> mockInsertObject;

	private OperationDispatcherImpl dispatcher;

	private String sessionId;
	private Long replicaId;

	private NewVector newVectorOne;
	private NewVector newVectorTwo;
	private NewObject newObjectOne;
	private NewObject newObjectTwo;
	private InsertObject insertObjectOne;
	private InsertObject insertObjectTwo;

	@BeforeEach
	void setUp() {

		when(mockNewObjectHandler.getOperationType()).thenReturn(OperationType.new_obj);
		when(mockNewVectorHandler.getOperationType()).thenReturn(OperationType.new_vec);
		when(mockInsertObject.getOperationType()).thenReturn(OperationType.ins_obj);

		dispatcher = Mockito.spy(
				new OperationDispatcherImpl(List.of(mockInsertObject, mockNewObjectHandler, mockNewVectorHandler)));

		newVectorOne = new NewVector(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L));
		newVectorTwo = new NewVector(new LogicalTimestamp().setReplicaId(31L).setSequenceNumber(4L));
		newObjectOne = new NewObject(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L));
		newObjectTwo = new NewObject(new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L));
		insertObjectOne = new InsertObject(
				new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(10L),
				newObjectOne.getOperationId(),
				Collections.singletonMap("foo", new LogicalTimestamp().setReplicaId(11L).setSequenceNumber(12L))
		);
		insertObjectTwo = new InsertObject(
				new LogicalTimestamp().setReplicaId(13L).setSequenceNumber(14L),
				newObjectTwo.getOperationId(),
				Collections.singletonMap("bar", new LogicalTimestamp().setReplicaId(15L).setSequenceNumber(16L))
		);

		sessionId = "session123";
		replicaId = 99L;
	}

	@Test
	public void testProcessAll() {

		when(mockNewVectorHandler.handleBatch(sessionId, replicaId, List.of(newVectorTwo, newVectorOne)))
				.thenReturn(Set.of(newVectorOne.getOperationId()));
		when(mockNewObjectHandler.handleBatch(sessionId, replicaId, List.of(newObjectOne, newObjectTwo)))
				.thenReturn(Set.of(newObjectTwo.getOperationId()));
		when(mockInsertObject.handleBatch(sessionId, replicaId, List.of(insertObjectOne, insertObjectTwo)))
				.thenReturn(Set.of(newObjectTwo.getOperationId()));

		// call under test
		Map<IndexType, Set<LogicalTimestamp>> changes = dispatcher.processAll(sessionId, replicaId,
				List.of(newObjectOne, insertObjectOne, newVectorTwo, newObjectTwo, insertObjectTwo, newVectorOne));
		Map<IndexType, Set<LogicalTimestamp>> expected = new LinkedHashMap<>();
		expected.put(IndexType.obj, Set.of(newObjectTwo.getOperationId()));
		expected.put(IndexType.vec, Set.of(newVectorOne.getOperationId()));
		assertEquals(expected, changes);

		InOrder inOrder = Mockito.inOrder(mockNewObjectHandler, mockNewVectorHandler, mockInsertObject);
		inOrder.verify(mockNewObjectHandler).handleBatch(sessionId, replicaId, List.of(newObjectOne, newObjectTwo));
		inOrder.verify(mockNewVectorHandler).handleBatch(sessionId, replicaId, List.of(newVectorTwo, newVectorOne));
		// insert objects must occur after the new objects
		inOrder.verify(mockInsertObject).handleBatch(sessionId, replicaId, List.of(insertObjectOne, insertObjectTwo));
	}

	@Test
	public void testProcessAllWithNullChanges() {
		doReturn(null).when(dispatcher).dispatchToHandler(any(), any(), any(), any());

		// call under test
		Map<IndexType, Set<LogicalTimestamp>> changes = dispatcher.processAll(sessionId, replicaId,
				List.of(newObjectOne, insertObjectOne, newVectorTwo, newObjectTwo, insertObjectTwo, newVectorOne));
		assertEquals(Map.of(), changes);

	}

	@Test
	public void testProcessAllWithEmptyChanges() {
		doReturn(Set.of()).when(dispatcher).dispatchToHandler(any(), any(), any(), any());

		// call under test
		Map<IndexType, Set<LogicalTimestamp>> changes = dispatcher.processAll(sessionId, replicaId,
				List.of(newObjectOne, insertObjectOne, newVectorTwo, newObjectTwo, insertObjectTwo, newVectorOne));
		assertEquals(Map.of(), changes);

	}

	@Test
	public void testProcessAllWithNullList() {

		String message = assertThrows(IllegalArgumentException.class, () -> {

			// call under test
			dispatcher.processAll(sessionId, replicaId, null);
		}).getMessage();
		assertEquals("operations is required.", message);
	}

	@Test
	public void testProcessAllWithNullSessionId() {
		sessionId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {

			// call under test
			dispatcher.processAll(sessionId, replicaId, List.of());
		}).getMessage();
		assertEquals("sessionId is required.", message);
	}

	@Test
	public void testProcessAllWithNullReplicaId() {
		replicaId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {

			// call under test
			dispatcher.processAll(sessionId, replicaId, List.of());
		}).getMessage();
		assertEquals("replicaId is required.", message);
	}

	@Test
	public void testProcessAllWithUnknownType() {

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			dispatcher.processAll(sessionId, replicaId, List.of(new NewConstant(new LogicalTimestamp().setReplicaId(17L).setSequenceNumber(18L), new ConValue(ConType.STRING, "foo" ))));
		}).getMessage();
		assertEquals("Unknown type: new_con", message);
	}

}
