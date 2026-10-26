package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.PatchUtils;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.GridReplicaPatchBuilderManagerImpl.BufferedPatchPublisher;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.PatchInfo;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

@ExtendWith(MockitoExtension.class)
public class GridReplicaPatchBuilderManagerImplTest {

	@Mock
	private ChangePatchBuilder mockChangePatchBuilder;
	@Mock
	private GridDao mockGridDao;
	@Mock
	private GridIndexDao mockGridIndexDao;
	@Mock
	private PatchPublisher mockPatchPublisher;
	@Mock
	private ChangeHandler<UpdateMetadataChange> mockChangeHandler;
	@Mock
	private BufferedPatchPublisher mockBufferedPatchPublisher;

	private IntendedChangeSet changeSet;
	private String sessionId;
	private Long replicaId;
	private String connectionId;
	private List<LogicalTimestamp> currentClock;
	private LogicalTimestamp clock;
	private GridConnectionInfo validationConnection;
	private Long patchSpan;
	private Long clockSequenceMaximum;

	private GridReplicaPatchBuilderManagerImpl manager;

	@BeforeEach
	public void before() {
		connectionId = "con123";
		replicaId = 3L;
		sessionId = "session34";
		clockSequenceMaximum = 98L;
		changeSet = new IntendedChangeSet().setSessionId(sessionId).setReplicaId(replicaId)
				.setConnectionId(connectionId)
				.setChanges(List.of(new UpdateMetadataChange()
						.setRowMetadataId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L))))
				.setClockSequenceMaximum(clockSequenceMaximum);
		clock = new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L);
		currentClock = List.of(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L));
		patchSpan = 333L;
		when(mockChangeHandler.getType()).thenReturn(IntendedChangeType.update_row_metadata);

		manager = Mockito.spy(new GridReplicaPatchBuilderManagerImpl(mockGridDao, mockGridIndexDao, mockPatchPublisher,
				List.of(mockChangeHandler)));

		validationConnection = new GridConnectionInfo().setConnectionId(connectionId).setSessionId(sessionId)
				.setReplicaId(replicaId).setSource(EventSource.VALIDATION);
	}

	@Test
	public void testBuildPatchWithHighWaterLessThanMaxClock() throws IOException {
		Long highWater = clockSequenceMaximum - 1L;
		LogicalTimestamp expectedPatchId = new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(clockSequenceMaximum);
		setupBuildPatch(Optional.of(highWater), expectedPatchId, patchSpan);

		// call under test
		manager.buildPatch(changeSet);

		verifyRangeReservedBeforePublish(clockSequenceMaximum + patchSpan);
	}

	@Test
	public void testBuildPatchWithHighWaterEqualToMaxClock() throws IOException {
		Long highWater = clockSequenceMaximum;
		LogicalTimestamp expectedPatchId = new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(clockSequenceMaximum);
		setupBuildPatch(Optional.of(highWater), expectedPatchId, patchSpan);

		// call under test
		manager.buildPatch(changeSet);

		verifyRangeReservedBeforePublish(clockSequenceMaximum + patchSpan);
	}

	@Test
	public void testBuildPatchWithHighWaterGreaterThanMaxClock() throws IOException {
		Long highWater = clockSequenceMaximum + 1L;
		LogicalTimestamp expectedPatchId = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(highWater);
		setupBuildPatch(Optional.of(highWater), expectedPatchId, patchSpan);

		// call under test
		manager.buildPatch(changeSet);

		verifyRangeReservedBeforePublish(highWater + patchSpan);
	}

	@Test
	public void testBuildPatchWithNoHighWater() throws IOException {
		// a replica that has not written since the high-water mark was introduced, or whose backup predates it
		LogicalTimestamp expectedPatchId = new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(clockSequenceMaximum);
		setupBuildPatch(Optional.empty(), expectedPatchId, patchSpan);

		// call under test
		manager.buildPatch(changeSet);

		verifyRangeReservedBeforePublish(clockSequenceMaximum + patchSpan);
	}

	@Test
	public void testBuildPatchWithNoPatches() throws IOException {
		LogicalTimestamp expectedPatchId = new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(clockSequenceMaximum);
		setupBuildPatch(Optional.empty(), expectedPatchId, 0L);

		// call under test
		manager.buildPatch(changeSet);

		verify(mockChangePatchBuilder).close();
		verify(mockGridDao, never()).setReplicaSequenceHighWater(any(), any(), anyLong());
		verify(mockBufferedPatchPublisher, never()).publishAll(any());
	}

	@Test
	public void testBuildPatchWithNoConnection() throws IOException {
		when(mockGridDao.getConnection(connectionId)).thenReturn(Optional.empty());
		// call under test
		manager.buildPatch(changeSet);
		verify(mockGridDao, never()).getReplicaSequenceHighWater(any(), any());
		verify(mockGridDao, never()).setReplicaSequenceHighWater(any(), any(), anyLong());
		verifyNoMoreInteractions(mockGridIndexDao, mockBufferedPatchPublisher, mockPatchPublisher);
	}

	void setupBuildPatch(Optional<Long> highWater, LogicalTimestamp expectedPatchId, Long totalSpan)
			throws IOException {
		when(mockGridDao.getConnection(connectionId)).thenReturn(Optional.of(validationConnection));
		when(mockGridDao.getReplicaSequenceHighWater(sessionId, replicaId)).thenReturn(highWater);
		doReturn(mockBufferedPatchPublisher).when(manager).createNewBufferedPatchPublisher();
		when(mockBufferedPatchPublisher.getTotalPatchSpan()).thenReturn(totalSpan);
		doReturn(mockChangePatchBuilder).when(manager).createChangePatchBuilder(mockBufferedPatchPublisher,
				validationConnection, expectedPatchId);
		doNothing().when(manager).processChanges(mockChangePatchBuilder, changeSet.getChanges());
	}

	void verifyRangeReservedBeforePublish(long expectedHighWater) throws IOException {
		InOrder inOrder = inOrder(mockChangePatchBuilder, mockGridDao, mockBufferedPatchPublisher);
		inOrder.verify(mockChangePatchBuilder).close();
		inOrder.verify(mockGridDao).setReplicaSequenceHighWater(sessionId, replicaId, expectedHighWater);
		inOrder.verify(mockBufferedPatchPublisher).publishAll(mockPatchPublisher);
		// the index database is no longer the record of the writer's sequence
		verifyNoMoreInteractions(mockGridIndexDao);
	}

	@Test
	public void testBufferedPatchPublisher() {
		BufferedPatchPublisher buffer = new BufferedPatchPublisher();
		JSONArray first = new JSONArray("[1]");
		JSONArray second = new JSONArray("[2]");

		// call under test
		buffer.publishPatch(validationConnection, first, 3L);
		buffer.publishPatch(validationConnection, second, 4L);

		assertEquals(7L, buffer.getTotalPatchSpan());
		// nothing is published until publishAll()
		verifyNoMoreInteractions(mockPatchPublisher);

		// call under test
		buffer.publishAll(mockPatchPublisher);

		InOrder inOrder = inOrder(mockPatchPublisher);
		inOrder.verify(mockPatchPublisher).publishPatch(validationConnection, first, 3L);
		inOrder.verify(mockPatchPublisher).publishPatch(validationConnection, second, 4L);
		verifyNoMoreInteractions(mockPatchPublisher);
	}

	@Test
	public void testCreateChangePatchBuilder() {
		ChangePatchBuilder builder = manager.createChangePatchBuilder(mockPatchPublisher, validationConnection, clock);
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
	
	@Test
	public void testGetCurrentClock() {
		Long last = 101L;
		when(mockGridIndexDao.getClockSequenceNumber(sessionId, replicaId, replicaId)).thenReturn(Optional.of(last));
		when(mockGridIndexDao.getClock(sessionId, replicaId)).thenReturn(currentClock);
		when(mockGridDao.listMissingPatchInfoForClock(sessionId, currentClock, 1)).thenReturn(List.of());

		// call under test
		Optional<LogicalTimestamp> result = manager.getCurrentClockIfAllPatchesApplied(sessionId, replicaId);
		assertEquals(Optional.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(last)), result);
	}

	@Test
	public void testGetCurrentClockWithNoSequence() {
		when(mockGridIndexDao.getClockSequenceNumber(sessionId, replicaId, replicaId)).thenReturn(Optional.empty());
		when(mockGridIndexDao.getClock(sessionId, replicaId)).thenReturn(currentClock);
		when(mockGridDao.listMissingPatchInfoForClock(sessionId, currentClock, 1)).thenReturn(List.of());

		// call under test
		Optional<LogicalTimestamp> result = manager.getCurrentClockIfAllPatchesApplied(sessionId, replicaId);
		assertEquals(Optional.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(0L)), result);
	}

	@Test
	public void testGetCurrentClockWithMissingPatches() {
		when(mockGridIndexDao.getClock(sessionId, replicaId)).thenReturn(currentClock);
		when(mockGridDao.listMissingPatchInfoForClock(sessionId, currentClock, 1))
				.thenReturn(List.of(new PatchInfo().setPatchId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(999L))));

		// call under test
		Optional<LogicalTimestamp> result = manager.getCurrentClockIfAllPatchesApplied(sessionId, replicaId);
		assertEquals(Optional.empty(), result);
	}

}
