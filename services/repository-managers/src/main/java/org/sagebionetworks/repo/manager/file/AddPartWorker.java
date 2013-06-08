package org.sagebionetworks.repo.manager.file;

import java.util.concurrent.Callable;

import org.sagebionetworks.repo.model.file.ChunkResult;

import com.amazonaws.services.s3.AmazonS3Client;

/**
 * This worker adds a part to a Multi-part upload.
 * 
 * @author jmhill
 *
 */
public class AddPartWorker implements Callable<ChunkResult> {
	
	AmazonS3Client s3Client;

	@Override
	public ChunkResult call() throws Exception {
		// S3 in US Standard is currently "eventually-consistent", this means users may have finished creating a part,
		// but it is not visible yet.  So the first step is to wait for the part to become visibile
		
		return null;
	}

}
