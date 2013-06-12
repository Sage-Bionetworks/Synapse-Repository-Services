package org.sagebionetworks.repo.manager.file;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;

/**
 * This worker performs two tasks on a single part of a muti-part upload.
 * First we might need to wait for this part to be readable in S3.
 * This is necessary because S3 in the Standard US is eventually consistent.
 * So there can be a lag between when the caller uploads the file to S3 and when it
 * is visible to be added to the multi-part.
 * Once the part is visible it will be copied to the muti-part upload.
 * 
 * @author jmhill
 *
 */
public class CopyPartWorker implements Callable<ChunkResult> {

	static private Log log = LogFactory.getLog(CopyPartWorker.class);
	
	MultipartManager multipartManager;
	ChunkedFileToken token;
	int partNumber;
	String bucket;
	long maxWaitMS;
	
	/**
	 * A new instances should be created for each part to copy.
	 * @param multipartManager
	 * @param token
	 * @param partNumber
	 * @param bucket
	 * @param maxWaitMS
	 */
	public CopyPartWorker(MultipartManager multipartManager,
			ChunkedFileToken token, int partNumber, String bucket,
			long maxWaitMS) {
		super();
		this.multipartManager = multipartManager;
		this.token = token;
		this.partNumber = partNumber;
		this.bucket = bucket;
		this.maxWaitMS = maxWaitMS;
	}


	@Override
	public ChunkResult call() throws Exception {
		// First we might need to wait for this part to be readable in S3.
		// This is necessary because S3 in the Standard US is eventually consistent.
		// So there can be a lag between when the caller uploads the file to S3 and when it
		// is visible to be added to the multi-part.
		long start = System.currentTimeMillis();
		while(!multipartManager.doesPartExist(token, partNumber, bucket)){
			log.debug("Waiting for S3 key to become visible.  Key: "+token.getKey()+"/"+partNumber+" bucket: "+bucket);
			Thread.sleep(1000);
			if(System.currentTimeMillis() - start > maxWaitMS){
				throw new RuntimeException("Timed out waiting for S3 object to become visible:  Key: "+token.getKey()+"/"+partNumber+" bucket: "+bucket+" token: "+token.toString());
			}
		}
		// Add the part to the multi-part upload.
		return multipartManager.copyPart(token, partNumber, bucket);
	}

}
