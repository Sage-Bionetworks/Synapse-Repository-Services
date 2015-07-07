package org.sagebionetworks.file.worker;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.JobCanceledException;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileCopyRequest;
import org.sagebionetworks.repo.model.file.S3FileCopyResult;
import org.sagebionetworks.repo.model.file.S3FileCopyResultType;
import org.sagebionetworks.repo.model.file.S3FileCopyResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.AmazonErrorCodes;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.sqs.model.Message;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class S3FileCopyWorker implements MessageDrivenRunner {

	// these two value not static final, for testing (so test frame work can change them to allow testing with small
	// files)
	private long S3_COPY_PART_SIZE = 5L * 1024L * 1024L * 1024L; // max part size for S3 copy is 5GB
	private long MULTIPART_UPLOAD_TRIGGER_SIZE = S3_COPY_PART_SIZE;

	private static final String ORIGINAL_MD5 = "original_md5";

	static private Logger log = LogManager.getLogger(S3FileCopyWorker.class);

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private AmazonS3Client s3Client;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AuthorizationManager authorizationManager;

	private class S3CopyFileProgressCallback implements ProgressCallback<Long> {
		private final Message message;
		private final long progressTotal;
		private final String fileName;
		private final AsynchronousJobStatus status;
		private long progressCurrent = 0L;
		private ProgressCallback<Message> progressCallback;

		S3CopyFileProgressCallback(Message message, long progressTotal, long progressCurrent, String fileName, AsynchronousJobStatus status, ProgressCallback<Message> progressCallback) {
			this.message = message;
			this.progressTotal = progressTotal;
			this.progressCurrent = progressCurrent;
			this.fileName = fileName;
			this.status = status;
			this.progressCallback = progressCallback;
		}

		@Override
		public void progressMade(Long sizeTransfered) {
			progressCurrent += sizeTransfered;
			asynchJobStatusManager.updateJobProgress(status.getJobId(), progressCurrent, progressTotal, "Copying " + fileName);
			progressCallback.progressMade(message);
		}

		long getProgressCurrent() {
			return progressCurrent;
		}
	}

	private static class Entry {
		private S3FileHandle fileHandle;
		private S3FileCopyResult fileCopyResult;

		public Entry(String fileEntityId, String destinationBucket) {
			fileCopyResult = new S3FileCopyResult();
			fileCopyResult.setFile(fileEntityId);
			fileCopyResult.setResultBucket(destinationBucket);
			fileCopyResult.setResultType(S3FileCopyResultType.NOTCOPIED);
		}

		public S3FileCopyResult getFileCopyResult() {
			return fileCopyResult;
		}

		public S3FileHandle getFileHandle() {
			return fileHandle;
		}

		public void setFileHandle(S3FileHandle fileHandle) {
			this.fileHandle = fileHandle;
		}

		public void setError(String errorMessage) {
			fileCopyResult.setResultType(S3FileCopyResultType.ERROR);
			fileCopyResult.setErrorMessage(errorMessage);
		}
	}

	/**
	 * This is where the real work happens
	 * 
	 * @param message
	 * @return
	 * @throws Throwable
	 */
	@Override
	public void run(ProgressCallback<Message> progressCallback,
			Message message) throws RecoverableMessageException, Exception {
		AsynchronousJobStatus status = extractStatus(message);
		try {
			UserInfo userInfo = userManager.getUserInfo(status.getStartedByUserId());
			S3FileCopyRequest request = (S3FileCopyRequest) status.getRequestBody();
			ValidateArgument.required(request.getBucket(), "bucket");

			long progressTotal = 0L;
			List<Entry> files = Lists.newArrayListWithCapacity(request.getFiles().size());
			for (String fileEntityId : request.getFiles()) {
				Entry entry = new Entry(fileEntityId, request.getBucket());
				files.add(entry);

				asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 0L, "Intializing " + fileEntityId);

				progressTotal += precheckFile(entry, userInfo, BooleanUtils.isTrue(request.getOverwrite()), request.getBaseKey());
			}

			boolean skipRestOfCopy = anyErrors(files);

			if (!skipRestOfCopy) {
				// Start the progress
				long progressCurrent = 0L;
				asynchJobStatusManager.updateJobProgress(status.getJobId(), progressCurrent, progressTotal, "Starting...");
				for (Entry file : files) {
					if (file.getFileCopyResult().getResultType() == S3FileCopyResultType.NOTCOPIED) {
						// check for canceled status
						AsynchronousJobStatus jobStatus = asynchJobStatusManager.getJobStatus(userInfo, status.getJobId());
						if (jobStatus.getJobCanceling()) {
							throw new JobCanceledException();
						}

						S3CopyFileProgressCallback progress = new S3CopyFileProgressCallback(message, progressTotal, progressCurrent, file
								.getFileHandle().getFileName(), status, progressCallback);
						try {
							copyFile(file.getFileHandle(), file.getFileCopyResult().getResultBucket(), file.getFileCopyResult()
									.getResultKey(), progress);
							file.getFileCopyResult().setResultType(S3FileCopyResultType.COPIED);
							progressCurrent = progress.getProgressCurrent();
						} catch (Throwable e) {
							file.setError(e.getMessage());
							progressTotal -= file.getFileHandle().getContentSize();
							asynchJobStatusManager.updateJobProgress(status.getJobId(), progressCurrent, progressTotal, "Error on file "
									+ file.getFileHandle().getFileName());
							log.error("S3 file copy failed: " + e.getMessage(), e);
						}
					}
				}
			}

			S3FileCopyResults resultBody = new S3FileCopyResults();
			resultBody.setResults(Transform.toList(files, new Function<Entry, S3FileCopyResult>() {
				@Override
				public S3FileCopyResult apply(Entry input) {
					return input.getFileCopyResult();
				}
			}));
			// done
			asynchJobStatusManager.setComplete(status.getJobId(), resultBody);
		} catch (JobCanceledException e) {
			log.error("Worker canceled");
			// Record the cancellation
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
		} catch (Throwable e) {
			log.error("Worker failed:", e);
			// Record the error
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
			throw new RecoverableMessageException();
		}
	}

	private boolean anyErrors(List<Entry> files) {
		for (Entry file : files) {
			if (file.getFileCopyResult().getResultType().equals(S3FileCopyResultType.ERROR)) {
				return true;
			}
		}
		return false;
	}

	private long precheckFile(Entry entry, UserInfo userInfo, boolean overwrite, String baseKey) {
		Entity entity;
		try {
			entity = entityManager.getEntity(userInfo, entry.getFileCopyResult().getFile());
		} catch (NotFoundException e) {
			entry.setError(e.getMessage());
			return 0;
		} catch (UnauthorizedException e) {
			entry.setError(e.getMessage());
			return 0;
		}
		if (!(entity instanceof FileEntity)) {
			entry.setError("Entity " + entry.getFileCopyResult().getFile()
					+ " is not a FileEntity. This operation can only handle FileEntity types");
			return 0;
		}
		FileEntity fileEntity = (FileEntity) entity;

		String path = entityManager.getEntityPathAsFilePath(userInfo, entry.getFileCopyResult().getFile());
		entry.getFileCopyResult().setResultKey(baseKey == null ? path : (baseKey + path));

		FileHandleResults fileHandleResults = fileHandleManager.getAllFileHandles(
				Collections.singletonList(fileEntity.getDataFileHandleId()), false);

		FileHandle fileHandle = Iterables.getOnlyElement(fileHandleResults.getList());

		if (!(fileHandle instanceof S3FileHandle)) {
			entry.setError("File " + fileHandle.getId() + " is not an S3FileHandle. This operation can only handle S3FileHandle types");
			return 0;
		}
		S3FileHandle s3FileHandle = (S3FileHandle) fileHandle;
		entry.setFileHandle(s3FileHandle);

		AuthorizationStatus auth = authorizationManager.canAccess(userInfo, entry.getFileCopyResult().getFile(), ObjectType.ENTITY,
				ACCESS_TYPE.DOWNLOAD);
		if (!auth.getAuthorized()) {
			entry.setError(auth.getReason());
			return 0;
		}

		ObjectMetadata originalFile = s3Client.getObjectMetadata(s3FileHandle.getBucketName(), s3FileHandle.getKey());
		try {
			ObjectMetadata existingCopy = s3Client.getObjectMetadata(entry.getFileCopyResult().getResultBucket(), entry.getFileCopyResult()
					.getResultKey());
			if (!overwrite) {
				entry.setError("The file " + s3FileHandle.getFileName() + " already exists in the destination bucket "
						+ entry.getFileCopyResult().getResultBucket() + " under key " + entry.getFileCopyResult().getResultKey()
						+ ". Either delete that key in your S3 bucket or specify overwrite=true.");
				return 0;
			}
			if (originalFile.getETag() != null) {
				// check the etag, which is not a good indicator, as it is very much dependent on how the file was
				// uploaded (multipart uploads and part size)
				if (originalFile.getETag().equals(existingCopy.getETag())) {
					// the file already exists, so lets not copy it again
					entry.getFileCopyResult().setResultType(S3FileCopyResultType.UPTODATE);
					return 0;
				}
				// next check our custom meta data against the etag of the original
				if (existingCopy.getUserMetaDataOf(ORIGINAL_MD5) != null
						&& existingCopy.getUserMetaDataOf(ORIGINAL_MD5).equals(originalFile.getETag())) {
					// the file already exists, so lets not copy it again
					entry.getFileCopyResult().setResultType(S3FileCopyResultType.UPTODATE);
					return 0;
				}
			}
		} catch (AmazonServiceException e) {
			if (AmazonErrorCodes.S3_BUCKET_NOT_FOUND.equals(e.getErrorCode())) {
				throw new IllegalArgumentException(
						"The bucket "
								+ entry.getFileCopyResult().getResultBucket()
								+ " could not be found or accessed. Check the name and permissions on that bucket. See https://www.synapse.org/#!Help:CreateS3DownloadBucket for details on how to set up a download bucket.");
			} else if (AmazonErrorCodes.S3_KEY_NOT_FOUND.equals(e.getErrorCode()) || AmazonErrorCodes.S3_NOT_FOUND.equals(e.getErrorCode())) {
				// just doesn't exist. Nothing needs doing
			} else {
				log.error("Error getting s3 object info: " + e.getMessage(), e);
			}
		} catch (AmazonClientException e) {
			log.error("Error getting s3 object info: " + e.getMessage(), e);
		}

		return s3FileHandle.getContentSize();
	}

	// package protected for testing
	void copyFile(S3FileHandle s3FileHandle, String destinationBucket, String destinationKey, ProgressCallback<Long> progress) {

		// Get object size.
		ObjectMetadata metadataResult = s3Client.getObjectMetadata(new GetObjectMetadataRequest(s3FileHandle.getBucketName(), s3FileHandle
				.getKey()));

		ObjectMetadata copyObjectMetadata = metadataResult.clone();
		copyObjectMetadata.getUserMetadata().put(ORIGINAL_MD5, metadataResult.getETag());

		long objectSize = metadataResult.getContentLength(); // in bytes
		if (objectSize < MULTIPART_UPLOAD_TRIGGER_SIZE) {
			// small enough to do a simple copy
			CopyObjectRequest copyObjectRequest = new CopyObjectRequest(s3FileHandle.getBucketName(), s3FileHandle.getKey(),
					destinationBucket, destinationKey).withCannedAccessControlList(CannedAccessControlList.BucketOwnerFullControl)
					.withNewObjectMetadata(copyObjectMetadata);
			s3Client.copyObject(copyObjectRequest);

			progress.progressMade(objectSize);
		} else {
			List<PartETag> parts = Lists.newArrayList();
			InitiateMultipartUploadResult initResult = s3Client.initiateMultipartUpload(new InitiateMultipartUploadRequest(destinationBucket,
					destinationKey).withCannedACL(CannedAccessControlList.BucketOwnerFullControl));
			int partNumber = 1;
			for (long offset = 0; offset < objectSize; offset += S3_COPY_PART_SIZE, partNumber++) {
				long size = S3_COPY_PART_SIZE;
				if (offset + size > objectSize) {
					size = objectSize - offset;
				}
				CopyPartRequest copyRequest = new CopyPartRequest().withSourceBucketName(s3FileHandle.getBucketName())
						.withSourceKey(s3FileHandle.getKey()).withDestinationBucketName(destinationBucket).withDestinationKey(destinationKey)
						.withUploadId(initResult.getUploadId()).withFirstByte(offset).withLastByte(offset + size - 1)
						.withPartNumber(partNumber);

				CopyPartResult copyPartResult = s3Client.copyPart(copyRequest);
				parts.add(new PartETag(copyPartResult.getPartNumber(), copyPartResult.getETag()));

				progress.progressMade(size);
			}
			CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(destinationBucket, destinationKey,
					initResult.getUploadId(), parts);

			s3Client.completeMultipartUpload(completeRequest);
		}
	}

	/**
	 * Extract the AsynchUploadRequestBody from the message.
	 * 
	 * @param message
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	AsynchronousJobStatus extractStatus(Message message) throws JSONObjectAdapterException {
		ValidateArgument.required(message, "message");
		if (message == null) {
			throw new IllegalArgumentException("Message cannot be null");
		}
		AsynchronousJobStatus status = MessageUtils.readMessageBody(message, AsynchronousJobStatus.class);
		ValidateArgument.requireType(status.getRequestBody(), S3FileCopyRequest.class, "job body");
		return status;
	}

}
