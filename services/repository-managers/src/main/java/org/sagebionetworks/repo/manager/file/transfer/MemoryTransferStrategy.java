package org.sagebionetworks.repo.manager.file.transfer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.util.FixedMemoryPool;
import org.sagebionetworks.repo.util.FixedMemoryPool.BlockConsumer;
import org.sagebionetworks.repo.util.FixedMemoryPool.NoBlocksAvailableException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.util.BinaryUtils;

/**
 * <p>
 * This FileTransferStrategy implementation is optimized for speed.  It will use a constant 5 MB of
 * memory buffer to transfer a single file regardless of the size of the file.
 * </p><p>
 * Since we are currently running on a fleet of Amazon EC2 Small instances, and
 * these instance types only have a moderate I/O performance 
 * <a href="http://aws.amazon.com/ec2/instance-types/">ec2-instance-types</a>, this strategy
 * does not depend on file I/O.  Instead the stream is read into a large memory buffer (5 MB)
 * then transfered from the buffer to S3 as a part of a multi-part upload.  This process is repeated
 * until the entire file has been transfered to S3.
 * </p><p>
 * Note: The buffer size is set to 5 MB because currently that is the smallest part 
 * supported by S3 multi-part upload.
 * </p><p>
 * Q: Why not just pass the stream directly, to AmazonS3Client.putObject() ?
 * </p><p>
 * A: We do not know the size of the file before we start reading. Currently, if
 * you call AmazonS3Client.putObject() without providing the "Content-Length", the 
 * S3 client will attempt to load the entire InputStream into memory. This could easily 
 * crash the server with an OutOfMemoryError.
 * </p>
 * @author John
 *
 */
public class MemoryTransferStrategy implements FileTransferStrategy {
	
	static private Log log = LogFactory.getLog(MemoryTransferStrategy.class);
	
	/**
	 * Note: 5 MB is currently the minimum size of a single part of S3 Multi-part upload.
	 */
	public static final int MINIMUM_BLOCK_SIZE_BYTES = ((int) Math.pow(2, 20))*5;
	
	@Autowired
	AmazonS3Client s3Client;
	
	/**
	 * This pool ensure we never use more memory than planned.
	 */
	@Autowired
	FixedMemoryPool fileTransferFixedMemoryPool;

	/**
	 * Used by spring.
	 */
	public MemoryTransferStrategy(){
		
	}

	/**
	 * IoC constructor.
	 * 
	 * @param s3Client
	 * @param fixedMemoryPool
	 */
	public MemoryTransferStrategy(AmazonS3Client s3Client,	FixedMemoryPool fixedMemoryPool) {
		super();
		this.s3Client = s3Client;
		this.fileTransferFixedMemoryPool = fixedMemoryPool;
	}

	@Override
	public S3FileHandle transferToS3(final TransferRequest request) throws ServiceUnavailableException {
		try {
			// Attempt to allocate a block of memory from the pool.  
			return fileTransferFixedMemoryPool.checkoutAndUseBlock(new BlockConsumer<S3FileHandle>(){

				@Override
				public S3FileHandle useBlock(byte[] block) throws Exception{
					// A block was successfully checked-out from the pool so we can proceed with the transfer.
					return transferToS3(request, block);
				}});
		} catch (NoBlocksAvailableException e1) {
			// This means there is no more memory for us to use.
			throw new ServiceUnavailableException("Could not check-out a memory block ");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	
	}
	
	/**
	 * Transfer the passed request to S3 using the passed buffer.
	 * @param request
	 * @param buffer
	 * @return
	 * @throws AmazonServiceException
	 * @throws AmazonClientException
	 * @throws IOException
	 */
	S3FileHandle transferToS3(TransferRequest request, byte[] buffer) throws IOException{
		if(buffer == null) throw new IllegalArgumentException("The buffer cannot be null");
		if(buffer.length < MINIMUM_BLOCK_SIZE_BYTES) throw new IllegalArgumentException("The buffer cannot be less than 5 MB as that is the miniumn size of a single part in a S3 multi-part upload.");
		// Create the result metadata from the input.
		S3FileHandle metadata = TransferUtils.prepareS3FileMetadata(request);
		// Start a multi-part upload
		ObjectMetadata objMeta = TransferUtils.prepareObjectMetadata(request);
		if(log.isDebugEnabled()){
			log.debug("Starting multi-part upload: "+metadata.toString());
		}
		// Start the upload.
		InitiateMultipartUploadResult initiate = s3Client.initiateMultipartUpload(new InitiateMultipartUploadRequest(metadata.getBucketName(), metadata.getKey(), objMeta));
		// Now read the stream
		// each part upload.
		// Use the digest to calculate the MD5
		MessageDigest fullDigest = TransferUtils.createMD5Digest();
		MessageDigest partDigest = TransferUtils.createMD5Digest();
		List<PartETag> partETags = new ArrayList<PartETag>();
		int read = -1;
		int partNumber = 1;
		long contentSize = 0;
		// First we need to fill up the memory buffer.
		while ((read = fillBufferFromStream(buffer, request.getInputStream())) > 0) {
			// Update the MD5 calculation of the entire file
			fullDigest.update(buffer, 0, read);
			// Calculate the MD5 of the part
			partDigest.reset();
			partDigest.update(buffer, 0, read);
			String partMD5 = BinaryUtils.toBase64(partDigest.digest());
			// Upload this part
			ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
			long start = System.currentTimeMillis();
			UploadPartResult partResult = s3Client
					.uploadPart(new UploadPartRequest()
							.withUploadId(initiate.getUploadId())
							.withBucketName(initiate.getBucketName())
							.withKey(initiate.getKey())
							.withPartSize(read)
							.withMD5Digest(partMD5)
							.withInputStream(bais).withPartNumber(partNumber));
			// Add to the list of etags
			partETags.add(partResult.getPartETag());
			// Increment the part number
			partNumber++;
			contentSize += read;
			long elapseMS = System.currentTimeMillis()-start;
			if(log.isDebugEnabled()){
				log.debug("Sent a part of size: "+read+" bytes in "+elapseMS+" MS with a MD5: "+partMD5+" total bytes transfered: "+contentSize);
			}
		}
		// Get the MD5
		String contentMd5 = BinaryUtils.toHex(fullDigest.digest());	
		if(log.isDebugEnabled()){
			log.debug("Completing multi-part upload with final MD5: "+contentMd5);
		}
		// Complete the Upload
		s3Client.completeMultipartUpload(new CompleteMultipartUploadRequest(
						initiate.getBucketName(), initiate.getKey(), initiate
								.getUploadId(), partETags));
		
		// Validate the MD5.
		try{
			TransferUtils.validateRequestedMD5(request, contentMd5);
		}catch(IllegalArgumentException e){
			// MD5 Validation failed so delete the file we created in S3
			s3Client.deleteObject(initiate.getBucketName(), initiate.getKey());
			throw e;
		}

		// Set the MD5 Of the file.
		metadata.setContentMd5(contentMd5);
		// Set the file size
		metadata.setContentSize(contentSize);
		return metadata;
	}

	/**
	 * Fill the passed buffer from the passed input stream.
	 * @param buffer
	 * @param in
	 * @return the number of bytes written to the buffer.
	 * @throws IOException 
	 */
	public static int fillBufferFromStream(byte[] buffer, InputStream in) throws IOException{
		int totalRead = 0;
		int read;
		while((read = in.read(buffer, totalRead, buffer.length-totalRead)) > 0){
			totalRead += read;
		}
		return totalRead;
	}

}
