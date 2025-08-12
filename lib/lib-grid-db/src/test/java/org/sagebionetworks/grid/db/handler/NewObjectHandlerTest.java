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
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewObject;

@ExtendWith(MockitoExtension.class)
public class NewObjectHandlerTest {

	@Mock
	private GridIndexDao mockDao;

	@InjectMocks
	private NewObjectHandler handler;

	private String sessionId;
	private Long replicaId;

	private List<NewObject> objects;

	@BeforeEach
	public void before() {
		sessionId = "sessionOne";
		replicaId = 123L;

		objects = List.of(new NewObject(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L)),
				new NewObject(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L)));
	}

	@Test
	public void testHandleBatch() {
		// call under test
		Set<LogicalTimestamp> changes = handler.handleBatch(sessionId, replicaId, objects);
		List<LogicalTimestamp> objectIds = objects.stream().map(NewObject::getOperationId).collect(Collectors.toList());
		assertEquals(new LinkedHashSet<>(objectIds), changes);
		verify(mockDao).saveIndex(sessionId, replicaId, IndexType.obj, objectIds);
		verify(mockDao).saveObjects(sessionId, replicaId,
				objects.stream().map(o -> new ObjectNode().setId(o.getOperationId())).collect(Collectors.toList()));
	}
}
