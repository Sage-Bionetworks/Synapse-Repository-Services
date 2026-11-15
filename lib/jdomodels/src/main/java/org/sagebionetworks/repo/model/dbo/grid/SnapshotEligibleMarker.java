package org.sagebionetworks.repo.model.dbo.grid;

import java.sql.Timestamp;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * A row of GRID_SNAPSHOT_ELIGIBLE: the patch this stack saved that makes the session eligible for snapshots.
 *
 * @param sessionId The grid session.
 * @param patchId   The logical ID of the marked patch.
 * @param s3Key     The S3 key of the marked patch, unique per patch.
 * @param createdOn When the marker was written, for diagnostics.
 */
public record SnapshotEligibleMarker(String sessionId, LogicalTimestamp patchId, String s3Key, Timestamp createdOn) {
}
