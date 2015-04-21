package org.sagebionetworks.file.worker;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.AbstractWorker;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.MultipartManagerImpl;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileCopyRequest;
import org.sagebionetworks.repo.model.file.S3FileCopyResult;
import org.sagebionetworks.repo.model.file.S3FileCopyResultType;
import org.sagebionetworks.repo.model.file.S3FileCopyResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.AmazonErrorCodes;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.ProgressCallback;
import org.sagebionetworks.util.ValidateArgument;
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

public class S3FileCopyWorker extends AbstractWorker {

	private static final long MAX_S3_COPY_PART_SIZE = 5 * 1024 * 1024; // max part size for S3 copy is 5MB

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
		private final long progressTotal;
		private final String fileName;
		private final AsynchronousJobStatus status;
		private long progressCurrent = 0L;

		S3CopyFileProgressCallback(long progressTotal, long progressCurrent, String fileName, AsynchronousJobStatus status) {
			this.progressTotal = progressTotal;
			this.progressCurrent = progressCurrent;
			this.fileName = fileName;
			this.status = status;
		}

		@Override
		public void progressMade(Long sizeTransfered) {
			progressCurrent += sizeTransfered;
			asynchJobStatusManager.updateJobProgress(status.getJobId(), progressCurrent, progressTotal, "Copying " + fileName);
		}

		long getProgressCurrent() {
			return progressCurrent;
		}
	}

	/**
	 * This is where the real work happens
	 * 
	 * @param message
	 * @return
	 * @throws Throwable
	 */
	protected Message processMessage(Message message) throws Throwable {
		AsynchronousJobStatus status = extractStatus(message);
		try {
			UserInfo userInfo = userManager.getUserInfo(status.getStartedByUserId());
			S3FileCopyRequest request = (S3FileCopyRequest) status.getRequestBody();
			ValidateArgument.required(request.getBucket(), "bucket");

			long progressCurrent = 0L;
			long progressTotal = 0L;

			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 0L, "Intializing...");

			List<Pair<S3FileHandle, S3FileCopyResult>> files = Lists.newArrayListWithCapacity(request.getFiles().size());
			long successCount = 0;
			long errorCount = 0;

			for (String fileEntityId : request.getFiles()) {
				Entity entity = entityManager.getEntity(userInfo, fileEntityId);
				if (!(entity instanceof FileEntity)) {
					throw new IllegalArgumentException("Entity " + fileEntityId
							+ " is not a FileEntity. This operation can only handle FileEntity types");
				}
				FileEntity fileEntity = (FileEntity) entity;

				List<EntityHeader> entityPath = entityManager.getEntityPath(userInfo, fileEntityId);
				int startIndex = 1;// we skip the root node
				StringBuilder path = new StringBuilder(256);
				if (entityPath.size() > startIndex) {
					for (int i = startIndex; i < entityPath.size(); i++) {
						if (path.length() > 0) {
							path.append(MultipartManagerImpl.FILE_TOKEN_TEMPLATE_SEPARATOR);
						}
						path.append(entityPath.get(i).getName());
					}
				} else {
					path.append(fileEntity.getName());
				}

				FileHandleResults fileHandleResults = fileHandleManager.getAllFileHandles(
						Collections.singletonList(fileEntity.getDataFileHandleId()), false);

				FileHandle fileHandle = Iterables.getOnlyElement(fileHandleResults.getList());

				if (!(fileHandle instanceof S3FileHandle)) {
					throw new IllegalArgumentException("File " + fileHandle.getId()
							+ " is not an S3FileHandle. This operation can only handle S3FileHandle types");
				}
				S3FileHandle s3FileHandle = (S3FileHandle) Iterables.getOnlyElement(fileHandleResults.getList());

				S3FileCopyResult result = new S3FileCopyResult();
				result.setFile(s3FileHandle.getId());
				result.setResultKey(path.toString());

				files.add(Pair.create(s3FileHandle, result));

				AuthorizationStatus auth = authorizationManager.canAccess(userInfo, fileEntityId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD);
				if (!auth.getAuthorized()) {
					result.setResultType(S3FileCopyResultType.ERROR);
					result.setErrorMessage(auth.getReason());
					errorCount++;
				} else {
					ObjectMetadata originalFile = s3Client.getObjectMetadata(s3FileHandle.getBucketName(), s3FileHandle.getKey());
					ObjectMetadata existingCopy = null;
					try {
						existingCopy = s3Client.getObjectMetadata(request.getBucket(), result.getResultKey());
					} catch (AmazonServiceException e) {
						if (AmazonErrorCodes.S3_BUCKET_NOT_FOUND.equals(e.getErrorCode())) {
							throw new IllegalArgumentException(
									"The bucket "
											+ request.getBucket()
											+ " could not be found or accessed. Check the name and permissions on that bucket. See www.synapse.org//#!HelpPages:CreatingADownloadBucket for details on how to set up a download bucket.");
						} else if (AmazonErrorCodes.S3_KEY_NOT_FOUND.equals(e.getErrorCode())
								|| AmazonErrorCodes.S3_NOT_FOUND.equals(e.getErrorCode())) {
							// just doesn't exist. Nothing needs doing
						} else {
							log.error("Error getting s3 object info: " + e.getMessage(), e);
						}
					} catch (AmazonClientException e) {
						log.error("Error getting s3 object info: " + e.getMessage(), e);
					}

					boolean shouldCopy = true;
					if (existingCopy != null) {
						if (BooleanUtils.isNotTrue(request.getOverwrite())) {
							throw new IllegalArgumentException("The file " + s3FileHandle.getFileName()
									+ " already exists in the destination bucket " + request.getBucket() + " under key "
									+ result.getResultKey() + ". Either delete that key in your S3 bucket or specify overwrite=true.");
						}
						if (originalFile.getETag() != null && originalFile.getETag().equals(existingCopy.getETag())) {
							// the file already exists, so lets not copy it again
							shouldCopy = false;
							result.setResultType(S3FileCopyResultType.UPTODATE);
							successCount++;
						}
					}
					if (shouldCopy) {
						progressTotal += s3FileHandle.getContentSize();
					}
				}
			}

			// Start the progress
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, progressTotal, "Starting...");
			for (Pair<S3FileHandle, S3FileCopyResult> file : files) {
				if (file.getSecond().getResultType() == null) {
					S3CopyFileProgressCallback progress = new S3CopyFileProgressCallback(progressTotal, progressCurrent, file.getFirst()
							.getFileName(), status);
					try {
						copyFile(file.getFirst(), request.getBucket(), file.getSecond().getResultKey(), progress);
						file.getSecond().setResultType(S3FileCopyResultType.COPIED);
						successCount++;
						progressCurrent = progress.getProgressCurrent();
					} catch (Throwable e) {
						file.getSecond().setResultType(S3FileCopyResultType.ERROR);
						file.getSecond().setErrorMessage(e.getMessage());
						errorCount++;
						progressTotal -= file.getFirst().getContentSize();
						asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, progressTotal, "Error on file "
								+ file.getFirst().getFileName());
						log.error("S3 file copy failed: " + e.getMessage(), e);
					}
				}
			}

			S3FileCopyResults resultBody = new S3FileCopyResults();
			resultBody.setResults(Transform.toList(files, new Function<Pair<S3FileHandle, S3FileCopyResult>, S3FileCopyResult>() {
				@Override
				public S3FileCopyResult apply(Pair<S3FileHandle, S3FileCopyResult> input) {
					return input.getSecond();
				}
			}));
			resultBody.setErrorCount(errorCount);
			resultBody.setSuccessCount(successCount);
			// done
			asynchJobStatusManager.setComplete(status.getJobId(), resultBody);
			return message;
		} catch (Throwable e) {
			log.error("Worker failed:", e);
			// Record the error
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
			throw e;
		}
	}

	// package protected for testing
	void copyFile(S3FileHandle s3FileHandle, String destinationBucket, String destinationKey, ProgressCallback<Long> progress) {

		// Get object size.
		ObjectMetadata metadataResult = s3Client.getObjectMetadata(new GetObjectMetadataRequest(s3FileHandle.getBucketName(), s3FileHandle
				.getKey()));
		long objectSize = metadataResult.getContentLength(); // in bytes

		if (objectSize < MAX_S3_COPY_PART_SIZE) {
			// small enough to do a simple copy
			CopyObjectRequest copyObjectRequest = new CopyObjectRequest(s3FileHandle.getBucketName(), s3FileHandle.getKey(),
					destinationBucket, destinationKey).withCannedAccessControlList(CannedAccessControlList.BucketOwnerFullControl);
			s3Client.copyObject(copyObjectRequest);

			progress.progressMade(objectSize);
		} else {
			List<PartETag> parts = Lists.newArrayList();
			InitiateMultipartUploadResult initResult = s3Client.initiateMultipartUpload(new InitiateMultipartUploadRequest(destinationBucket,
					destinationKey));
			int partNumber = 1;
			for (long offset = 0; offset < objectSize; offset += MAX_S3_COPY_PART_SIZE, partNumber++) {
				long size = MAX_S3_COPY_PART_SIZE;
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
