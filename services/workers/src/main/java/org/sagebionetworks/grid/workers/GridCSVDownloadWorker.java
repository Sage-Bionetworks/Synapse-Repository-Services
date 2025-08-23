package org.sagebionetworks.grid.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.grid.internal.replica.export.GridReplicaCsvExporter;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.DownloadFromGridRequest;
import org.sagebionetworks.repo.model.grid.DownloadFromGridResult;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This worker will stream the contents of a grid to a local CSV file and upload the file
 * to S3 as a FileHandle.
 */
@Service
public class GridCSVDownloadWorker implements AsyncJobRunner<DownloadFromGridRequest, DownloadFromGridResult> {

    static private Logger log = LogManager.getLogger(GridCSVDownloadWorker.class);

    @Autowired
    private GridReplicaCsvExporter gridReplicaCsvExporter;

    @Override
    public Class<DownloadFromGridRequest> getRequestType() {
        return DownloadFromGridRequest.class;
    }

    @Override
    public Class<DownloadFromGridResult> getResponseType() {
        return DownloadFromGridResult.class;
    }

    @Override
    public DownloadFromGridResult run(String jobId, UserInfo user, DownloadFromGridRequest request, AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
        try {
            return gridReplicaCsvExporter.exportGridAsCsv(jobId, user, request, jobProgressCallback);
        } catch (RecoverableMessageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Worker Failed", e);
            throw e;
        }
    }

}
