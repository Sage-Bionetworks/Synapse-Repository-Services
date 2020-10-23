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

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getRequestBody() {
		return requestBody;
	}

	public void setRequestBody(String requestBody) {
		this.requestBody = requestBody;
	}

	public String getUploadToken() {
		return uploadToken;
	}

	public void setUploadToken(String uploadToken) {
		this.uploadToken = uploadToken;
	}

	public UploadType getUploadType() {
		return uploadType;
	}

	public void setUploadType(UploadType uploadType) {
		this.uploadType = uploadType;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Integer getNumberOfParts() {
		return numberOfParts;
	}

	public void setNumberOfParts(Integer numberOfParts) {
		this.numberOfParts = numberOfParts;
	}

	public String getSourceFileHandleId() {
		return sourceFileHandleId;
	}
	
	public void setSourceFileHandleId(String sourceFileHandleId) {
		this.sourceFileHandleId = sourceFileHandleId;
	}
	
	public String getSourceFileEtag() {
		return sourceFileEtag;
	}
	
	public void setSourceFileEtag(String sourceFileEtag) {
		this.sourceFileEtag = sourceFileEtag;
	}

	public Long getPartSize() {
		return partSize;
	}

	public void setPartSize(Long partSize) {
		this.partSize = partSize;
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
