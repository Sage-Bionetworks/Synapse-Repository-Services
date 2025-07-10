package org.sagebionetworks.grid.db.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertValue;

@ExtendWith(MockitoExtension.class)
public class InsertValueHandlerTest {
	@Mock
	private GridIndexDao mockDao;

	@InjectMocks
	private InsertValueHandler handler;

	private String sessionId;
	private Long replicaId;

	private List<InsertValue> inserts;

	@BeforeEach
	public void before() {
		sessionId = "sessionOne";
		replicaId = 123L;

		inserts = List.of(
				new InsertValue().setValueId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L))
						.setReferenceId(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L)),
				new InsertValue().setValueId(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L))
						.setReferenceId(new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L)));
	}

	@Test
	public void testHandleBatch() {

		when(mockDao.getValues(sessionId, replicaId,
				inserts.stream().map(InsertValue::getValueId).collect(Collectors.toList())))
				.thenReturn(List.of(new ValueNode().setId(inserts.get(0).getValueId()),
						new ValueNode().setId(inserts.get(1).getValueId()).setValueFromJson("[7,8]")));

		// call under test
		handler.handleBatch(sessionId, replicaId, inserts);
		// only the first items should be saved as the second is already set.
		verify(mockDao).saveValues(sessionId, replicaId,
				List.of(new ValueNode().setId(inserts.get(0).getValueId()).setValueFromJson("[3,4]")));
	}

	@Test
	public void testHandleBatchWithObjectDoesNotExist() {

		when(mockDao.getValues(sessionId, replicaId,
				inserts.stream().map(InsertValue::getValueId).collect(Collectors.toList())))
				.thenReturn(List.of(new ValueNode().setId(inserts.get(0).getValueId())));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.handleBatch(sessionId, replicaId, inserts);
		}).getMessage();
		assertEquals("Cannot update a value that does not exist: LogicalTimestamp [replicaId=5, sequenceNumber=6]",
				message);

		verify(mockDao, never()).saveValues(any(), any(), any());
	}
}
