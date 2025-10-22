package org.sagebionetworks.migration.worker;

import java.io.IOException;

import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.migration.AdminRequest;
import org.sagebionetworks.repo.model.migration.AdminResponse;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationResponse;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountsRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeRangeRequest;
import org.sagebionetworks.repo.model.migration.BatchChecksumRequest;
import org.sagebionetworks.repo.model.migration.CalculateOptimalRangeRequest;
import org.sagebionetworks.repo.model.migration.RestoreTypeRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MigrationWorker implements AsyncJobRunner<AsyncMigrationRequest, AsyncMigrationResponse> {
	
	private MigrationManager migrationManager;

	@Autowired
	public MigrationWorker(MigrationManager migrationManager) {
		this.migrationManager = migrationManager;
	}
	
	@Override
	public Class<AsyncMigrationRequest> getRequestType() {
		return AsyncMigrationRequest.class;
	}
	
	@Override
	public Class<AsyncMigrationResponse> getResponseType() {
		return AsyncMigrationResponse.class;
	}
	
	@Override
	public AsyncMigrationResponse run(String jobId, UserInfo user, AsyncMigrationRequest request, AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		
		AdminResponse resp = processRequest(user, request.getAdminRequest(), jobId);
		
		AsyncMigrationResponse mResp = new AsyncMigrationResponse()
			.setAdminResponse(resp);
		
		return mResp;
	}
	
	AdminResponse processRequest(final UserInfo user, final AdminRequest req, final String jobId) throws DatastoreException, NotFoundException, IOException {
		if (req instanceof AsyncMigrationTypeCountRequest) {
			return migrationManager.processAsyncMigrationTypeCountRequest(user, (AsyncMigrationTypeCountRequest)req);
		} else if (req instanceof AsyncMigrationTypeCountsRequest) {
			return migrationManager.processAsyncMigrationTypeCountsRequest(user, (AsyncMigrationTypeCountsRequest)req);
		} else if (req instanceof AsyncMigrationTypeChecksumRequest) {
			return migrationManager.processAsyncMigrationTypeChecksumRequest(user, (AsyncMigrationTypeChecksumRequest)req);
		} else if (req instanceof AsyncMigrationRangeChecksumRequest) {
			return migrationManager.processAsyncMigrationRangeChecksumRequest(user, (AsyncMigrationRangeChecksumRequest)req);
		} else if (req instanceof BackupTypeRangeRequest) {
			return migrationManager.backupRequest(user, (BackupTypeRangeRequest)req);
		} else if (req instanceof RestoreTypeRequest) {
			return migrationManager.restoreRequest(user, (RestoreTypeRequest)req);
		} else if (req instanceof CalculateOptimalRangeRequest) {
			return migrationManager.calculateOptimalRanges(user, (CalculateOptimalRangeRequest)req);
		} else if (req instanceof BatchChecksumRequest) {
			return migrationManager.calculateBatchChecksums(user, (BatchChecksumRequest)req);
		} else {
			throw new IllegalArgumentException("AsyncMigrationRequest not supported.");
		}
	}

}
