package org.sagebionetworks.repo.manager.file.download;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

public interface BulkDownloadManager {

	/**
	 * Add all of the files from the given folder to a user's download list.
	 * 
	 * @param user
	 * @param folderId
	 */
	public DownloadList addFilesFromFolder(UserInfo user, String folderId);
	
	/**
	 * Add all of the files from the given view query.
	 * @param user
	 * @param query
	 * @return
	 * @throws TableFailedException 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws RecoverableMessageException 
	 */
	public DownloadList addFilesFromQuery(UserInfo user, Query query) throws DatastoreException, NotFoundException, TableFailedException, RecoverableMessageException;

}
