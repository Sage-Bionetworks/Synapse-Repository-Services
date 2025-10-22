package org.sagebionetworks.repo.manager.grid.internal.replica.export;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportRequest;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportResponse;

public interface GridRecordSetExporter {

	GridRecordSetExportResponse exportGrid(UserInfo user, GridRecordSetExportRequest request, AsyncJobProgressCallback jobCallback);
	
}
