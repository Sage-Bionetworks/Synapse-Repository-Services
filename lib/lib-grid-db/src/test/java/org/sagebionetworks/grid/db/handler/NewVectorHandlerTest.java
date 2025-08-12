package org.sagebionetworks.grid.db.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewVector;

@ExtendWith(MockitoExtension.class)
public class NewVectorHandlerTest {
	@Mock
	private GridIndexDao mockDao;

	@InjectMocks
	private NewVectorHandler handler;

	private String sessionId;
	private Long replicaId;

	private List<NewVector> vectors;

	@BeforeEach
	public void before() {
		sessionId = "sessionOne";
		replicaId = 123L;

		vectors = List.of(new NewVector(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L)),
				new NewVector(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L)));
	}

	@Test
	public void testHandleBatch() {
		// call under test
		Set<LogicalTimestamp> changes = handler.handleBatch(sessionId, replicaId, vectors);
		List<LogicalTimestamp> vecIds = vectors.stream().map(NewVector::getOperationId).collect(Collectors.toList());
		assertEquals(new LinkedHashSet<>(vecIds), changes);
		verify(mockDao).saveIndex(sessionId, replicaId, IndexType.vec, vecIds);
		verify(mockDao).saveVectors(sessionId, replicaId,
				vectors.stream().map(n -> new VectorNode().setId(n.getOperationId())).collect(Collectors.toList()));
	}
}
