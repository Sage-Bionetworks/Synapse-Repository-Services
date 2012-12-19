package org.sagebionetworks.repo.manager.file.transfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;

import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.BinaryUtils;

/**
 * This transfer Strategy is optimized for robustness over speed. Data is read from the stream and 
 * written to a temporary file.  The data is then transfered from the file to S3.
 * 
 * @author John
 *
 */
public class TempFileTransferStrategy implements FileTransferStrategy {
	
	@Autowired
	AmazonS3Client s3Client;
	

	/**
	 * The IoC constructor.
	 * @param s3Client
	 */
	public TempFileTransferStrategy(AmazonS3Client s3Client) {
		super();
		this.s3Client = s3Client;
	}



	@Override
	public S3FileMetadata transferToS3(TransferRequest request)	throws ServiceUnavailableException, IOException {
		// Create the result metadata from the input.
		S3FileMetadata metadata = TransferUtils.prepareS3FileMetadata(request);
		// Start a multi-part upload
		ObjectMetadata objMeta = TransferUtils.prepareObjectMetadata(request);
		// The digest will be used to calculate the MD5
		MessageDigest fullDigest = TransferUtils.createMD5Digest();
		// Create the temp file
		File tempFile = File.createTempFile("TempFileTransferStrategy", ".tmp");
		FileOutputStream fos = new FileOutputStream(tempFile);
		try{
			// First write the entire stream to a temp file.
			byte[] buffer = new byte[4096]; // 4 K buffer
			int length = -1;
			while((length = request.getInputStream().read(buffer)) > 0){
				// Write to the file
				fos.write(buffer, 0, length);
				fullDigest.update(buffer, 0, length);
			}
			// We should now have the MD5
			String contentMd5 = BinaryUtils.toHex(fullDigest.digest());
			// If they passed an MD5 does it match?
			TransferUtils.validateRequestedMD5(request, contentMd5);
		}finally{
			fos.close();
			tempFile.delete();
		}
		// We must delete this file or we will have a hard drive leak.
		if(tempFile.exists()) {
			throw new IllegalStateException("Failed to delete the tempoary file created by this file transer!  This will fill up the hardrive over time!");
		}
		return metadata;
	}

}
