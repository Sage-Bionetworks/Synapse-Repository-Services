package org.sagebionetworks.repo.manager.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.GridSnapshot;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Error;

@ExtendWith(MockitoExtension.class)
public class SnapshotStoreImplTest {

	@Mock
	private GridDao mockGridDao;
	@Mock
	private StackConfiguration mockConfig;
	@Mock
	private S3Client mockS3Client;
	@Mock
	private TransferManager mockTransferManager;
	@Mock
	private Upload mockUpload;
	@Mock
	private File mockSnapshotFile;

	private SnapshotStoreImpl snapshotStore;

	private String sessionId;
	private Long createdBy;
	private ClockTable clockTable;

	@BeforeEach
	public void before() {
		when(mockConfig.getStack()).thenReturn("dev");
		snapshotStore = new SnapshotStoreImpl(mockGridDao, mockConfig, mockS3Client, mockTransferManager);
		sessionId = "session456";
		createdBy = 123L;
		clockTable = new ClockTable(List.of(new LogicalTimestamp().setReplicaId(88L).setSequenceNumber(777L)));
	}

	@Test
	public void testSaveSnapshot() throws Exception {
		UploadResult uploadResult = new UploadResult();
		uploadResult.setKey("snapshot-key-123");
		when(mockTransferManager.upload(any(String.class), any(String.class), any(File.class))).thenReturn(mockUpload);
		when(mockUpload.waitForUploadResult()).thenReturn(uploadResult);

		// call under test
		snapshotStore.saveSnapshot(sessionId, clockTable, createdBy, mockSnapshotFile);

		verify(mockTransferManager).upload(eq("dev.grid.snapshot.sagebase.org"), any(String.class), eq(mockSnapshotFile));
		verify(mockGridDao).saveSnapshot(sessionId, clockTable, "snapshot-key-123", createdBy);
	}

	@Test
	public void testSaveSnapshotWithNullSessionId() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			snapshotStore.saveSnapshot(null, clockTable, createdBy, mockSnapshotFile);
		}).getMessage();
		assertEquals("sessionId is required.", message);
		verifyNoInteractions(mockGridDao, mockTransferManager);
	}

	@Test
	public void testSaveSnapshotWithNullClockTable() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			snapshotStore.saveSnapshot(sessionId, null, createdBy, mockSnapshotFile);
		}).getMessage();
		assertEquals("clockTable is required.", message);
		verifyNoInteractions(mockGridDao, mockTransferManager);
	}

	@Test
	public void testSaveSnapshotWithNullCreatedBy() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			snapshotStore.saveSnapshot(sessionId, clockTable, null, mockSnapshotFile);
		}).getMessage();
		assertEquals("createdBy is required.", message);
		verifyNoInteractions(mockGridDao, mockTransferManager);
	}

	@Test
	public void testSaveSnapshotWithNullSnapshotFile() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			snapshotStore.saveSnapshot(sessionId, clockTable, createdBy, null);
		}).getMessage();
		assertEquals("snapshotFile is required.", message);
		verifyNoInteractions(mockGridDao, mockTransferManager);
	}

	@Test
	public void testSaveSnapshotWithUploadFailure() {
		when(mockTransferManager.upload(any(String.class), any(String.class), any(File.class)))
				.thenThrow(new RuntimeException("Upload failed"));

		assertThrows(RuntimeException.class, () -> {
			// call under test
			snapshotStore.saveSnapshot(sessionId, clockTable, createdBy, mockSnapshotFile);
		});
		verifyNoInteractions(mockGridDao);
	}

	@Test
	public void testDeleteSupersededSnapshots() {
		Instant supersededBy = Instant.parse("2026-01-04T00:00:00Z");
		when(mockGridDao.getSupersededSnapshots(sessionId, Timestamp.from(supersededBy),
				SnapshotStoreImpl.MAX_SNAPSHOTS_PER_DELETE)).thenReturn(List.of(
						new GridSnapshot().setId(1L).setS3Key("snapshot/one.cbor"),
						new GridSnapshot().setId(2L).setS3Key("snapshot/two.cbor")));
		when(mockS3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(DeleteObjectsResponse.builder()
				.errors(S3Error.builder().key("snapshot/two.cbor").message("Access Denied").build()).build());

		// call under test
		int deleted = snapshotStore.deleteSupersededSnapshots(sessionId, supersededBy);

		// A file that fails to delete is only logged: its row is already gone
		assertEquals(2, deleted);
		verify(mockGridDao).deleteSnapshots(List.of(1L, 2L));
		verify(mockS3Client).deleteObjects(DeleteObjectsRequest.builder().bucket("dev.grid.snapshot.sagebase.org")
				.delete(Delete.builder()
						.objects(ObjectIdentifier.builder().key("snapshot/one.cbor").build(),
								ObjectIdentifier.builder().key("snapshot/two.cbor").build())
						.quiet(true).build())
				.build());
	}

	@Test
	public void testDeleteSupersededSnapshotsWithNoneSuperseded() {
		Instant supersededBy = Instant.parse("2026-01-04T00:00:00Z");
		when(mockGridDao.getSupersededSnapshots(sessionId, Timestamp.from(supersededBy),
				SnapshotStoreImpl.MAX_SNAPSHOTS_PER_DELETE)).thenReturn(List.of());

		// call under test
		int deleted = snapshotStore.deleteSupersededSnapshots(sessionId, supersededBy);

		assertEquals(0, deleted);
		verify(mockGridDao, never()).deleteSnapshots(any());
		verifyNoInteractions(mockS3Client);
	}
}
