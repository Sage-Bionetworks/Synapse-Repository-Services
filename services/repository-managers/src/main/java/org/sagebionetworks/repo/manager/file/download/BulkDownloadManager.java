package org.sagebionetworks.repo.manager.file.download;

import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.DownloadOrder;
import org.sagebionetworks.repo.model.file.DownloadOrderSummaryRequest;
import org.sagebionetworks.repo.model.file.DownloadOrderSummaryResponse;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
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
	 * 
	 * @param user
	 * @param query
	 * @return
	 * @throws TableFailedException
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws RecoverableMessageException
	 */
	public DownloadList addFilesFromQuery(ProgressCallback progressCallback, UserInfo user, Query query)
			throws DatastoreException, NotFoundException, TableFailedException, RecoverableMessageException;

	/**
	 * Add a list of FileHandleAssociations to a user's download list.
	 * 
	 * @param user
	 * @param toAdd
	 * @return
	 */
	public DownloadList addFileHandleAssociations(UserInfo user, List<FileHandleAssociation> toAdd);

	/**
	 * Remove a list of FileHandleAssociations from a user's download list.
	 * 
	 * @param user
	 * @param toRemove
	 * @return
	 */
	public DownloadList removeFileHandleAssociations(UserInfo user, List<FileHandleAssociation> toRemove);

	/**
	 * Get a user's download list.
	 * 
	 * @param user
	 * @return
	 */
	public DownloadList getDownloadList(UserInfo user);

	/**
	 * Clear a user's download list.
	 * 
	 * @param user
	 * @return
	 */
	public DownloadList clearDownloadList(UserInfo user);

	/**
	 * Truncate all download data for all users.
	 * 
	 * @param admin
	 */
	void truncateAllDownloadDataForAllUsers(UserInfo admin);

	/**
	 * Create a download order from the user's current download list.
	 * 
	 * @param user
	 * @param zipFileName
	 * @return
	 */
	DownloadOrder createDownloadOrder(UserInfo user, String zipFileName);

	/**
	 * Get a user's DownloadOrder history in reverse chronological order.
	 * 
	 * @param user
	 * @param request
	 * @return
	 */
	DownloadOrderSummaryResponse getDownloadHistory(UserInfo user, DownloadOrderSummaryRequest request);

	/**
	 * Get a DownloadOrder for the given orderId.
	 * 
	 * @param user
	 * @param orderId
	 * @return
	 */
	DownloadOrder getDownloadOrder(UserInfo user, String orderId);

}
