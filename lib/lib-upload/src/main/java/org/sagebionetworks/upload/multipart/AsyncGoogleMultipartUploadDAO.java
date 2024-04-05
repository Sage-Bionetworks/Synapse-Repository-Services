package org.sagebionetworks.upload.multipart;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.part.AsyncMultipartUploadComposerDAO;
import org.sagebionetworks.repo.model.dbo.file.part.Compose;
import org.sagebionetworks.repo.model.dbo.file.part.OrderBy;
import org.sagebionetworks.repo.model.dbo.file.part.PartRange;
import org.sagebionetworks.repo.model.file.AbortMultipartRequest;
import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.transactions.TransactionNotSupported;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.HttpMethod;

@Service
public class AsyncGoogleMultipartUploadDAO implements CloudServiceMultipartUploadDAO {

	private static final String LOCK_FAILED_MESSAGE = "Cannot perform this action while parts are still being added to this multipart upload.";
	private static final int NUM_COMPOSE_PER_ADD = 4;
	private final SynapseGoogleCloudStorageClient googleCloudStorageClient;
	private final AsyncMultipartUploadComposerDAO asyncDAO;

	@Autowired
	public AsyncGoogleMultipartUploadDAO(SynapseGoogleCloudStorageClient googleCloudStorageClient,
			AsyncMultipartUploadComposerDAO asyncDAO) {
		super();
		this.googleCloudStorageClient = googleCloudStorageClient;
		this.asyncDAO = asyncDAO;
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

	@TransactionNotSupported
	@Override
	public void validateAndAddPart(AddPartRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getBucket(), "request.bucket");
		ValidateArgument.required(request.getKey(), "request.key");
		ValidateArgument.required(request.getPartKey(), "request.partKey");
		ValidateArgument.required(request.getPartMD5Hex(), "request.partMD5Hex");

		GoogleUtils.validatePartMd5(googleCloudStorageClient.getObject(request.getBucket(), request.getPartKey()),
				request.getPartMD5Hex());
		// save this part to the DB.
		asyncDAO.addPart(request.getUploadId(),
				new PartRange().setLowerBound(request.getPartNumber()).setUpperBound(request.getPartNumber()));

		// find existing parts that are ready to be merged and merge them.
		asyncDAO.findContiguousParts(request.getUploadId(), OrderBy.random, NUM_COMPOSE_PER_ADD).forEach(c -> {
			asyncDAO.attemptToLockParts(request.getUploadId(), con -> {
				composeLockedParts(request.getUploadId(), request.getBucket(), request.getKey(), c);
			}, c.getLeft(), c.getRight());
		});

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
		asyncDAO.addPart(uploadId, newPart);
		asyncDAO.removePart(uploadId, toCompose.getLeft());
		asyncDAO.removePart(uploadId, toCompose.getRight());

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

	@TransactionNotSupported
	@Override
	public long completeMultipartUpload(CompleteMultipartRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getBucket(), "request.bucket");
		ValidateArgument.required(request.getKey(), "request.key");
		ValidateArgument.required(request.getUploadId(), "request.uploadId");
		String uploadId = request.getUploadId().toString();
		Optional<Compose> optional = null;
		do {
			// merge any remaining parts
			optional = asyncDAO.findContiguousParts(uploadId, OrderBy.asc, 1).stream().findFirst();
			optional.ifPresent(toMerge -> {
				if (!asyncDAO.attemptToLockParts(uploadId, con -> {
					composeLockedParts(uploadId, request.getBucket(), request.getKey(), toMerge);
				}, toMerge.getLeft(), toMerge.getRight())) {
					throw new IllegalArgumentException(LOCK_FAILED_MESSAGE);
				}
			});
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

		asyncDAO.listAllPartsForUploadId(request.getUploadId()).forEach(p -> {
			if (!asyncDAO.attemptToLockParts(request.getUploadId(), c -> {
				asyncDAO.removePart(request.getUploadId(), p);
			}, p)) {
				throw new IllegalArgumentException(LOCK_FAILED_MESSAGE);
			}
		});
		// delete any existing parts in Google
		googleCloudStorageClient.getObjects(request.getBucket(), request.getKey() + "/").forEach(Blob::delete);
		if (doesObjectExist(request.getBucket(), request.getKey())) {
			googleCloudStorageClient.deleteObject(request.getBucket(), request.getKey());
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
