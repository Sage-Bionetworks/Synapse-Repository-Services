package org.sagebionetworks.repo.model.dbo.file;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.BucketAndKey;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.FileHandleMetadataType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.FileHandleStatus;
import org.sagebionetworks.repo.web.FileHandleLinkedException;
import org.sagebionetworks.repo.web.NotFoundException;

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
	FileHandle createFile(FileHandle fileHandle);

	/**
	 * Set the preview ID of a file.
	 * @param fileId
	 * @param previewId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	void setPreviewId(String fileId, String previewId) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the file metadata by ID.
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FileHandle get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * Get all of the file handles for a given list of IDs.
	 * @param ids - The list of FileHandle ids to fetch.
	 * @param includePreviews - When true, any preview handles will associated with each handle will also be returned.
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FileHandleResults getAllFileHandles(Iterable<String> ids, boolean includePreviews) throws DatastoreException, NotFoundException;

	/**
	 * Map all of the file handles for a given list of IDs in batch calls
	 * 
	 * @param ids - The list of FileHandle ids to fetch.
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	Map<String, FileHandle> getAllFileHandlesBatch(Iterable<String> idsList);

	/**
	 * Delete the file metadata.
	 * @param id
	 * @throws FileHandleLinkedException If the file handle is still linked to some object through a FK that restricts its deletion
	 */
	void delete(String id);
	
	/**
	 * Does the given file object exist?
	 * @param id
	 * @return true if it exists.
	 */
	boolean doesExist(String id);

	/**
	 * Lookup the creator of a FileHandle.
	 * @param fileHandleId
	 * @return
	 * @throws NotFoundException 
	 */
	String getHandleCreator(String fileHandleId) throws NotFoundException;
	

	/**
	 * Given a list of FileHandleIds, get the sub-set of FileHandleIds of the FileHandles that 
	 * were created by the passed userId.
	 * @param createdById
	 * @param fileHandleIds
	 * @return
	 * @throws NotFoundException
	 */
	Set<String> getFileHandleIdsCreatedByUser(Long createdById, List<String> fileHandleIds) throws NotFoundException;
	
	/**
	 * Given a list of {@link FileHandle} ids, gets the sub-set of ids that are previews mapped to the originating file handle id.
	 * 
	 * @param fileHandlePreviewIds A list of ids of {@link FileHandle}
	 * @return A map where each entry is a (fileHandlePreviewId, fileHandleId) entry which is subset of the input fileHandlePreviewIds.
	 */
	Map<String, String> getFileHandlePreviewIds(List<String> fileHandlePreviewIds);
	
	/**
	 * Get the preview associated with a given file handle.
	 * 
	 * @param handleId
	 * @return
	 */
	String getPreviewFileHandleId(String handleId) throws NotFoundException;

	/**
	 * Get the number of file handles referencing this bucket and key
	 * 
	 * @param bucketName
	 * @param key
	 * @return
	 */
	long getNumberOfReferencesToFile(FileHandleMetadataType metadataType, String bucketName, String key);

	long getCount() throws DatastoreException;

	long getMaxId() throws DatastoreException;

	/**
	 * Create a batch of FileHandle
	 * 
	 * @param toCreate
	 */
	void createBatch(List<FileHandle> toCreate);
	
	/**
	 * Checks if the MD5 of the file handles identified by the given ids is not null and matches.
	 * 
	 * @param sourceFileHandleId The id of the source file handle
	 * @param targetFileHandleId The id of the target file handle
	 * @return True if the MD5 of the two file handles is not null and matches
	 */
	boolean isMatchingMD5(String sourceFileHandleId, String targetFileHandleId);
	
	/**
	 * 
	 * @param ids The batch of ids to check
	 * @param status The status to check against
	 * @return True if any file handle in the given batch has the given status
	 */
	boolean hasStatusBatch(List<Long> ids, FileHandleStatus status);
	
	/**
	 * Updates the status of the given batch of file handle ids for any that is in the given currentStatus and were modified before the given number of days
	 * 
	 * @param ids The batch of ids to update
	 * @param newStatus The new status to assign
	 * @param currentStatus The current status for the update to apply
	 * @param updatedOnBeforeDays Does not update file handles that were modified after the given number of days. If 0 no filter is applied.
	 * 
	 * @return The list of file handle ids that were actually modified
	 */
	List<Long> updateStatusForBatch(List<Long> ids, FileHandleStatus newStatus, FileHandleStatus currentStatus, int updatedOnBeforeDays);
	
	/**
	 * Updates the status of all the file handles matching the given bucket, key and status whose modifedOn is before the given instant
	 * 
	 * @param bucketName The bucket name
	 * @param key The key
	 * @param newStatus The new status to set
	 * @param currentStatus The current status
	 * @param modifiedBefore The upper bound for modifiedOn
	 * @return The number of updated file handles
	 */
	int updateStatusByBucketAndKey(String bucketName, String key, FileHandleStatus newStatus, FileHandleStatus currentStatus, Instant modifiedBefore);
	
	/**
	 * Get the given list of file handles, filtering those that are not in the given status
	 * 
	 * @param ids The batch of file handles
	 * @param status The status to filter by
	 * @return
	 */
	List<FileHandle> getFileHandlesBatchByStatus(List<Long> ids, FileHandleStatus status);
	
	/**
	 * @param ids
	 * @param updatedOnBeforeDays Filter on the updatedOn, only fetch data for file handles that have been update more than the given number of days ago. If 0 no filter is applied
	 * @return The plain DBO object for the file handles with the given id
	 */
	List<DBOFileHandle> getDBOFileHandlesBatch(List<Long> ids, int updatedOnBeforeDays);
	
	/**
	 * @param bucketName The name of the bucket to filter for
	 * @param modifedBefore Include only files modified before the given instant
	 * @param modifiedAfter Include only files modified after the given instant
	 * @param limit The limit to apply
	 * @return A batch of keys of file handles that are unlinked within the given range
	 */
	List<String> getUnlinkedKeysForBucket(String bucketName, Instant modifiedBefore, Instant modifiedAfter, int limit);
	
	/**
	 * Counts the number of file handles that matches the given bucket and key and that are either {@link FileHandleStatus#AVAILABLE} or {@link FileHandleStatus#UNLINKED} but modified after the given instant
	 * 
	 * @param bucketName The name of the bucket
	 * @param key The object key
	 * @param modifiedAfter The lower bound for the filter
	 * @return The count of file handles for the given key that are available or that are unlinked but modified after the given instant
	 */
	int getAvailableOrEarlyUnlinkedFileHandlesCount(String bucketName, String key, Instant modifiedAfter);
	
	/**
	 * Clear all the previews for the file handles matching the given bucket, key and status
	 * 
	 * @param bucketName The name of the bucket
	 * @param key The object key
	 * @param status The status filter
	 * @return The set of ids of the cleared previews
	 */
	Set<Long> clearPreviewByKeyAndStatus(String bucketName, String key, FileHandleStatus status);
	
	/**
	 * Given the list of preview ids returns the subset of previews that are not referenced by file handles
	 * 
	 * @param previewIds The list of preview ids
	 * @return The subset of preview ids that are not referenced anymore
	 */
	Set<Long> getReferencedPreviews(Set<Long> previewIds);
	
	/**
	 * @param ids The set of file handle ids
	 * @return The bucket/key pairs for the file handles matching the ids in the set
	 */
	Set<BucketAndKey> getBucketAndKeyBatch(Set<Long> ids);
	
	/**
	 * Deletes all the file handles that match the given bucket name and key and that are not {@link FileHandleStatus#AVAILABLE}
	 * 
	 * @param bucketName The name of the bucket
	 * @param key The object key
	 */
	void deleteUnavailableByBucketAndKey(String bucketName, String key);
	
	/**
	 * Deletes a batch of file handles by ids
	 * 
	 * @param ids
	 */
	void deleteBatch(Set<Long> ids);
	
	/**
	 * @param bucketName The name of the bucket
	 * @param key The object key
	 * @return The max size assigned to the given key, can be null if no content size is assigned
	 */
	Long getContentSizeByKey(String bucketName, String key);
	
	/**
	 * Deleted all file data
	 */
	void truncateTable();

	/**
	 * Creates a batch from the given list of DBOs
	 * 
	 * @param dbos
	 */
	void createBatchDbo(List<DBOFileHandle> dbos);
	
}
