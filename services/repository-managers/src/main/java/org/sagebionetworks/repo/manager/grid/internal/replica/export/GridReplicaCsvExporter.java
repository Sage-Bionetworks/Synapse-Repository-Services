package org.sagebionetworks.repo.manager.grid.internal.replica.export;

import java.io.IOException;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.DownloadFromGridRequest;
import org.sagebionetworks.repo.model.grid.DownloadFromGridResult;

public interface GridReplicaCsvExporter {

    /**
     * Exports the current state of the specified grid session to a CSV file.
     */
    DownloadFromGridResult exportGridAsCsv(String jobId, UserInfo user, DownloadFromGridRequest request, AsyncJobProgressCallback jobProgressCallback) throws IOException;
}
