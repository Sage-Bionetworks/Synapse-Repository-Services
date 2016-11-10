package org.sagebionetworks.repo.manager.table;

import java.io.IOException;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;

public interface TableUploadManager {

	/**
	 * Upload a CSV to a table or view.
	 * 
	 * @param progressCallback
	 * @param user
	 * @param request
	 * @param rowProcessor
	 * @return
	 * @throws IOException
	 */
	public UploadToTableResult uploadCSV(ProgressCallback<Void> progressCallback, UserInfo user, UploadToTableRequest request, UploadRowProcessor rowProcessor);

}
