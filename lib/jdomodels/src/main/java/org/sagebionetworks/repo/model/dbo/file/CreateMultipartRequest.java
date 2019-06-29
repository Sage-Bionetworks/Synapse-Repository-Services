package org.sagebionetworks.repo.model.dbo.file;

import org.sagebionetworks.repo.model.file.UploadType;

/**
 * DTO to create a new multi-part request.
 *
 */
public class CreateMultipartRequest {

	Long userId;
	String hash;
	String requestBody;
	String uploadToken;
	UploadType uploadType;
	String bucket;
	String key;
	Integer numberOfParts;
	
	/**
	 * 
	 * @param userId The Id of the user that started the upload.
	 * @param hash The hash represents request.
	 * @param requestBody
	 * @param uploadToken
	 * @param bucket
	 * @param key
	 */
	public CreateMultipartRequest(Long userId, String hash,
			String requestBody, String uploadToken, UploadType uploadType, String bucket, String key, Integer numberOfParts) {
		super();
		this.userId = userId;
		this.hash = hash;
		this.requestBody = requestBody;
		this.uploadToken = uploadToken;
		this.uploadType = uploadType;
		this.bucket = bucket;
		this.key = key;
		this.numberOfParts = numberOfParts;
	}
	public CreateMultipartRequest() {
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
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uploadType == null) ? 0 : uploadType.hashCode());
		result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
		result = prime * result + ((hash == null) ? 0 : hash.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result
				+ ((numberOfParts == null) ? 0 : numberOfParts.hashCode());
		result = prime * result
				+ ((requestBody == null) ? 0 : requestBody.hashCode());
		result = prime * result
				+ ((uploadToken == null) ? 0 : uploadToken.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CreateMultipartRequest other = (CreateMultipartRequest) obj;
		if (bucket == null) {
			if (other.bucket != null)
				return false;
		} else if (!bucket.equals(other.bucket))
			return false;
		if (uploadType == null) {
			if (other.uploadType != null)
				return false;
		} else if (!uploadType.equals(other.uploadType))
			return false;
		if (hash == null) {
			if (other.hash != null)
				return false;
		} else if (!hash.equals(other.hash))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (numberOfParts == null) {
			if (other.numberOfParts != null)
				return false;
		} else if (!numberOfParts.equals(other.numberOfParts))
			return false;
		if (requestBody == null) {
			if (other.requestBody != null)
				return false;
		} else if (!requestBody.equals(other.requestBody))
			return false;
		if (uploadToken == null) {
			if (other.uploadToken != null)
				return false;
		} else if (!uploadToken.equals(other.uploadToken))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "CreateMultipartRequest [userId=" + userId + ", hash=" + hash
				+ ", requestBody=" + requestBody + ", uploadToken="
				+ uploadToken + ", uploadType=" + uploadType
				+ " ,bucket=" + bucket + ", key=" + key
				+ ", numberOfParts=" + numberOfParts + "]";
	}
	
}
