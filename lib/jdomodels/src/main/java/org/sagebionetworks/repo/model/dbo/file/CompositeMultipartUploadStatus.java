package org.sagebionetworks.repo.model.dbo.file;

import org.sagebionetworks.repo.model.file.MultipartUploadStatus;

/**
 * Includes the MultipartUploadStatus exposed in the API and data used internally.
 *
 */
public class CompositeMultipartUploadStatus {

	MultipartUploadStatus multipartUploadStatus;
	String uploadToken;
	String bucket;
	String key;
	
	public MultipartUploadStatus getMultipartUploadStatus() {
		return multipartUploadStatus;
	}
	public void setMultipartUploadStatus(MultipartUploadStatus multipartUploadStatus) {
		this.multipartUploadStatus = multipartUploadStatus;
	}
	public String getUploadToken() {
		return uploadToken;
	}
	public void setUploadToken(String uploadToken) {
		this.uploadToken = uploadToken;
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
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime
				* result
				+ ((multipartUploadStatus == null) ? 0 : multipartUploadStatus
						.hashCode());
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
		CompositeMultipartUploadStatus other = (CompositeMultipartUploadStatus) obj;
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
		if (multipartUploadStatus == null) {
			if (other.multipartUploadStatus != null)
				return false;
		} else if (!multipartUploadStatus.equals(other.multipartUploadStatus))
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
		return "CompleteMultipartUploadStatus [multipartUploadStatus="
				+ multipartUploadStatus + ", uploadToken=" + uploadToken
				+ ", bucket=" + bucket + ", key=" + key + "]";
	}

}
