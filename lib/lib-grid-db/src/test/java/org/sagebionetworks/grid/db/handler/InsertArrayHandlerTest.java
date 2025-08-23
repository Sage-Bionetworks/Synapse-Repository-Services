package org.sagebionetworks.grid.db.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.LogicalTimestampTestHelper;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertArray;

@ExtendWith(MockitoExtension.class)
public class InsertArrayHandlerTest {

	@Mock
	private GridIndexDao mockDao;

	@Spy
	@InjectMocks
	private InsertArrayHandler handler;

	private String sessionId;
	private Long replicaId;
	private List<LogicalTimestamp> ids;
	private List<InsertArray> batch;

	@BeforeEach
	public void before() {
		sessionId = "sessionOne";
		replicaId = 123L;
		ids = LogicalTimestampTestHelper.createIds(7);
		batch = List.of(
				// one
				new InsertArray(
						new LogicalTimestamp().setReplicaId(99L).setSequenceNumber(1L),
						ids.get(0),
						ids.get(0),
						List.of(ids.get(1), ids.get(2))
				),
				// two
				new InsertArray(
						new LogicalTimestamp().setReplicaId(101L).setSequenceNumber(1L),
						ids.get(3),
						ids.get(4),
						List.of(ids.get(5), ids.get(6))
				)
		);
	}

	@Test
	public void testExpandInsertArrays() {

		// call under test
		List<ArrayNode> results = handler.expandInsertArrays(batch);
		List<ArrayNode> expected = List.of(
				//
				new ArrayNode().setNodeId(new LogicalTimestamp().setReplicaId(99L).setSequenceNumber(1L))
						.setArrayId(ids.get(0)).setDataId(ids.get(1)).setReferenceNodeId(ids.get(0)),
				//
				new ArrayNode().setNodeId(new LogicalTimestamp().setReplicaId(99L).setSequenceNumber(2L))
						.setArrayId(ids.get(0)).setDataId(ids.get(2))
						.setReferenceNodeId(new LogicalTimestamp().setReplicaId(99L).setSequenceNumber(1L)),
				//
				new ArrayNode().setNodeId(new LogicalTimestamp().setReplicaId(101L).setSequenceNumber(1L))
						.setArrayId(ids.get(3)).setDataId(ids.get(5)).setReferenceNodeId(ids.get(4)),
				//
				new ArrayNode().setNodeId(new LogicalTimestamp().setReplicaId(101L).setSequenceNumber(2L))
						.setArrayId(ids.get(3)).setDataId(ids.get(6))
						.setReferenceNodeId(new LogicalTimestamp().setReplicaId(101L).setSequenceNumber(1L))

		);
		assertEquals(expected, results);

	}

	@Test
	public void testHandleBatch() {
		ArrayNode one = new ArrayNode().setNodeId(ids.get(0));
		ArrayNode two = new ArrayNode().setNodeId(ids.get(1));
		List<ArrayNode> expanded = List.of(one, two);

		doReturn(expanded).when(handler).expandInsertArrays(batch);
		when(mockDao.findArrayInsertLocation(sessionId, replicaId, one)).thenReturn(Optional.empty());
		when(mockDao.findArrayInsertLocation(sessionId, replicaId, two)).thenReturn(Optional.of(ids.get(2)));

		// call under test
		Set<LogicalTimestamp> changes = handler.handleBatch(sessionId, replicaId, batch);
		assertEquals(Set.of(two.getId()), changes);

		verify(mockDao).insertIntoArray(sessionId, replicaId, two);
		assertEquals(ids.get(2), two.getReferenceNodeId());
		verifyNoMoreInteractions(mockDao);
	}
}
