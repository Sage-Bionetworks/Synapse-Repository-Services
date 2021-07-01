package org.sagebionetworks.repo.manager.file;

import java.time.Instant;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;
import org.sagebionetworks.repo.model.file.FileHandleKeyArchiveResult;
import org.sagebionetworks.repo.model.file.FileHandleKeysArchiveRequest;
import org.sagebionetworks.repo.model.file.FileHandleRestoreResult;
import org.sagebionetworks.repo.model.file.FileHandleStatus;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.sqs.model.Message;

/**
 * Entry point for file handles archival
 */
public interface FileHandleArchivalManager {
	
	/**
	 * The default S3 Tag assigned to archived objects
	 */
	Tag S3_TAG_ARCHIVED = new Tag("synapse-status", "archive");
	
	/**
	 * The default size threshold for tagging S3 objects
	 */
	long S3_TAG_SIZE_THRESHOLD = 128 * 1024;

	/**
	 * Process a request to submit a batch of UNLINKED file handles for archival
	 * 
	 * @param user    Must be an administrator
	 * @param request The archival request
	 * @return The response containing information the archival request
	 */
	FileHandleArchivalResponse processFileHandleArchivalRequest(UserInfo user, FileHandleArchivalRequest request);

	/**
	 * Parses an SQS message into a {@link FileHandleKeysArchiveRequest} that is processed by a worker
	 * 
	 * @param message The SQS message containing the archival request of a set of keys in a bucket
	 * @return The parsed request, throws if cannot be parsed
	 */
	FileHandleKeysArchiveRequest parseArchiveKeysRequestFromSqsMessage(Message message);

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
	 * Restore the file handle with the given id
	 * 
	 * @param user The user initiating the request, must be the owner of the file handle
	 * @param id The id of the file handle to restore
	 * @return The result of the restore operation
	 */
	FileHandleRestoreResult restoreFileHandle(UserInfo user, String id);
	
}
