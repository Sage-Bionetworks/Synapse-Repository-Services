package org.sagebionetworks.upload.multipart;

import java.util.List;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.HttpMethod;

@Service
public class AsyncGoogleMultipartUploadDAO implements CloudServiceMultipartUploadDAO {

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
		System.out.println("\tComposing part: "+toCompose.toString());
		// holding the lock on both the left and right part...
		List<String> startingPartKeys = List.of(
				MultipartUploadUtils.createPartKeyFromRange(key, toCompose.getLeft().getLowerBound(),
						toCompose.getLeft().getUpperBound()),
				MultipartUploadUtils.createPartKeyFromRange(key, toCompose.getRight().getLowerBound(),
						toCompose.getRight().getUpperBound()));
		
		PartRange newPart = new PartRange().setLowerBound(toCompose.getLeft().getLowerBound())
				.setUpperBound(toCompose.getRight().getUpperBound());
		String newPartKey = MultipartUploadUtils.createPartKeyFromRange(key,
				newPart.getLowerBound(), newPart.getUpperBound());

		googleCloudStorageClient.composeObjects(bucket, newPartKey, startingPartKeys);
		asyncDAO.addPart(uploadId, newPart);
		asyncDAO.removePart(uploadId, toCompose.getLeft());
		asyncDAO.removePart(uploadId, toCompose.getRight());
		
		startingPartKeys.forEach(k->{
			googleCloudStorageClient.deleteObject(bucket, k);
		});
	}

	@Override
	public void validatePartCopy(CompositeMultipartUploadStatus status, long partNumber, String partMD5Hex) {
		throw new UnsupportedOperationException(GoogleUtils.UNSUPPORTED_COPY_MSG);
	}

	@TransactionNotSupported
	@Override
	public long completeMultipartUpload(CompleteMultipartRequest request) {
		String uploadId = request.getUploadId().toString();
		// merge any remaining parts
		List<Compose> toMerge = asyncDAO.findContiguousParts(uploadId, OrderBy.asc, 1);
		while(!toMerge.isEmpty()) {
			Compose remainingPart = toMerge.get(0);
			if(!asyncDAO.attemptToLockParts(uploadId, con->{
				composeLockedParts(uploadId, request.getBucket(), request.getKey(), remainingPart);
			}, remainingPart.getLeft(), remainingPart.getRight())) {
				throw new IllegalArgumentException(
						"Cannot perform this action while parts are still being added to this multipart upload.");
			}
			toMerge = asyncDAO.findContiguousParts(uploadId, OrderBy.asc, 1);
		}
		return 0;
	}

	@Override
	public void tryAbortMultipartRequest(AbortMultipartRequest request) {
		deleteAllPartsForUploadID(request.getUploadId());
		// delete any existing parts in Google
		googleCloudStorageClient.getObjects(request.getBucket(), request.getKey() + "/").forEach(Blob::delete);
		if (doesObjectExist(request.getBucket(), request.getKey())) {
			googleCloudStorageClient.deleteObject(request.getBucket(), request.getKey());
		}
	}

	void deleteAllPartsForUploadID(String uploadId) {

		asyncDAO.listAllPartsForUploadId(uploadId).forEach(p -> {
			if (!asyncDAO.attemptToLockParts(uploadId, c -> {
				asyncDAO.removePart(uploadId, p);
			}, p)) {
				throw new IllegalArgumentException(
						"Cannot perform this action while parts are still being added to this multipart upload.");
			}
		});
	}

	@Override
	public String getObjectEtag(String bucket, String key) {
		throw new UnsupportedOperationException(GoogleUtils.UNSUPPORTED_COPY_MSG);
	}

	@Override
	public boolean doesObjectExist(String bucketName, String objectKey) {
		return googleCloudStorageClient.doesObjectExist(bucketName, objectKey);
	}

}
