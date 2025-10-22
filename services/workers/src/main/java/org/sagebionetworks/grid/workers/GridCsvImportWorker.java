package org.sagebionetworks.grid.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.grid.internal.replica.merge.GridCsvImporter;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.GridCsvImportRequest;
import org.sagebionetworks.repo.model.grid.GridCsvImportResponse;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.sagebionetworks.workers.util.semaphore.WriteLockRequest;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphore;
import org.springframework.stereotype.Service;

@Service
public class GridCsvImportWorker implements AsyncJobRunner<GridCsvImportRequest, GridCsvImportResponse> {

	private static final Logger LOGGER = LogManager.getLogger(GridCsvImportWorker.class);
	private static final String SEMAPHORE_KEY_PREFIX = "gridCsvImport-";
	
	private GridCsvImporter importer;
	private WriteReadSemaphore semaphore;
	
	
	public GridCsvImportWorker(GridCsvImporter importer, WriteReadSemaphore semaphore) {
		this.importer = importer;
		this.semaphore = semaphore;
	}

	@Override
	public Class<GridCsvImportRequest> getRequestType() {
		return GridCsvImportRequest.class;
	}

	@Override
	public Class<GridCsvImportResponse> getResponseType() {
		return GridCsvImportResponse.class;
	}

	@Override
	public GridCsvImportResponse run(String jobId, UserInfo user, GridCsvImportRequest request, AsyncJobProgressCallback jobProgressCallback)
		throws RecoverableMessageException, Exception {
		try {
			
			String callerContext = SEMAPHORE_KEY_PREFIX + jobId + "-" + user.getId();			
			String semaphoreKey = SEMAPHORE_KEY_PREFIX + request.getSessionId();
			
			return semaphore.tryRunWithWriteLock(new WriteLockRequest(jobProgressCallback, callerContext, semaphoreKey), callback -> 				
				importer.importCsv(user, request, jobProgressCallback)
			);
			
		} catch (RecoverableMessageException e) {
			throw e;
		} catch (LockUnavilableException e) {
			LOGGER.warn("Failed to import CSV into the grid (will retry): " + e.getMessage());
			throw new RecoverableMessageException(e);
		} catch (Exception e) {
			LOGGER.error("Failed to import CSV into the grid: " + e.getMessage(), e);
			throw e;
		}
	}

}
