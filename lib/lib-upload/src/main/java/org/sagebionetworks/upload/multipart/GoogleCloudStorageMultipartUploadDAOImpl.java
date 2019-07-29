package org.sagebionetworks.upload.multipart;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.model.dbo.file.DBOMultipartUploadComposerPartState;
import org.sagebionetworks.repo.model.dbo.file.MultipartUploadComposerDAO;
import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.upload.PartRange;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.HttpMethod;

public class GoogleCloudStorageMultipartUploadDAOImpl implements CloudServiceMultipartUploadDAO {

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

	@WriteTransaction
	@Override
	public void validateAndAddPart(AddPartRequest request) {
		validatePartMd5(request);
		addPart(request.getUploadId(), request.getBucket(), request.getKey(), request.getPartNumber(), request.getPartNumber(), request.getTotalNumberOfParts());
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

	void addPart(String uploadId, String bucket, String key, Long lowerBound, Long upperBound, Long totalNumberOfParts) {
		multipartUploadComposerDAO.addPartToUpload(uploadId, lowerBound, upperBound);
		// If lowerBound is 1 and upperBound == totalNumber, then we have the entire file. Otherwise, try to stitch
		if (lowerBound != 1 || !upperBound.equals(totalNumberOfParts)) {
			PartRange expectedStitchPartRange =
					MultipartUploadUtils.getRangeOfPotentialStitchTargets(lowerBound, upperBound, totalNumberOfParts);

			// Get the parts that have already been uploaded
			List<DBOMultipartUploadComposerPartState> uploadedParts = multipartUploadComposerDAO
					.getAddedPartRanges(Long.valueOf(uploadId),
							expectedStitchPartRange.getLowerBound(),
							expectedStitchPartRange.getUpperBound());

			// We must make sure that the sum of each part size is equivalent to the size of a new merged part
			long sumOfUploadedPartSizesInRange = 0;
			for (DBOMultipartUploadComposerPartState part : uploadedParts) {
				sumOfUploadedPartSizesInRange += part.getSizeOfPart();
			}

			if (expectedStitchPartRange.getSize() == sumOfUploadedPartSizesInRange // Make sure that amount of data in the new part is equivalent to the amount of data we have
					&& expectedStitchPartRange.getNumberOfParts() == uploadedParts.size()){	// We must also make sure that we get exactly the number of parts that we expect

				// We can now stitch the parts
				// Get the key names for the old parts
				List<String> partKeys = new ArrayList<>();
				for (DBOMultipartUploadComposerPartState part : uploadedParts) {
					partKeys.add(MultipartUploadUtils.createPartKeyFromRange(key, part.getPartRangeLowerBound(), part.getPartRangeUpperBound()));
				}

				// Create a new part
				googleCloudStorageClient.composeObjects(bucket,
						MultipartUploadUtils.createPartKeyFromRange(key, expectedStitchPartRange.getLowerBound(), expectedStitchPartRange.getUpperBound()),
						partKeys);

				multipartUploadComposerDAO.deletePartsInRange(uploadId, expectedStitchPartRange.getLowerBound(), expectedStitchPartRange.getUpperBound());

				// Recursively add the new composed part (and attempt to merge it)
				addPart(uploadId, bucket, key, expectedStitchPartRange.getLowerBound(), expectedStitchPartRange.getUpperBound(), totalNumberOfParts);
			}
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

		// Get all files that start with the key to delete all of the temporary part files.
		for (Blob blob : googleCloudStorageClient.getObjects(request.getBucket(), request.getKey())) {
			if (!blob.getName().equals(request.getKey())) { // If not an exact match (i.e. these will have part number junk at the end of the name)
				blob.delete();
			}
		}
		return googleCloudStorageClient.getObject(request.getBucket(), request.getKey()).getSize();
	}

}