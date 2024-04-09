package org.sagebionetworks.upload.multipart;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.google.AsyncGooglePartRangeDao;
import org.sagebionetworks.repo.model.dbo.file.google.Compose;
import org.sagebionetworks.repo.model.dbo.file.google.OrderBy;
import org.sagebionetworks.repo.model.dbo.file.google.PartRange;
import org.sagebionetworks.repo.model.file.AbortMultipartRequest;
import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.transactions.TransactionNotSupported;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.HttpMethod;

@Service
public class AsyncGoogleMultipartUploadDao implements CloudServiceMultipartUploadDAO {

	private static final String LOCK_FAILED_MESSAGE = "Cannot perform this action while parts are still being added to this multipart upload.";
	private static final int NUM_COMPOSE_PER_ADD = 4;
	private final SynapseGoogleCloudStorageClient googleCloudStorageClient;
	private final AsyncGooglePartRangeDao asyncDAO;
	private final Logger log;

	@Autowired
	public AsyncGoogleMultipartUploadDao(SynapseGoogleCloudStorageClient googleCloudStorageClient,
			AsyncGooglePartRangeDao asyncDAO, LoggerProvider logProvider) {
		super();
		this.googleCloudStorageClient = googleCloudStorageClient;
		this.asyncDAO = asyncDAO;
		this.log = logProvider.getLogger(AsyncGoogleMultipartUploadDao.class.getName());
	}

	@Override
	public String initiateMultipartUpload(String bucket, String key, MultipartUploadRequest request) {
		return "";
	}

	@Override
	public String initiateMultipartUploadCopy(String bucket, String key, MultipartUploadCopyRequest request,
			FileHandle fileHandle) {
		throw new UnsupportedOperationException(GoogleUtils.UNSUPPORTED_COPY_MSG);
	}

	@Override
	public PresignedUrl createPartUploadPreSignedUrl(String bucket, String partKey, String contentType) {
		ValidateArgument.required(bucket, "bucket");
		ValidateArgument.required(partKey, "key");
		return new PresignedUrl().withUrl(googleCloudStorageClient.createSignedUrl(bucket, partKey,
				GoogleUtils.PRE_SIGNED_URL_EXPIRATION_MS, HttpMethod.PUT));
	}

	@Override
	public PresignedUrl createPartUploadCopyPresignedUrl(CompositeMultipartUploadStatus status, long partNumber,
			String contentType) {
		throw new UnsupportedOperationException(GoogleUtils.UNSUPPORTED_COPY_MSG);
	}

	@WriteTransaction
	@Override
	public void validateAndAddPart(AddPartRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getBucket(), "request.bucket");
		ValidateArgument.required(request.getKey(), "request.key");
		ValidateArgument.required(request.getPartKey(), "request.partKey");
		ValidateArgument.required(request.getPartMD5Hex(), "request.partMD5Hex");

		/*
		 * With Google multi-part upload we need to compose each part into larger parts
		 * until all parts are composed into a single file. By composing parts with each
		 * add request, we can distribute the work of composing parts across the entire
		 * upload. This ensures that the amount compose work needed to complete the
		 * upload is kept to a minimum. The following will find four random pairs of
		 * contiguous parts, where each pair consists of a left and right part. If a
		 * lock can be acquired on both the left and right, then they will be composed
		 * into a new single file in a separate transaction. Since each compose is
		 * independent of this add request, compose failures are logged but not thrown.
		 */
		asyncDAO.findContiguousPartRanges(request.getUploadId(), OrderBy.random, NUM_COMPOSE_PER_ADD).forEach(c -> {
			asyncDAO.attemptToLockPartRanges(request.getUploadId(), () -> {
				try {
					composeLockedParts(request.getUploadId(), request.getBucket(), request.getKey(), c);
				} catch (Exception e) {
					log.error("Failed to compose parts:", e);
				}
			}, c.getLeft(), c.getRight());
		});

		GoogleUtils.validatePartMd5(googleCloudStorageClient.getObject(request.getBucket(), request.getPartKey()),
				request.getPartMD5Hex());
		// save this part to the DB.
		asyncDAO.addPartRange(request.getUploadId(),
				new PartRange().setLowerBound(request.getPartNumber()).setUpperBound(request.getPartNumber()));

	}

	void composeLockedParts(String uploadId, String bucket, String key, Compose toCompose) {
		ValidateArgument.required(uploadId, "uploadId");
		ValidateArgument.required(bucket, "bucket");
		ValidateArgument.required(key, "key");
		validateCompose(toCompose);

		// holding the lock on both the left and right part...
		List<String> startingPartKeys = List.of(
				MultipartUploadUtils.createPartKeyFromRange(key, toCompose.getLeft().getLowerBound(),
						toCompose.getLeft().getUpperBound()),
				MultipartUploadUtils.createPartKeyFromRange(key, toCompose.getRight().getLowerBound(),
						toCompose.getRight().getUpperBound()));

		PartRange newPart = new PartRange().setLowerBound(toCompose.getLeft().getLowerBound())
				.setUpperBound(toCompose.getRight().getUpperBound());
		String newPartKey = MultipartUploadUtils.createPartKeyFromRange(key, newPart.getLowerBound(),
				newPart.getUpperBound());

		googleCloudStorageClient.composeObjects(bucket, newPartKey, startingPartKeys);
		asyncDAO.addPartRange(uploadId, newPart);
		asyncDAO.removePartRange(uploadId, toCompose.getLeft());
		asyncDAO.removePartRange(uploadId, toCompose.getRight());

		startingPartKeys.forEach(k -> {
			googleCloudStorageClient.deleteObject(bucket, k);
		});
	}

	public void validateCompose(Compose compose) {
		ValidateArgument.required(compose, "compose");
		validatePartRange(compose.getLeft());
		validatePartRange(compose.getRight());
	}

	public void validatePartRange(PartRange range) {
		ValidateArgument.required(range, "range");
		ValidateArgument.required(range.getLowerBound(), "range.lowerBound");
		ValidateArgument.required(range.getUpperBound(), "range.upperBound");
	}

	@Override
	public void validatePartCopy(CompositeMultipartUploadStatus status, long partNumber, String partMD5Hex) {
		throw new UnsupportedOperationException(GoogleUtils.UNSUPPORTED_COPY_MSG);
	}

	@WriteTransaction
	@Override
	public long completeMultipartUpload(CompleteMultipartRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getBucket(), "request.bucket");
		ValidateArgument.required(request.getKey(), "request.key");
		ValidateArgument.required(request.getUploadId(), "request.uploadId");
		ValidateArgument.required(request.getNumberOfParts(), "request.numberOfParts");
		String uploadId = request.getUploadId().toString();
		Optional<Compose> optional = null;
		int count = 0;
		do {
			// merge any remaining parts
			optional = asyncDAO.findContiguousPartRanges(uploadId, OrderBy.asc, 1).stream().findFirst();
			optional.ifPresent(toMerge -> {
				if (!asyncDAO.attemptToLockPartRanges(uploadId, () -> {
					composeLockedParts(uploadId, request.getBucket(), request.getKey(), toMerge);
				}, toMerge.getLeft(), toMerge.getRight())) {
					throw new IllegalArgumentException(LOCK_FAILED_MESSAGE);
				}
			});
			count++;
			if (count > request.getNumberOfParts()) {
				throw new IllegalStateException("Failed to merge all parts for: " + request.toString());
			}
		} while (optional.isPresent());

		googleCloudStorageClient.rename(request.getBucket(),
				MultipartUploadUtils.createPartKeyFromRange(request.getKey(), 1, request.getNumberOfParts().intValue()),
				request.getKey());
		return googleCloudStorageClient.getObject(request.getBucket(), request.getKey()).getSize();
	}

	@TransactionNotSupported
	@Override
	public void tryAbortMultipartRequest(AbortMultipartRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getUploadId(), "request.uploadId");
		ValidateArgument.required(request.getBucket(), "request.bucket");
		ValidateArgument.required(request.getKey(), "request.key");

		asyncDAO.listAllPartRangesForUploadId(request.getUploadId()).forEach(p -> {
			if (!asyncDAO.attemptToLockPartRanges(request.getUploadId(), () -> {
				asyncDAO.removePartRange(request.getUploadId(), p);
				deleteObjectIfExists(request.getBucket(), MultipartUploadUtils.createPartKeyFromRange(request.getKey(),
						p.getLowerBound(), p.getUpperBound()));
			}, p)) {
				throw new IllegalArgumentException(LOCK_FAILED_MESSAGE);
			}
		});
		deleteObjectIfExists(request.getBucket(), request.getKey());
	}

	void deleteObjectIfExists(String bucket, String key) {
		Blob partBlob = googleCloudStorageClient.getObject(bucket, key);
		if (partBlob != null) {
			partBlob.delete();
		}
	}

	@Override
	public String getObjectEtag(String bucket, String key) {
		throw new UnsupportedOperationException(GoogleUtils.UNSUPPORTED_COPY_MSG);
	}

	@Override
	public boolean doesObjectExist(String bucketName, String objectKey) {
		ValidateArgument.required(bucketName, "bucketName");
		ValidateArgument.required(objectKey, "objectKey");
		return googleCloudStorageClient.doesObjectExist(bucketName, objectKey);
	}

}
