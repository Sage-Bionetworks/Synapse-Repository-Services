package org.sagebionetworks.repo.manager.file;

import java.time.Instant;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;
import org.sagebionetworks.repo.model.file.FileHandleKeyArchiveResult;
import org.sagebionetworks.repo.model.file.FileHandleRestoreResult;
import org.sagebionetworks.repo.model.file.FileHandleStatus;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.s3.model.Tag;

/**
 * Entry point for file handles archival
 */
public interface FileHandleArchivalManager {
	
	/**
	 * The S3 tag assigned to archived objects. An S3 lifecycle rule matching this tag is what moves the
	 * object to a cheaper access tier and, once it is no longer needed at all, deletes it.
	 * <p>
	 * Objects of every size carry the tag. Tagging used to be limited to objects of at least 128 KiB,
	 * which is the smallest object S3 intelligent tiering will move between access tiers, so tagging
	 * anything smaller achieved nothing. Expiration has no such minimum, and PLFM-9902 requires that
	 * deleted data is deleted whatever its size.
	 */
	Tag S3_TAG_ARCHIVED = new Tag("synapse-status", "archive");

	/**
	 * Process a request to submit a batch of UNLINKED file handles for archival
	 * 
	 * @param user    Must be an administrator
	 * @param request The archival request
	 * @return The response containing information the archival request
	 */
	FileHandleArchivalResponse processFileHandleArchivalRequest(UserInfo user, FileHandleArchivalRequest request);

	/**
	 * Perform the archival of all the file handles that are unlinked and that match the given key in
	 * the given bucket. The status of the files that were modified before the given instant will be set
	 * to {@link FileHandleStatus#ARCHIVED}. If no other file handle exists with a different status then the
	 * actual object will be tagged in S3 for archival. All the previews of the ARCHIVED files will be deleted.
	 * 
	 * If the S3 key does not exists the unlinked file handles will be deleted.
	 * 
	 * @param user The user asking for the archival, must be an admin
	 * @param bucket The bucket name
	 * @param key The S3 object key
	 * @param modifedBefore Defines the upper bound for the modifiedOn when updating the matching file handles
	 */
	FileHandleKeyArchiveResult archiveUnlinkedFileHandlesByKey(UserInfo user, String bucket, String key, Instant modifedBefore) throws RecoverableMessageException;

	/**
	 * Restore the file handle with the given id. If the operation fails the exception is thrown
	 * 
	 * @param user The user initiating the request, must be the owner of the file handle
	 * @param id The id of the file handle to restore
	 * @return The result of the restore operation
	 */
	FileHandleRestoreResult restoreFileHandle(UserInfo user, String id);
		
}
