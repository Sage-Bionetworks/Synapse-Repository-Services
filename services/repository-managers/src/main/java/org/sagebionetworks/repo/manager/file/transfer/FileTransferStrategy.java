package org.sagebionetworks.repo.manager.file.transfer;

import java.io.IOException;

import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.web.ServiceUnavailableException;


/**
 * Abstraction for a strategy that transfers a file from an InputStream of unknown size to S3.
 * 
 * @author John
 *
 */
public interface FileTransferStrategy {


	/**
	 * Transfer a file from the given InputStream to S3.
	 * @param request The parameters of the request.
	 * @return
	 * @throws ServiceUnavailableException - This should be thrown if the strategy cannot be executed.
	 * @throws IOException 
	 */
	public S3FileMetadata transferToS3(TransferRequest request) throws ServiceUnavailableException, IOException ;
}
