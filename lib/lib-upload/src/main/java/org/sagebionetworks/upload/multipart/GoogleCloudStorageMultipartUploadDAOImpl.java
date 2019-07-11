package org.sagebionetworks.upload.multipart;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.model.dbo.file.DBOMultipartUploadComposerPartState;
import org.sagebionetworks.repo.model.dbo.file.MultipartUploadComposerDAO;
import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.upload.PartRange;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.HttpMethod;

public class GoogleCloudStorageMultipartUploadDAOImpl implements GoogleCloudStorageMultipartUploadDAO {

	// 15 minutes
	private static final int PRE_SIGNED_URL_EXPIRATION_MS = 15 * 1000 * 60;

	@Autowired
	private SynapseGoogleCloudStorageClient googleCloudStorageClient;

	@Autowired
	private MultipartUploadComposerDAO multipartUploadComposerDAO;

	@Override
	public String initiateMultipartUpload(String bucket, String key, MultipartUploadRequest request) {
		// Google cloud uploads do not require a token, so just return an empty string.
		return "";
	}

	@Override
	public URL createPreSignedPutUrl(String bucket, String partKey, String contentType) {
		return googleCloudStorageClient.createSignedUrl(bucket, partKey, PRE_SIGNED_URL_EXPIRATION_MS, HttpMethod.PUT);
	}

	@Override
	public void addPart(AddPartRequest request) {
		validatePartMd5(request);
		addPartWithoutValidatingMd5(request.getUploadId(), request.getBucket(), request.getKey(), request.getPartNumber(), request.getPartNumber(), request.getTotalNumberOfParts());
	}

	void validatePartMd5(AddPartRequest request) {
		Blob uploadedPart = googleCloudStorageClient.getObject(request.getBucket(), request.getPartKey());
		if (uploadedPart == null) {
			throw new IllegalArgumentException("The uploaded part could not be found");
		}
		if (!Hex.encodeHexString(Base64.decodeBase64(uploadedPart.getMd5())).equals(request.getPartMD5Hex())) {
			throw new IllegalArgumentException("The provided MD5 does not match the MD5 of the uploaded part.  Please re-upload the part.");
		}
		// The part was uploaded successfully
	}

	// Parts that are stitched by Synapse and not uploaded by the client don't need to be validated
	// (and cannot be validated, since we don't know the md5 before stitching)
	@WriteTransaction
	void addPartWithoutValidatingMd5(String uploadId, String bucket, String key, Long lowerBound, Long upperBound, Long totalNumberOfParts) {
		multipartUploadComposerDAO.addPartToUpload(uploadId, lowerBound, upperBound);
		if (lowerBound != 1 || !upperBound.equals(totalNumberOfParts)) { // If this is false, we have the entire file and no longer need to merge parts.
			attemptToMergePart(Long.valueOf(uploadId), bucket, key, lowerBound, upperBound, totalNumberOfParts);
		}
	}

	@MandatoryWriteTransaction
	void attemptToMergePart(Long uploadId, String bucket, String key, Long lowerBound, Long upperBound, Long totalNumberOfParts) {
		PartRange newStitchedPartRange = MultipartUploadUtils.getRangeOfPotentialStitchTargets(lowerBound, upperBound, totalNumberOfParts);
		List<DBOMultipartUploadComposerPartState> uploadedPartRanges =
				multipartUploadComposerDAO.getAddedPartRanges(uploadId, newStitchedPartRange.getLowerBound(), newStitchedPartRange.getUpperBound());

		// We must make sure that the sum of each part size is equivalent to the size of a new merged part
		long sizeOfNewPart = newStitchedPartRange.getUpperBound() - newStitchedPartRange.getLowerBound() + 1;
		long sumOfUploadedPartSizesInRange = uploadedPartRanges.stream().mapToLong(part -> part.getPartRangeUpperBound() - part.getPartRangeLowerBound() + 1).sum();

		// We must also make sure that we get exactly the number of parts that we expect
		if (sizeOfNewPart == sumOfUploadedPartSizesInRange && newStitchedPartRange.getNumberOfParts() == uploadedPartRanges.size()) {
			List<String> partKeys = uploadedPartRanges.stream()
					.map(part -> MultipartUploadUtils.createPartKeyFromRange(key, part.getPartRangeLowerBound(), part.getPartRangeUpperBound()))
					.collect(Collectors.toList());

			googleCloudStorageClient.composeObjects(bucket,
					MultipartUploadUtils.createPartKeyFromRange(key, newStitchedPartRange.getLowerBound(), newStitchedPartRange.getUpperBound()),
					partKeys);

			// Delete the old parts
			for (String part : partKeys) {
				googleCloudStorageClient.deleteObject(bucket, part);
			}
			multipartUploadComposerDAO.deletePartsInRange(uploadId.toString(), newStitchedPartRange.getLowerBound(), newStitchedPartRange.getUpperBound());

			// Add the new composed part (and attempt to merge it)
			addPartWithoutValidatingMd5(uploadId.toString(), bucket, key, newStitchedPartRange.getLowerBound(), newStitchedPartRange.getUpperBound(), totalNumberOfParts);
		}
	}

	@WriteTransaction
	@Override
	public long completeMultipartUpload(CompleteMultipartRequest request) {
		// first verify we have one part that contains all of the part contents (i.e. a merged file with part range
		// 1-n, where n is the number of parts)
		List<DBOMultipartUploadComposerPartState> parts = multipartUploadComposerDAO.getAddedParts(request.getUploadId());
		if (!parts.get(0).getPartRangeLowerBound().equals(1L) || !parts.get(0).getPartRangeUpperBound().equals(request.getNumberOfParts())) {
			throw new IllegalArgumentException("Not every part has been uploaded and merged.");
		}
		multipartUploadComposerDAO.deletePartsInRange(request.getUploadId().toString(), -1, Long.MAX_VALUE);
		googleCloudStorageClient.rename(request.getBucket(), MultipartUploadUtils.createPartKeyFromRange(request.getKey(), 1, request.getNumberOfParts().intValue()), request.getKey());
		return googleCloudStorageClient.getObject(request.getBucket(), request.getKey()).getSize();
	}

}