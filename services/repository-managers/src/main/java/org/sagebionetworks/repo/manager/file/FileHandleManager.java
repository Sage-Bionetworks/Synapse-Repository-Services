package org.sagebionetworks.repo.manager.file;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
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

import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;

/**
 * Manages uploading files.
 * 
 * @author John
 *
 */
public interface FileHandleManager {

	/**
	 * The ID used for the CORS rule on the data bucket.
	 * 
	 */
	public static final String AUTO_GENERATED_ALLOW_ALL_CORS_RULE_ID = "auto-generated-allow-all-CORS";
	
	/**
	 * Upload all of the files found in a request and capture all of the parameters and meta-data.
	 * 
	 * @param userInfo - The User's information.
	 * @param expectedParams - If any of the expected parameters are missing by the time we get to a file then an IllegalArgumentException will be thrown.
	 * @param itemIterator - Iterates over all of the parts.
	 * @return FileUploadResults
	 * @throws IOException 
	 * @throws FileUploadException 
	 * @throws ServiceUnavailableException 
	 */
	FileUploadResults uploadfiles(UserInfo userInfo, Set<String> expectedParams, FileItemIterator itemIterator) throws FileUploadException, IOException, ServiceUnavailableException;
	
	/**
	 * Upload a file.
	 * @param userId
	 * @param fis
	 * @return
	 * @throws IOException
	 * @throws ServiceUnavailableException
	 */
	public S3FileHandle uploadFile(String userId, FileItemStream fis)	throws IOException, ServiceUnavailableException;

	/**
	 * Get a file handle for a user.
	 * Note: Only the creator of the FileHandle can access it.
	 * @param userInfo
	 * @param handleId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FileHandle getRawFileHandle(UserInfo userInfo, String handleId) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the FileHandle ID of a preview associated with a file handle.
	 * @param handleId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	String getPreviewFileHandleId(String handleId) throws DatastoreException, NotFoundException;
	
	/**
	 * Clear the preview associated with a file handle
	 * @param userInfo
	 * @param handleId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void clearPreview(UserInfo userInfo, String handleId) throws DatastoreException, NotFoundException;
	
	/**
	 * Delete a file handle
	 * @param userInfo
	 * @param handleId
	 * @throws DatastoreException
	 */
	void deleteFileHandle(UserInfo userInfo, String handleId) throws DatastoreException;
	
	/**
	 * Get the redirect URL for a given FileHandle ID.  The UserInfo is not needed as Authorization should have already been
	 * checked before attempting this call.
	 * @param handleId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws MalformedURLException 
	 */
	URL getRedirectURLForFileHandle(String handleId) throws DatastoreException, NotFoundException;

	/**
	 * Get all file handles on the list.
	 * @param idsList
	 * @param includePreviews - When true, preview file handles will be included in the resutls.
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FileHandleResults getAllFileHandles(List<String> idsList, boolean includePreviews) throws DatastoreException, NotFoundException;

	/**
	 * Create an external file handle.
	 * @param userInfo
	 * @param fileHandle
	 * @return
	 */
	ExternalFileHandle createExternalFileHandle(UserInfo userInfo,	ExternalFileHandle fileHandle);
	
	/**
	 * Ge the bucket CORS settings
	 * @return
	 */
	BucketCrossOriginConfiguration getBucketCrossOriginConfiguration();

	/**
	 * This is the first step in uploading a file as multiple chunks. The returned file token must be provide in all subsequent chunk calls.
	 * @param userInfo
	 * @return
	 */
	public ChunkedFileToken createChunkedFileUploadToken(UserInfo userInfo, CreateChunkedFileTokenRequest ccftr);
	
	/**
	 * Create a pre-signed URL that can be used to PUT a single chunk of file data to S3.
	 * @param token
	 * @param partNumber
	 * @return
	 */
	public URL createChunkedFileUploadPartURL(UserInfo userInfo, ChunkRequest cpr);
	
	/**
	 * After uploading a file chunk to the pre-signed URL add it to the larger file.
	 * This must be called for each chunk.
	 * @param userInfo
	 * @param token
	 * @param partNumber
	 * @return
	 */
	public ChunkResult addChunkToFile(UserInfo userInfo, ChunkRequest cpr);

	/**
	 * The final step of a chunked file upload.  This is where an {@link S3FileHandle} is created.
	 * @param userInfo
	 * @param token
	 * @param partList
	 * @return
	 */
	public S3FileHandle completeChunkFileUpload(UserInfo userInfo,	CompleteChunkedFileRequest ccfr);
	
	/**
	 * Start an asynchronous daemon that will add all chunks to the file upload and complete the file upload
	 * process. 
	 * @param userInfo
	 * @param cacf
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public UploadDaemonStatus startUploadDeamon(UserInfo userInfo, CompleteAllChunksRequest cacf) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the status of an asynchronous daemon stated with {@link #startUploadDeamon(UserInfo, CompleteAllChunksRequest)}
	 * @param userInfo
	 * @param daemonId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public UploadDaemonStatus getUploadDaemonStatus(UserInfo userInfo, String daemonId) throws DatastoreException, NotFoundException;
	
}
