package org.sagebionetworks.repo.manager.table;

import java.io.IOException;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.util.progress.ProgressCallback;

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
	public TableUpdateResponse uploadCSV(ProgressCallback progressCallback, UserInfo user, UploadToTableRequest request, UploadRowProcessor rowProcessor);

}
