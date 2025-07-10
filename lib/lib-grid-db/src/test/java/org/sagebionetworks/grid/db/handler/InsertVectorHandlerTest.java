package org.sagebionetworks.grid.db.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertVector;

@ExtendWith(MockitoExtension.class)
public class InsertVectorHandlerTest {

	@Mock
	private GridIndexDao mockDao;

	@InjectMocks
	private InsertVectorHandler handler;

	private String sessionId;
	private Long replicaId;

	private List<LogicalTimestamp> ids;
	private List<InsertVector> inserts;
	private List<ConstantNode> constants;
	private List<VectorNode> currentVectors;

	@BeforeEach
	public void before() {
		sessionId = "sessionOne";
		replicaId = 123L;
		ids = List.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L),
				new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L),
				new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L),
				new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(10L));

		inserts = List.of(new InsertVector().setVectorId(ids.get(0)).setMap(Map.of(2, ids.get(1), 0, ids.get(2))),
				new InsertVector().setVectorId(ids.get(3)).setMap(Map.of(1, ids.get(2))));

		constants = List.of(new ConstantNode().setId(ids.get(1)).setValue(111L),
				new ConstantNode().setId(ids.get(2)).setValue("one"));

		currentVectors = List.of(new VectorNode().setId(ids.get(0)),
				new VectorNode().setId(ids.get(3)).setValueFromJson("{\"c1\":{\"v\":\"one\",\"i\":[5,6]}}"));
	}

	@Test
	public void testHandleBatch() {

		when(mockDao.getConstants(eq(sessionId), eq(replicaId),
				argThat(list -> list.size() == 2
						&& new HashSet<>(list).equals(new HashSet<>(List.of(ids.get(1), ids.get(2)))))))
				.thenReturn(constants);
		when(mockDao.getVectors(sessionId, replicaId, List.of(ids.get(0), ids.get(3)))).thenReturn(currentVectors);

		// call under test
		handler.handleBatch(sessionId, replicaId, inserts);

		// only the first changed, so only it should be saved.
		verify(mockDao).saveVectors(sessionId, replicaId, List.of(new VectorNode().setId(ids.get(0))
				.setValueFromJson("{\"c2\":{\"v\":111,\"i\":[3,4]},\"c0\":{\"v\":\"one\",\"i\":[5,6]}}")));

	}

	@Test
	public void testHandleBatchWithDoesNotExist() {

		when(mockDao.getConstants(eq(sessionId), eq(replicaId),
				argThat(list -> list.size() == 2
						&& new HashSet<>(list).equals(new HashSet<>(List.of(ids.get(1), ids.get(2)))))))
				.thenReturn(constants);
		// no current vectors match
		when(mockDao.getVectors(sessionId, replicaId, List.of(ids.get(0), ids.get(3))))
				.thenReturn(Collections.emptyList());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.handleBatch(sessionId, replicaId, inserts);
		}).getMessage();
		assertEquals("Cannot update a vector that does not exist: LogicalTimestamp [replicaId=1, sequenceNumber=2]", message);

		verify(mockDao, never()).saveVectors(any(), any(), any());

	}
}
