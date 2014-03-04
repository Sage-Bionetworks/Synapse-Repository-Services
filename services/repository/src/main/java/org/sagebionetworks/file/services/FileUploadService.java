package org.sagebionetworks.file.services;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUploadException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

/**
 * Abstraction for the handling multi-part upload.
 * @author John
 *
 */
public interface FileUploadService {

	
	/**
	 * Upload all files for this request.
	 * @param userName
	 * @param itemIterator
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 * @throws FileUploadException 
	 * @throws ServiceUnavailableException 
	 */
	FileHandleResults uploadFiles(Long userId, FileItemIterator itemIterator) throws DatastoreException, NotFoundException, FileUploadException, IOException, ServiceUnavailableException;

	/**
	 * Get a file handle by ID.
	 * @param handleId
	 * @param userId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FileHandle getFileHandle(String handleId, Long userId) throws DatastoreException, NotFoundException;

	/**
	 * Delete a file handle.
	 * @param handleId
	 * @param userId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	void deleteFileHandle(String handleId, Long userId) throws DatastoreException, NotFoundException;

	/**
	 * Delete the preview associated with the given file handle (causes the preview generator worker to recreate).
	 * @param handleId
	 * @param userId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	void clearPreview(String handleId, Long userId) throws DatastoreException, NotFoundException;
	
	/**
	 * Create an external file Handle.
	 * @param userId
	 * @param fileHandle
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	ExternalFileHandle createExternalFileHandle(Long userId,	ExternalFileHandle fileHandle) throws DatastoreException, NotFoundException;

	/**
	 * Create a chunked file upload token that can be used to upload large files to S3.
	 * 
	 * @param userId
	 * @param ccftr
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	ChunkedFileToken createChunkedFileUploadToken(Long userId, CreateChunkedFileTokenRequest ccftr) throws DatastoreException, NotFoundException;
	
	/**
	 * Creates a pre-signed URL that can be used PUT file data to S3.
	 * @param userId
	 * @param cpr
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	URL createChunkedFileUploadPartURL(Long userId, ChunkRequest cpr) throws DatastoreException, NotFoundException;

	/**
	 * After upload a file chunk to a pre-signed URL, the part must be added to the final file.
	 * @param userId
	 * @param cpr
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	ChunkResult addChunkToFile(Long userId, ChunkRequest cpr) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userId
	 * @param ccfr
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	S3FileHandle completeChunkFileUpload(Long userId, CompleteChunkedFileRequest ccfr) throws DatastoreException, NotFoundException;
	
	/**
	 * Start an asynchronous daemon that will add all chunks to the file upload and complete the file upload
	 * process. 
	 * @param userInfo
	 * @param cacf
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public UploadDaemonStatus startUploadDeamon(Long userId, CompleteAllChunksRequest cacf) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the status of an asynchronous daemon stated with {@link #startUploadDeamon(UserInfo, CompleteAllChunksRequest)}
	 * @param userInfo
	 * @param daemonId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public UploadDaemonStatus getUploadDaemonStatus(Long userId, String daemonId) throws DatastoreException, NotFoundException;
	

}
