package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;

/**
 * Utilities for common chunking operations.
 * 
 * @author jmhill
 *
 */
public class ChunkUtils {

	/**
	 * The chunk part key is just the key concatenated with the part number
	 * @param token
	 * @param partNumber
	 * @return
	 */
	public static String getChunkPartKey(ChunkedFileToken token, int partNumber) {
		return token.getKey()+"/"+partNumber;
	}
	
	/**
	 * Copy an S3 object to a mutli-part upload.
	 * 
	 * @param token
	 * @param partNumber
	 * @param bucket
	 * @param s3Client
	 * @return
	 */
	public static ChunkResult copyPart(ChunkedFileToken token, int partNumber, String bucket, AmazonS3Client s3Client){
		// The part number cannot be less than one
		if(partNumber < 1) throw new IllegalArgumentException("partNumber cannot be less than one");
		String partKey = ChunkUtils.getChunkPartKey(token, partNumber);
		// copy this part to the larger file.
		CopyPartRequest copyPartRequest = new CopyPartRequest();
		copyPartRequest.setDestinationBucketName(bucket);
		copyPartRequest.setDestinationKey(token.getKey());
		copyPartRequest.setPartNumber(partNumber);
		copyPartRequest.setSourceBucketName(bucket);
		copyPartRequest.setSourceKey(partKey);
		copyPartRequest.setUploadId(token.getUploadId());
		// copy the part
		CopyPartResult result = s3Client.copyPart(copyPartRequest);
		// Now delete the original file since we now have a copy
		s3Client.deleteObject(bucket, partKey);
		ChunkResult cp = new ChunkResult();
		cp.setEtag(result.getETag());
		cp.setChunkNumber((long) result.getPartNumber());
		return cp;
	}
}
