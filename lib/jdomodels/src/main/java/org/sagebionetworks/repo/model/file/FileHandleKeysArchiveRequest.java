package org.sagebionetworks.repo.model.file;

import java.util.List;
import java.util.Objects;

/**
 * DTO that contains the set of keys to potentially archive for a given bucket
 */
public class FileHandleKeysArchiveRequest {

	private Long modifiedBefore;
	private String bucket;
	private List<String> keys;

	public FileHandleKeysArchiveRequest() {
	}

	public Long getModifiedBefore() {
		return modifiedBefore;
	}

	public FileHandleKeysArchiveRequest withModifiedBefore(Long modifiedBefore) {
		this.modifiedBefore = modifiedBefore;
		return this;
	}

	public String getBucket() {
		return bucket;
	}

	public FileHandleKeysArchiveRequest withBucket(String bucket) {
		this.bucket = bucket;
		return this;
	}

	public List<String> getKeys() {
		return keys;
	}

	public FileHandleKeysArchiveRequest withKeys(List<String> keys) {
		this.keys = keys;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bucket, keys, modifiedBefore);
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
		FileHandleKeysArchiveRequest other = (FileHandleKeysArchiveRequest) obj;
		return Objects.equals(bucket, other.bucket) && Objects.equals(keys, other.keys)
				&& Objects.equals(modifiedBefore, other.modifiedBefore);
	}

	@Override
	public String toString() {
		return "FileHandleKeysArchiveRequest [modifiedBefore=" + modifiedBefore + ", bucket=" + bucket + ", keys=" + keys + "]";
	}

}
