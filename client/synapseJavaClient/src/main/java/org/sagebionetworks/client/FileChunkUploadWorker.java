package org.sagebionetworks.client;

import java.io.File;
import java.net.URL;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.file.ChunkRequest;

/**
 * This worker will upload a single chunk.
 * 
 * If it fails it will try a total of 3 times to finish before failing permanently.
 * 
 * @author John
 *
 */
public class FileChunkUploadWorker implements Callable<Long> {
	
	protected static final Logger log = Logger.getLogger(FileChunkUploadWorker.class.getName());

	Synapse client;
	ChunkRequest request;
	File chunk;
	
	
	/**
	 * The IoC constructor
	 * @param client
	 * @param token
	 * @param request
	 * @param chunk
	 */
	public FileChunkUploadWorker(Synapse client, ChunkRequest request, File chunk) {
		super();
		this.client = client;
		this.request = request;
		this.chunk = chunk;
	}



	@Override
	public Long call() throws Exception {
		try{
			// The first try
			return tryUpload();
		}catch(Exception e){
			log.error("First attempt to upload a file chunk failed with Request: "+request, e);
			try{
				// Sleep and try again
				Thread.sleep(1000);
				return tryUpload();
			}catch(Exception e2){
				log.error("Second attempt to upload a file chunk failed with Request: "+request, e2);
				// This is our final try
				// Sleep and try again
				Thread.sleep(2000);
				return tryUpload();
			}
		}
	}


	/**
	 * Attempt the upload
	 * @return
	 * @throws SynapseException
	 */
	private Long tryUpload() throws SynapseException {
		log.info("Attempting to upload: "+request);
		// First get a pre-signed URL for this chunk
		long start = System.currentTimeMillis();
		URL url = client.createChunkedPresignedUrl(request);
		log.info("createChunkedPresignedUrl() in "+(System.currentTimeMillis()-start)+" ms"); 
		// Put the file to the URL
		start = System.currentTimeMillis();
		client.putFileToURL(url, chunk, request.getChunkedFileToken().getContentType());
		log.info("putFileToURL() in "+(System.currentTimeMillis()-start)+" ms"); 
		return request.getChunkNumber();
	}

}
