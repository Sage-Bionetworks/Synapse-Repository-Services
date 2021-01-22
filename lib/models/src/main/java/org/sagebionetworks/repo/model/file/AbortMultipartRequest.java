package org.sagebionetworks.repo.model.file;

import java.util.List;
import java.util.Objects;

public class AbortMultipartRequest {

	private String uploadId;
	private String uploadToken;
	private String bucket;
	private String key;
	private List<String> partKeys;

	public AbortMultipartRequest(String uploadId, String uploadToken, String bucket, String key) {
		this.uploadId = uploadId;
		this.uploadToken = uploadToken;
		this.bucket = bucket;
		this.key = key;
	}

	public String getUploadId() {
		return uploadId;
	}

	public String getUploadToken() {
		return uploadToken;
	}

	public String getBucket() {
		return bucket;
	}

	public String getKey() {
		return key;
	}

	public List<String> getPartKeys() {
		return partKeys;
	}
	
	public AbortMultipartRequest withPartKeys(List<String> partKeys) {
		this.partKeys = partKeys;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bucket, key, partKeys, uploadId, uploadToken);
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
		AbortMultipartRequest other = (AbortMultipartRequest) obj;
		return Objects.equals(bucket, other.bucket) && Objects.equals(key, other.key) && Objects.equals(partKeys, other.partKeys)
				&& Objects.equals(uploadId, other.uploadId) && Objects.equals(uploadToken, other.uploadToken);
	}

	@Override
	public String toString() {
		return "AbortMultipartRequest [uploadId=" + uploadId + ", uploadToken=" + uploadToken + ", bucket=" + bucket + ", key=" + key
				+ ", partKeys=" + partKeys + "]";
	}

}
