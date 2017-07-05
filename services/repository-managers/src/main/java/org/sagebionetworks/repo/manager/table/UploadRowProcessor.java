package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for an upload row processor used to upload CVS files to tables or view.
 * 
 *
 */
public interface UploadRowProcessor {

	/**
	 * Process the rows of a CSV to be uploaded to a table or view.
	 * 
	 * @param user
	 * @param tableId
	 * @param tableSchema
	 * @param iterator
	 * @param updateEtag
	 * @param progressCallback
	 * @return Resulting etag of the last row change applied to the table.
	 * @throws IOException 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * 
	 */
	TableUpdateResponse processRows(UserInfo user, String tableId,
			List<ColumnModel> tableSchema, Iterator<SparseRowDto> rowStream,
			String updateEtag,
			ProgressCallback progressCallback) throws DatastoreException, NotFoundException, IOException;

}
