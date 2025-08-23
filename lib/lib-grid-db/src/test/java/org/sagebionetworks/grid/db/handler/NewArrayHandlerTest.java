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
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewArray;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;

@ExtendWith(MockitoExtension.class)
public class NewArrayHandlerTest {

	@Mock
	private GridIndexDao mockDao;

	@InjectMocks
	private NewArrayHandler handler;

	private String sessionId;
	private Long replicaId;

	private List<NewArray> arrays;

	@BeforeEach
	public void before() {
		sessionId = "sessionOne";
		replicaId = 123L;

		arrays = List.of(new NewArray(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L)),
				new NewArray(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L)));
	}

	@Test
	public void testHandleBatch() {
		// call under test
		Set<LogicalTimestamp> changes = handler.handleBatch(sessionId, replicaId, arrays);
		List<LogicalTimestamp> arrayIds = arrays.stream().map(NewArray::getOperationId).collect(Collectors.toList());
		assertEquals(new LinkedHashSet<>(arrayIds), changes);
		verify(mockDao).saveIndex(sessionId, replicaId, IndexType.arr, arrayIds);
		verify(mockDao).createArrayBatch(sessionId, replicaId, arrayIds);
	}
}
