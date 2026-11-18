package org.sagebionetworks.repo.manager.grid;

import java.io.File;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.GridSnapshot;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.model.UploadResult;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

@Service
public class SnapshotStoreImpl implements SnapshotStore {

	private static final Logger log = LogManager.getLogger(SnapshotStoreImpl.class);

	/**
	 * The most keys a single S3 DeleteObjects request accepts.
	 */
	static final int MAX_SNAPSHOTS_PER_DELETE = 1000;

	private final GridDao gridDao;
	private final String gridSnapshotBucket;
	private final S3Client s3Client;
	private final TransferManager transferManager;

	public SnapshotStoreImpl(GridDao gridDao, StackConfiguration config, S3Client s3Client,
			TransferManager transferManager) {
		this.gridDao = gridDao;
		this.gridSnapshotBucket = String.format("%s.grid.snapshot.sagebase.org", config.getStack());
		this.s3Client = s3Client;
		this.transferManager = transferManager;
	}

	@WriteTransaction
	@Override
	public void saveSnapshot(String sessionId, ClockTable clockTable, Long createdBy, File snapshotFile) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(clockTable, "clockTable");
		ValidateArgument.required(createdBy, "createdBy");
		ValidateArgument.required(snapshotFile, "snapshotFile");

		String s3Key = String.format("snapshot/%s/%d-%s.cbor", sessionId, System.currentTimeMillis(), UUID.randomUUID());

		UploadResult uploadResult;
		try {
			uploadResult = transferManager.upload(gridSnapshotBucket, s3Key, snapshotFile).waitForUploadResult();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Snapshot upload was interrupted", e);
		} catch (Exception e) {
			throw new RuntimeException("Failed to upload snapshot to S3", e);
		}
		gridDao.saveSnapshot(sessionId, clockTable, uploadResult.getKey(), createdBy);
	}

	@Override
	public int deleteSupersededSnapshots(String sessionId, Instant supersededBy) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(supersededBy, "supersededBy");
		List<GridSnapshot> superseded = gridDao.getSupersededSnapshots(sessionId, Timestamp.from(supersededBy),
				MAX_SNAPSHOTS_PER_DELETE);
		if (superseded.isEmpty()) {
			return 0;
		}
		/*
		 * Rows go first: a row whose file is gone would only matter if it were the latest snapshot, which is never
		 * superseded, but a file left behind by a failed S3 delete is only wasted storage.
		 */
		gridDao.deleteSnapshots(superseded.stream().map(GridSnapshot::getId).toList());
		List<ObjectIdentifier> keys = superseded.stream()
				.map(snapshot -> ObjectIdentifier.builder().key(snapshot.getS3Key()).build()).toList();
		DeleteObjectsResponse response = s3Client.deleteObjects(DeleteObjectsRequest.builder().bucket(gridSnapshotBucket)
				.delete(Delete.builder().objects(keys).quiet(true).build()).build());
		response.errors().forEach(error -> log.warn("Failed to delete snapshot file {} of session {}: {}",
				error.key(), sessionId, error.message()));
		return superseded.size();
	}
}
