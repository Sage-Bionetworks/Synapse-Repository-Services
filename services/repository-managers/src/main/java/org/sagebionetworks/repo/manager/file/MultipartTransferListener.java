package org.sagebionetworks.repo.manager.file;

/**
 * Allows a caller to listen to the progress of a mutli-part upload.
 * @author John
 *
 */
public interface MultipartTransferListener {
	
	/**
	 * Called when progress is mad.
	 * @param bytesTransfered The number of bytes completed in the associated transfer.
	 * @param totalBytesToTransfer The total size in bytes of the associated transfer, or -1
     * if the total size isn't known
	 */
	public void transferProgress(long bytesTransfered, long totalBytesToTransfer);

}
