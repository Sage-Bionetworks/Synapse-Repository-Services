package org.sagebionetworks.repo.model.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.web.FileHandleLinkedException;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.collect.Multimap;

/**
 * Abstraction for creating/updating/reading/deleting CRUD metadata about files. 
 * 
 * @author John
 *
 */
public interface FileHandleDao {
	
	/**
	 * Create file metadata.
	 */
	public FileHandle createFile(FileHandle fileHandle);

	/**
	 * Set the preview ID of a file.
	 * @param fileId
	 * @param previewId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void setPreviewId(String fileId, String previewId) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the file metadata by ID.
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public FileHandle get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * Get all of the file handles for a given list of IDs.
	 * @param ids - The list of FileHandle ids to fetch.
	 * @param includePreviews - When true, any preview handles will associated with each handle will also be returned.
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public FileHandleResults getAllFileHandles(Iterable<String> ids, boolean includePreviews) throws DatastoreException, NotFoundException;

	/**
	 * Map all of the file handles for a given list of IDs in batch calls
	 * 
	 * @param ids - The list of FileHandle ids to fetch.
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public Map<String, FileHandle> getAllFileHandlesBatch(Iterable<String> idsList);

	/**
	 * Delete the file metadata.
	 * @param id
	 * @throws FileHandleLinkedException If the file handle is still linked to some object through a FK that restricts its deletion
	 */
	public void delete(String id);
	
	/**
	 * Does the given file object exist?
	 * @param id
	 * @return true if it exists.
	 */
	public boolean doesExist(String id);

	/**
	 * Lookup the creator of a FileHandle.
	 * @param fileHandleId
	 * @return
	 * @throws NotFoundException 
	 */
	public String getHandleCreator(String fileHandleId) throws NotFoundException;

	/**
	 * Lookup the creators of a FileHandles.
	 * 
	 * @param fileHandleIds
	 * @return the list of creators in the same order as the file handles
	 * @throws NotFoundException
	 */
	@Deprecated
	public Multimap<String, String> getHandleCreators(List<String> fileHandleIds) throws NotFoundException;
	

	/**
	 * Given a list of FileHandleIds, get the sub-set of FileHandleIds of the FileHandles that 
	 * were created by the passed userId.
	 * @param createdById
	 * @param fileHandleIds
	 * @return
	 * @throws NotFoundException
	 */
	public Set<String> getFileHandleIdsCreatedByUser(Long createdById, List<String> fileHandleIds) throws NotFoundException;
	
	/**
	 * Given a list of {@link FileHandle} ids, gets the sub-set of ids that are previews mapped to the originating file handle id.
	 * 
	 * @param fileHandlePreviewIds A list of ids of {@link FileHandle}
	 * @return A map where each entry is a (fileHandlePreviewId, fileHandleId) entry which is subset of the input fileHandlePreviewIds.
	 */
	public Map<String, String> getFileHandlePreviewIds(List<String> fileHandlePreviewIds);
	
	/**
	 * Get the preview associated with a given file handle.
	 * 
	 * @param handleId
	 * @return
	 */
	public String getPreviewFileHandleId(String handleId) throws NotFoundException;

	/**
	 * Get the number of file handles referencing this bucket and key
	 * 
	 * @param bucketName
	 * @param key
	 * @return
	 */
	public long getNumberOfReferencesToFile(FileHandleMetadataType metadataType, String bucketName, String key);

	long getCount() throws DatastoreException;

	long getMaxId() throws DatastoreException;

	/**
	 * Create a batch of FileHandle
	 * 
	 * @param toCreate
	 */
	public void createBatch(List<FileHandle> toCreate);
	
	/**
	 * Checks if the MD5 of the file handles identified by the given ids is not null and matches.
	 * 
	 * @param sourceFileHandleId The id of the source file handle
	 * @param targetFileHandleId The id of the target file handle
	 * @return True if the MD5 of the two file handles is not null and matches
	 */
	boolean isMatchingMD5(String sourceFileHandleId, String targetFileHandleId);
	
	/**
	 * Deleted all file data
	 */
	public void truncateTable();
	
}
