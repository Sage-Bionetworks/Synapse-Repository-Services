package org.sagebionetworks.grid.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObject;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;
import org.sagebionetworks.repo.model.grid.patch.operation.NewObject;
import org.sagebionetworks.repo.model.grid.patch.operation.NewVector;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

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

		dispatcher = new OperationDispatcherImpl(List.of(mockInsertObject, mockNewObjectHandler, mockNewVectorHandler));

		newVectorOne = new NewVector().setOperationId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L));
		newVectorTwo = new NewVector().setOperationId(new LogicalTimestamp().setReplicaId(31L).setSequenceNumber(4L));
		newObjectOne = new NewObject().setOperationId(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L));
		newObjectTwo = new NewObject().setOperationId(new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L));
		insertObjectOne = new InsertObject().setObjectId(newObjectOne.getOperationId());
		insertObjectTwo = new InsertObject().setObjectId(newObjectTwo.getOperationId());

		sessionId = "session123";
		replicaId = 99L;
	}

	@Test
	public void testProcessAll() {

		// call under test
		dispatcher.processAll(sessionId, replicaId,
				List.of(newObjectOne, insertObjectOne, newVectorTwo, newObjectTwo, insertObjectTwo, newVectorOne));

		InOrder inOrder = Mockito.inOrder(mockNewObjectHandler, mockNewVectorHandler, mockInsertObject);
		inOrder.verify(mockNewObjectHandler).handleBatch(sessionId, replicaId, List.of(newObjectOne, newObjectTwo));
		inOrder.verify(mockNewVectorHandler).handleBatch(sessionId, replicaId, List.of(newVectorTwo, newVectorOne));
		// insert objects must occur after the new objects
		inOrder.verify(mockInsertObject).handleBatch(sessionId, replicaId, List.of(insertObjectOne, insertObjectTwo));
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
			dispatcher.processAll(sessionId, replicaId, List.of(new NewConstant()));
		}).getMessage();
		assertEquals("Unknown type: new_con", message);
	}

}
