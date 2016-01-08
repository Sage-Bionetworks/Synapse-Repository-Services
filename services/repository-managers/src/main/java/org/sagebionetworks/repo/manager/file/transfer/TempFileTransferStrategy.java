package org.sagebionetworks.repo.manager.file.transfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;

import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.TempFileProvider;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
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
	
	@Autowired
	TempFileProvider tempFileProvider;
	
	/**
	 * Used by Spring
	 */
	public TempFileTransferStrategy(){}
	/**
	 * The IoC constructor.
	 * @param s3Client
	 */
	public TempFileTransferStrategy(AmazonS3Client s3Client, TempFileProvider tempFileProvider) {
		super();
		this.s3Client = s3Client;
		this.tempFileProvider = tempFileProvider;
	}



	@Override
	public S3FileHandle transferToS3(TransferRequest request)	throws ServiceUnavailableException, IOException {
		// Create the result metadata from the input.
		S3FileHandle metadata = TransferUtils.prepareS3FileMetadata(request);
		// Start a multi-part upload
		ObjectMetadata objMeta = TransferUtils.prepareObjectMetadata(request);
		// The digest will be used to calculate the MD5
		MessageDigest fullDigest = TransferUtils.createMD5Digest();
		// Create the temp file
		File tempFile = tempFileProvider.createTempFile("TempFileTransferStrategy", ".tmp");
		FileOutputStream fos = tempFileProvider.createFileOutputStream(tempFile);
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
			// Set the MD5 Of the file.
			metadata.setContentMd5(contentMd5);
			// Set the file size
			metadata.setContentSize(tempFile.length());
			// Transfer the file to s3.
			s3Client.putObject(new PutObjectRequest(request.getS3bucketName(), request.getS3key(), tempFile).withMetadata(objMeta));
		}finally{
			try{
				fos.close();
			}finally{
				// we still need to delete the file even if close failed.
				tempFile.delete();
			}
		}
		return metadata;
	}

}
