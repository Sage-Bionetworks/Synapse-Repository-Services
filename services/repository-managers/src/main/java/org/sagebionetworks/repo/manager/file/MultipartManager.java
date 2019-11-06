package org.sagebionetworks.repo.manager.file;

import java.net.URL;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for multi-part support.
 * @author jmhill
 *
 */
public interface MultipartManager {

	/**
	 * Create a new ChunkedFileToken to be used for multi-part upload
	 * 
	 * @param ccftr
	 * @param userId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	ChunkedFileToken createChunkedFileUploadToken(CreateChunkedFileTokenRequest ccftr, Long storageLocationId, String userId)
			throws DatastoreException, NotFoundException;
	
	/**
	 * Copy a part to a multi-part upload.
	 * 
	 * @param token
	 * @param partNumber
	 * @param bucket
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public ChunkResult copyPart(ChunkedFileToken token, int partNumber, Long storageLocationId) throws DatastoreException, NotFoundException;
	
	/**
	 * Does a given part exist?
	 * 
	 * @param token
	 * @param partNumber
	 * @param bucket
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public boolean doesPartExist(ChunkedFileToken token, int partNumber, Long storageLocationId) throws DatastoreException, NotFoundException;
	
	/**
	 * Create a pre-signed URL that can be used for upload.
	 * 
	 * @param cpr
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public URL createChunkedFileUploadPartURL(ChunkRequest cpr, Long storageLocationId) throws DatastoreException, NotFoundException;
	
	/**
	 * Complete the chunked file upload process
	 * 
	 * @param userInfo
	 * @param ccfr
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	S3FileHandle completeChunkFileUpload(CompleteChunkedFileRequest ccfr, Long storageLocationId, String userId) throws DatastoreException,
			NotFoundException;
	
	/**
	 * Get the part key
	 * @param token
	 * @param partNumber
	 * @return
	 */
	String getChunkPartKey(ChunkedFileToken token, int partNumber);

	/**
	 * Upload local file to S3.
	 * 
	 * @param request
	 * @return
	 */
	S3FileHandle multipartUploadLocalFile(LocalFileUploadRequest request);
}
