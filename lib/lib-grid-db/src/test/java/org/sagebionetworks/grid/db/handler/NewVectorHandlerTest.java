package org.sagebionetworks.grid.db.handler;

import static org.mockito.Mockito.verify;

import java.util.List;
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

		vectors = List.of(new NewVector().setOperationId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L)),
				new NewVector().setOperationId(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L)));
	}

	@Test
	public void testHandleBatch() {
		// call under test
		handler.handleBatch(sessionId, replicaId, vectors);
		verify(mockDao).saveIndex(sessionId, replicaId, IndexType.vec,
				vectors.stream().map(NewVector::getOperationId).collect(Collectors.toList()));
		verify(mockDao).saveVectors(sessionId, replicaId,
				vectors.stream().map(n -> new VectorNode().setId(n.getOperationId())).collect(Collectors.toList()));
	}
}
