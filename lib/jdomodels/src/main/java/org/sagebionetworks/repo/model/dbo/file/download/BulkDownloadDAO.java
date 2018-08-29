package org.sagebionetworks.repo.model.dbo.file.download;

import java.util.List;

import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.DownloadOrder;
import org.sagebionetworks.repo.model.file.DownloadOrderSummary;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;

/**
 * Abstraction for bulk download database operations.
 * @author jhill
 *
 */
public interface BulkDownloadDAO {
	
	/**
	 * Get a user's download list.
	 * 
	 * @param ownerPrincipalId
	 * @return
	 */
	public DownloadList getUsersDownloadList(String ownerPrincipalId);
	
	/**
	 * Add the given files to the user's download list.
	 * 
	 * @param ownerPrincipalId
	 * @param toAdd The list of files to add to the user's list.
	 * @return
	 */
	public DownloadList addFilesToDownloadList(String ownerPrincipalId, List<FileHandleAssociation> toAdd);
	
	/**
	 * Remove the given files from the user's download list.
	 * 
	 * @param ownerPrincipalId
	 * @param toRemove List of files to remove from the user's list.
	 * @return
	 */
	public DownloadList removeFilesFromDownloadList(String ownerPrincipalId, List<FileHandleAssociation> toRemove);
	
	/**
	 * Clear all files from a user's download list.
	 * 
	 * @param ownerPrincipalId
	 * @return
	 */
	public DownloadList clearDownloadList(String ownerPrincipalId);
	
	/**
	 * Get the total number of files in a user's download list.
	 * 
	 * @param ownerPrincipalId
	 * @return
	 */
	public long getDownloadListFileCount(String ownerPrincipalId);

	/**
	 * Bump the etag and updatedOn of the given user's download list.
	 * 
	 * @param principalId
	 */
	public void touchUsersDownloadList(long principalId);
	
	/**
	 * Truncate the download lists for all users.
	 * 
	 */
	public void truncateAllDownloadDataForAllUsers();
	
	/**
	 * Create a download order.
	 * @param toCreate
	 * @return
	 */
	public DownloadOrder createDownloadOrder(DownloadOrder toCreate);
	
	/**
	 * Get the history of a user's download orders in reverse chronological order.
	 * @param ownerPrincipalId
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<DownloadOrderSummary> getUsersDownloadOrders(String ownerPrincipalId, Long limit, Long offset);
	
	
	/**
	 * Get a Download order from its ID.
	 * @param orderId
	 * @return
	 */
	public DownloadOrder getDownloadOrder(String orderId);
	

}
