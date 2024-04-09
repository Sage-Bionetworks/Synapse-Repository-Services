package org.sagebionetworks.repo.model.dbo.file;

import java.util.Objects;

import org.sagebionetworks.repo.model.file.UploadType;

/**
 * DTO to create a new multi-part request.
 *
 */
public class CreateMultipartRequest {

	private Long userId;
	private String hash;
	private String requestBody;
	private String uploadToken;
	private UploadType uploadType;
	private String bucket;
	private String key;
	private Integer numberOfParts;
	private Long partSize;
	private String sourceFileHandleId;
	private String sourceFileEtag;

	/**
	 * User for a normal upload request
	 */
	public CreateMultipartRequest(Long userId, String hash, String requestBody, String uploadToken, UploadType uploadType, String bucket, String key, Integer numberOfParts, Long partSize) {
		this.userId = userId;
		this.hash = hash;
		this.requestBody = requestBody;
		this.uploadToken = uploadToken;
		this.uploadType = uploadType;
		this.bucket = bucket;
		this.key = key;
		this.numberOfParts = numberOfParts;
		this.partSize = partSize;
	}
	
	/**
	 * Used when the request is a copy from a source file
	 */
	public CreateMultipartRequest(Long userId, String hash, String requestBody, String uploadToken,
			UploadType uploadType, String bucket, String key, Integer numberOfParts, Long partSize, 
			String sourceFileHandleId,
			String sourceFileEtag) {
		this(userId, hash, requestBody, uploadToken, uploadType, bucket, key, numberOfParts, partSize);
		this.sourceFileHandleId = sourceFileHandleId;
		this.sourceFileEtag = sourceFileEtag;
	}
	
	public CreateMultipartRequest() {}

	public Long getUserId() {
		return userId;
	}

	public CreateMultipartRequest setUserId(Long userId) {
		this.userId = userId;
		return this;
	}

	public String getHash() {
		return hash;
	}

	public CreateMultipartRequest setHash(String hash) {
		this.hash = hash;
		return this;
	}

	public String getRequestBody() {
		return requestBody;
	}

	public CreateMultipartRequest setRequestBody(String requestBody) {
		this.requestBody = requestBody;
		return this;
	}

	public String getUploadToken() {
		return uploadToken;
	}

	public CreateMultipartRequest setUploadToken(String uploadToken) {
		this.uploadToken = uploadToken;
		return this;
	}

	public UploadType getUploadType() {
		return uploadType;
	}

	public CreateMultipartRequest setUploadType(UploadType uploadType) {
		this.uploadType = uploadType;
		return this;
	}

	public String getBucket() {
		return bucket;
	}

	public CreateMultipartRequest setBucket(String bucket) {
		this.bucket = bucket;
		return this;
	}

	public String getKey() {
		return key;
	}

	public CreateMultipartRequest setKey(String key) {
		this.key = key;
		return this;
	}

	public Integer getNumberOfParts() {
		return numberOfParts;
	}

	public CreateMultipartRequest setNumberOfParts(Integer numberOfParts) {
		this.numberOfParts = numberOfParts;
		return this;
	}

	public String getSourceFileHandleId() {
		return sourceFileHandleId;
	}
	
	public CreateMultipartRequest setSourceFileHandleId(String sourceFileHandleId) {
		this.sourceFileHandleId = sourceFileHandleId;
		return this;
	}
	
	public String getSourceFileEtag() {
		return sourceFileEtag;
	}
	
	public CreateMultipartRequest setSourceFileEtag(String sourceFileEtag) {
		this.sourceFileEtag = sourceFileEtag;
		return this;
	}

	public Long getPartSize() {
		return partSize;
	}

	public CreateMultipartRequest setPartSize(Long partSize) {
		this.partSize = partSize;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bucket, hash, key, numberOfParts, partSize, requestBody, sourceFileEtag, sourceFileHandleId,
				uploadToken, uploadType, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		CreateMultipartRequest other = (CreateMultipartRequest) obj;
		return Objects.equals(bucket, other.bucket) && Objects.equals(hash, other.hash)
				&& Objects.equals(key, other.key) && Objects.equals(numberOfParts, other.numberOfParts)
				&& Objects.equals(partSize, other.partSize) && Objects.equals(requestBody, other.requestBody)
				&& Objects.equals(sourceFileEtag, other.sourceFileEtag)
				&& Objects.equals(sourceFileHandleId, other.sourceFileHandleId)
				&& Objects.equals(uploadToken, other.uploadToken) && uploadType == other.uploadType
				&& Objects.equals(userId, other.userId);
	}

	@Override
	public String toString() {
		return "CreateMultipartRequest [userId=" + userId + ", hash=" + hash + ", requestBody=" + requestBody
				+ ", uploadToken=" + uploadToken + ", uploadType=" + uploadType + ", bucket=" + bucket + ", key=" + key
				+ ", numberOfParts=" + numberOfParts + ", partSize=" + partSize + ", sourceFileHandleId="
				+ sourceFileHandleId + ", sourceFileEtag=" + sourceFileEtag + "]";
	}

}
