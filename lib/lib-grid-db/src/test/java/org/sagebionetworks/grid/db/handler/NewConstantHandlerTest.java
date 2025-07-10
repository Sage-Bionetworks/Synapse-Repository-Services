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
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;

@ExtendWith(MockitoExtension.class)
public class NewConstantHandlerTest {

	@Mock
	private GridIndexDao mockDao;

	@InjectMocks
	private NewConstantHandler handler;

	private String sessionId;
	private Long replicaId;

	private List<NewConstant> constants;

	@BeforeEach
	public void before() {
		sessionId = "sessionOne";
		replicaId = 123L;

		constants = List.of(
				new NewConstant().setOperationId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L))
						.setValue(new ConValue(ConType.BOOLEAN, true)),
				new NewConstant().setOperationId(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L))
						.setValue(new ConValue(ConType.STRING, "hello")));
	}

	@Test
	public void testHandleBatch() {
		// call under test
		handler.handleBatch(sessionId, replicaId, constants);
		verify(mockDao).saveIndex(sessionId, replicaId, IndexType.con,
				constants.stream().map(NewConstant::getOperationId).collect(Collectors.toList()));
		verify(mockDao).saveNewConstants(sessionId, replicaId,
				constants.stream()
						.map(c -> new ConstantNode().setId(c.getOperationId()).setValue(c.getValue().getValue()))
						.collect(Collectors.toList()));
	}
}
