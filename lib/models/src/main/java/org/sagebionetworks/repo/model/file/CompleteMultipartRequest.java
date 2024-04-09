package org.sagebionetworks.repo.model.file;

import java.util.List;

/**
 * DTO to complete a multi-part upload.
 * 
 */
public class CompleteMultipartRequest {

	Long uploadId;
	String uploadToken;
	Long numberOfParts;
	List<PartMD5> addedParts;
	String bucket;
	String key;

	public Long getUploadId() {
		return uploadId;
	}
	public CompleteMultipartRequest setUploadId(Long uploadId) {
		this.uploadId = uploadId;
		return this;
	}
	public String getUploadToken() {
		return uploadToken;
	}
	public CompleteMultipartRequest setUploadToken(String uploadToken) {
		this.uploadToken = uploadToken;
		return this;
	}
	public Long getNumberOfParts() {
		return numberOfParts;
	}
	public CompleteMultipartRequest setNumberOfParts(Long numberOfParts) {
		this.numberOfParts = numberOfParts;
		return this;
	}
	public List<PartMD5> getAddedParts() {
		return addedParts;
	}
	public CompleteMultipartRequest setAddedParts(List<PartMD5> addedParts) {
		this.addedParts = addedParts;
		return this;
	}
	public String getBucket() {
		return bucket;
	}
	public CompleteMultipartRequest setBucket(String bucket) {
		this.bucket = bucket;
		return this;
	}
	public String getKey() {
		return key;
	}
	public CompleteMultipartRequest setKey(String key) {
		this.key = key;
		return this;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((uploadId == null) ? 0 : uploadId.hashCode());
		result = prime * result
				+ ((numberOfParts == null) ? 0 : numberOfParts.hashCode());
		result = prime * result
				+ ((addedParts == null) ? 0 : addedParts.hashCode());
		result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result
				+ ((uploadToken == null) ? 0 : uploadToken.hashCode());
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
		CompleteMultipartRequest other = (CompleteMultipartRequest) obj;
		if (uploadId == null) {
			if (other.uploadId != null)
				return false;
		} else if (!uploadId.equals(other.uploadId))
			return false;
		if (numberOfParts == null) {
			if (other.numberOfParts != null)
				return false;
		} else if (!numberOfParts.equals(other.numberOfParts))
			return false;
		if (addedParts == null) {
			if (other.addedParts != null)
				return false;
		} else if (!addedParts.equals(other.addedParts))
			return false;
		if (bucket == null) {
			if (other.bucket != null)
				return false;
		} else if (!bucket.equals(other.bucket))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (uploadToken == null) {
			if (other.uploadToken != null)
				return false;
		} else if (!uploadToken.equals(other.uploadToken))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "CompleteMultipartRequest ["
				+ "uploadId =" + uploadId
				+ ", uploadToken=" + uploadToken
				+ ", numberOfParts=" + numberOfParts
				+ ", addedParts=" + addedParts
				+ ", bucket=" + bucket
				+ ", key=" + key + "]";
	}

}
