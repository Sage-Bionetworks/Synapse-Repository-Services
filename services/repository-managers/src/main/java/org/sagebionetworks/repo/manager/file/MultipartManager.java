package org.sagebionetworks.repo.manager.file;

import java.net.URL;

import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;

/**
 * Abstraction for multi-part support.
 * @author jmhill
 *
 */
public interface MultipartManager {

	/**
	 * Create a new ChunkedFileToken to be used for multi-part upload
	 * @param ccftr
	 * @param userId
	 * @return
	 */
	ChunkedFileToken createChunkedFileUploadToken(CreateChunkedFileTokenRequest ccftr, String bucket, String userId);
	/**
	 * Copy a part to a multi-part upload.
	 * @param token
	 * @param partNumber
	 * @param bucket
	 * @return
	 */
	public ChunkResult copyPart(ChunkedFileToken token, int partNumber, String bucket);
	
	/**
	 * Does a given part exist?
	 * @param token
	 * @param partNumber
	 * @param bucket
	 * @return
	 */
	public boolean doesPartExist(ChunkedFileToken token, int partNumber, String bucket);
	
	/**
	 * Create a pre-signed URL that can be used for upload.
	 * @param cpr
	 * @return
	 */
	public URL createChunkedFileUploadPartURL(ChunkRequest cpr, String bucket);
	
	/**
	 * Complete the chunked file upload process
	 * @param userInfo
	 * @param ccfr
	 * @return
	 */
	S3FileHandle completeChunkFileUpload(CompleteChunkedFileRequest ccfr, String bucket, String userId);
	
	/**
	 * Get the part key
	 * @param token
	 * @param partNumber
	 * @return
	 */
	String getChunkPartKey(ChunkedFileToken token, int partNumber);
}
