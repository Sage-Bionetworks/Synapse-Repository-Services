package org.sagebionetworks.repo.manager.grid.internal.replica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.GridReplicaPatchBuilderManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
class GridReplicaSupportImplTest {

	@Mock
	private GridManager mockGridManager;
	
	@Mock
	private GridReplicaViewManager mockViewManager;
	
	@Mock
	private EntityManager mockEntityManager;
	
	@Mock
	private GridReplicaPatchBuilderManager mockPatchBuilderManager;
	
	@InjectMocks
	private GridReplicaSupportImpl gridReplicaSupport;
	
	private UserInfo user;
	
	private GridConnectionInfo connectionInfo;
	
	private GridSession gridSession;
	
	@Mock
	private GridHeader mockGridHeader;
	
	@Mock
	private RecordSet mockRecordSet;
	
	@Mock
	private LogicalTimestamp mockLogicalTimestamp;
	
	private static final String SESSION_ID = "sessionId123";
	private static final Long REPLICA_ID = 456L;
	
	private static final String SOURCE_ENTITY_ID = "entityId789";

	@BeforeEach
	void setUp() {
		user = new UserInfo(false, 123L, AuthorizationConstants.DEFAULT_REALM_ID);
		
		gridSession = new GridSession()
			.setSessionId(SESSION_ID)
			.setSourceEntityId(SOURCE_ENTITY_ID);
		
		connectionInfo = new GridConnectionInfo()
			.setSessionId(SESSION_ID)
			.setReplicaId(REPLICA_ID);
	}

	@Test
	void testGetGridHeaderOrThrow() throws RecoverableMessageException {		
		when(mockGridManager.getSingletonConnection(SESSION_ID, EventSource.INTERNAL)).thenReturn(Optional.of(connectionInfo));
		when(mockPatchBuilderManager.getCurrentClockIfAllPatchesApplied(SESSION_ID, REPLICA_ID)).thenReturn(Optional.of(mockLogicalTimestamp));
		when(mockViewManager.readHeader(SESSION_ID, REPLICA_ID)).thenReturn(Optional.of(mockGridHeader));

		// Call under test
		GridHeader result = gridReplicaSupport.getGridHeaderOrThrow(gridSession);

		assertEquals(mockGridHeader, result);
	}
	
	@Test
	void testGetGridHeaderOrThrowWithNoConnection() throws RecoverableMessageException {		
		when(mockGridManager.getSingletonConnection(SESSION_ID, EventSource.INTERNAL)).thenReturn(Optional.empty());

		// Call under test
		assertEquals("No internal connection found for session: " + SESSION_ID, assertThrows(RecoverableMessageException.class, () -> {
			gridReplicaSupport.getGridHeaderOrThrow(gridSession);
		}).getMessage());
	}
	
	@Test
	void testGetGridHeaderOrThrowWithPatchesNotApplied() throws RecoverableMessageException {		
		when(mockGridManager.getSingletonConnection(SESSION_ID, EventSource.INTERNAL)).thenReturn(Optional.of(connectionInfo));
		when(mockPatchBuilderManager.getCurrentClockIfAllPatchesApplied(SESSION_ID, REPLICA_ID)).thenReturn(Optional.empty());

		// Call under test
		assertEquals("Current clock could not be retrieved, patches are still being applied to sessionId: " + SESSION_ID + ", replicaId: " + REPLICA_ID, assertThrows(RecoverableMessageException.class, () -> {
			gridReplicaSupport.getGridHeaderOrThrow(gridSession);
		}).getMessage());
	} 

	@Test
	void testGetGridHeaderOrThrowWithHeaderNotInstantiated() {
		when(mockGridManager.getSingletonConnection(SESSION_ID, EventSource.INTERNAL)).thenReturn(Optional.of(connectionInfo));
		when(mockPatchBuilderManager.getCurrentClockIfAllPatchesApplied(SESSION_ID, REPLICA_ID)).thenReturn(Optional.of(mockLogicalTimestamp));
		when(mockViewManager.readHeader(SESSION_ID, REPLICA_ID)).thenReturn(Optional.empty());
		
		// Call under test
		assertEquals("Grid header has not yet been instantiated for sessionId: " + SESSION_ID, assertThrows(RecoverableMessageException.class, () -> {
			gridReplicaSupport.getGridHeaderOrThrow(gridSession);
		}).getMessage());
	}

	@Test
	void testGetRecordSetOrThrow() {
		when(mockEntityManager.getEntity(user, SOURCE_ENTITY_ID)).thenReturn(mockRecordSet);

		// Call under test
		RecordSet result = gridReplicaSupport.getRecordSetOrThrow(user, gridSession);

		assertEquals(mockRecordSet, result);
		
		verify(mockEntityManager).getEntity(user, SOURCE_ENTITY_ID);
	}
	
	@Test
	void testGetRecordSetOrThrowWithUnsupportedType() {
		when(mockEntityManager.getEntity(user, SOURCE_ENTITY_ID)).thenReturn(Mockito.mock(Entity.class));

		assertEquals("Unsupported grid session: only a grid created from a record set is supported.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			gridReplicaSupport.getRecordSetOrThrow(user, gridSession);
		}).getMessage());
		
	}
}