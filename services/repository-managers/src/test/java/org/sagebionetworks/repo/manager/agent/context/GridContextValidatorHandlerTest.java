package org.sagebionetworks.repo.manager.agent.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;

@ExtendWith(MockitoExtension.class)
public class GridContextValidatorHandlerTest {

	@Mock
	private UserInfo mockUser;
	@Mock
	private GridManager mockGridManager;

	@InjectMocks
	private GridContextValidatorHandler handler;

	private String gridSessionId;
	private Long replicaId;
	private GridAgentSessionContext context;
	private GridReplica replica;
	private GridSession gridSession;
	private GridReplica agentReplica;

	@BeforeEach
	public void before() {
		gridSessionId = "s123";
		replicaId = 456L;
		context = new GridAgentSessionContext().setGridSessionId(gridSessionId).setUsersReplicaId(replicaId);
		replica = new GridReplica().setGridSessionId(gridSessionId).setReplicaId(replicaId);
		gridSession = new GridSession().setSessionId(gridSessionId);
		agentReplica = new GridReplica().setReplicaId(88L);
	}

	@Test
	public void testDoContextValidation() {
		when(mockGridManager.getReplica(mockUser, gridSessionId, replicaId)).thenReturn(replica);
		when(mockGridManager.getGridSession(mockUser, gridSessionId)).thenReturn(gridSession);
		when(mockGridManager.createAgentReplica(mockUser, gridSession)).thenReturn(agentReplica);
		// call under test
		handler.doContextValidation(mockUser, context);

	}

	@Test
	public void testDoContextValidationWithNullContext() {
		context = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.doContextValidation(mockUser, context);
		}).getMessage();
		assertEquals("context is required.", message);
	}

}
