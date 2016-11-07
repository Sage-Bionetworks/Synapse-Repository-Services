package org.sagebionetworks.migration.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRowMetadataRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;

public interface AsyncMigrationRequestProcessor {
	
	public void processAsyncMigrationRequest(
			final ProgressCallback<Void> progressCallback, final UserInfo user,
			final AsyncMigrationRequest mReq, final String jobId) throws Throwable;
}
