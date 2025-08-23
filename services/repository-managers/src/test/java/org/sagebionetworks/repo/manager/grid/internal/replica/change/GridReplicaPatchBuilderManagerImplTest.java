package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.ConstantProvider;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.PatchUtils;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class GridReplicaPatchBuilderManagerImplTest {

	@Mock
	private ChangePatchBuilder mockChangePatchBuilder;
	@Mock
	private GridDao mockGridDao;
	@Mock
	private GridIndexDao mockGridIndexDao;
	@Mock
	private ConstantProvider mocConstantProvider;
	@Mock
	private PatchPublisher mockPatchPublisher;
	@Mock
	private ChangeHandler<UpdateMetadataChange> mockChangeHandler;

	private IntendedChangeSet changeSet;
	private String sessionId;
	private Long replicaId;
	private String connectionId;
	private GridConnectionInfo con;
	private List<LogicalTimestamp> currentClock;
	private LogicalTimestamp clock;
	private GridSession session;

	private GridReplicaPatchBuilderManagerImpl manager;

	@BeforeEach
	public void before() {
		connectionId = "con123";
		replicaId = 3L;
		sessionId = "session34";

		con = new GridConnectionInfo().setConnectionId(connectionId).setSessionId(sessionId).setReplicaId(replicaId);
		changeSet = new IntendedChangeSet().setSessionId(sessionId).setReplicaId(replicaId)
				.setConnectionId(connectionId).setChanges(List.of(new UpdateMetadataChange()
						.setRowMetadataId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L))));
		clock = new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L);
		currentClock = List.of(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L));
		session = new GridSession().setEtag("etag").setSessionId(sessionId);
		when(mockChangeHandler.getType()).thenReturn(IntendedChangeType.update_row_metadata);

		manager = Mockito.spy(new GridReplicaPatchBuilderManagerImpl(mockGridDao, mockGridIndexDao, mockPatchPublisher,
				List.of(mockChangeHandler), mocConstantProvider));
	}

	@Test
	public void testBuildPatch() throws IOException {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(session));
		doReturn(Optional.of(clock)).when(manager).getCurrentClockIfAllPatchesApplied(sessionId, replicaId);
		doReturn(mockChangePatchBuilder).when(manager).createChangePatchBuilder(con, clock);
		doNothing().when(manager).processChanges(mockChangePatchBuilder, changeSet.getChanges());

		// call under test
		manager.buildPatch(changeSet);
		verify(mockChangePatchBuilder).close();
	}

	@Test
	public void testBuildPatchWithNoSession() throws IOException {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.empty());

		// call under test
		manager.buildPatch(changeSet);
		verify(manager, never()).getCurrentClockIfAllPatchesApplied(any(), any());
		verify(manager, never()).createChangePatchBuilder(any(), any());
		verify(manager, never()).createChangePatchBuilder(any(), any());
		verifyNoMoreInteractions(mockGridDao, mockGridIndexDao);
	}

	@Test
	public void testBuildPatchWithNoClock() throws IOException {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(session));
		doReturn(Optional.empty()).when(manager).getCurrentClockIfAllPatchesApplied(sessionId, replicaId);
		String message = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			manager.buildPatch(changeSet);
		}).getMessage();
		assertEquals("Waiting for outstanding patches to be applied before building new ones", message);

		verifyZeroInteractions(mockChangePatchBuilder);
		verify(manager, never()).createChangePatchBuilder(any(), any());
		verify(manager, never()).processChanges(any(), any());
	}

	@Test
	public void testCreateChangePatchBuilder() {
		ChangePatchBuilder builder = manager.createChangePatchBuilder(con, clock);
		assertNotNull(builder);
		assertEquals(PatchUtils.MAX_BYTES_PER_PATCH, builder.getMaxBytesPerPatch());
	}

	@Test
	public void testProcessChanges() {
		doNothing().when(mockChangeHandler).handleChange(mockChangePatchBuilder,
				(UpdateMetadataChange) changeSet.getChanges().get(0));

		// call under test
		manager.processChanges(mockChangePatchBuilder, changeSet.getChanges());

		verify(mockChangeHandler).handleChange(mockChangePatchBuilder,
				(UpdateMetadataChange) changeSet.getChanges().get(0));
	}

//<<<<<<< HEAD
	@Test
	public void testGetCurrentClock() {
		Long last = 101L;
		when(mockGridIndexDao.getClockSequenceNumber(sessionId, replicaId, replicaId)).thenReturn(Optional.of(last));
		when(mockGridIndexDao.getClock(sessionId, replicaId)).thenReturn(currentClock);
		when(mockGridDao.listMissingPatchIdsForClock(sessionId, currentClock, 1)).thenReturn(List.of());

		// call under test
		Optional<LogicalTimestamp> result = manager.getCurrentClockIfAllPatchesApplied(sessionId, replicaId);
		assertEquals(Optional.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(last)), result);
	}

	@Test
	public void testGetCurrentClockWithNoSequence() {
		when(mockGridIndexDao.getClockSequenceNumber(sessionId, replicaId, replicaId)).thenReturn(Optional.empty());
		when(mockGridIndexDao.getClock(sessionId, replicaId)).thenReturn(currentClock);
		when(mockGridDao.listMissingPatchIdsForClock(sessionId, currentClock, 1)).thenReturn(List.of());

		// call under test
		Optional<LogicalTimestamp> result = manager.getCurrentClockIfAllPatchesApplied(sessionId, replicaId);
		assertEquals(Optional.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(0L)), result);
	}

	@Test
	public void testGetCurrentClockWithMissingPatches() {
		when(mockGridIndexDao.getClock(sessionId, replicaId)).thenReturn(currentClock);
		when(mockGridDao.listMissingPatchIdsForClock(sessionId, currentClock, 1))
				.thenReturn(List.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(999L)));

		// call under test
		Optional<LogicalTimestamp> result = manager.getCurrentClockIfAllPatchesApplied(sessionId, replicaId);
		assertEquals(Optional.empty(), result);
	}

}
