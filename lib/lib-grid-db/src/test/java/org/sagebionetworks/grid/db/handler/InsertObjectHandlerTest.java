package org.sagebionetworks.grid.db.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObject;

@ExtendWith(MockitoExtension.class)
public class InsertObjectHandlerTest {
	@Mock
	private GridIndexDao mockDao;

	@InjectMocks
	private InsertObjectHandler handler;

	private String sessionId;
	private Long replicaId;

	private List<InsertObject> inserts;

	@BeforeEach
	public void before() {
		sessionId = "sessionOne";
		replicaId = 123L;
		inserts = List.of(
				new InsertObject(
						new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
						new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L),
						Map.of("one", new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L))
				),
				new InsertObject(
						new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L),
						new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(10L),
						Map.of("two", new LogicalTimestamp().setReplicaId(11L).setSequenceNumber(12L))
				)
		);
	}

	@Test
	public void testHandleBatch() {

		when(mockDao.getObjects(sessionId, replicaId,
				inserts.stream().map(InsertObject::getObjectId).collect(Collectors.toList())))
				.thenReturn(List.of(new ObjectNode().setId(inserts.get(0).getObjectId()),
						new ObjectNode().setId(inserts.get(1).getObjectId()).setValueFromJson("{\"two\":[11,12]}")));

		// call under test
		Set<LogicalTimestamp> changes = handler.handleBatch(sessionId, replicaId, inserts);
		assertEquals(Set.of(inserts.get(0).getObjectId()), changes);
		// only the first items should be saved as the second is already set.
		verify(mockDao).saveObjects(sessionId, replicaId,
				List.of(new ObjectNode().setId(inserts.get(0).getObjectId()).setValueFromJson("{\"one\":[5,6]}")));
	}

	@Test
	public void testHandleBatchWithObjectDoesNotExist() {

		when(mockDao.getObjects(sessionId, replicaId,
				inserts.stream().map(InsertObject::getObjectId).collect(Collectors.toList())))
				.thenReturn(List.of(new ObjectNode().setId(inserts.get(0).getObjectId())));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.handleBatch(sessionId, replicaId, inserts);
		}).getMessage();
		assertEquals("Cannot update an object that does not exist: LogicalTimestamp [replicaId=9, sequenceNumber=10]",
				message);

		verify(mockDao, never()).saveObjects(any(), any(), any());
	}
}
