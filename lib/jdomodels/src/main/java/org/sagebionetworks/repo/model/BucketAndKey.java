package org.sagebionetworks.repo.model;

/**
 * Buck and Key pair.
 *
 */
public class BucketAndKey {

	private String bucket;
	private String key;

	public String getBucket() {
		return bucket;
	}

	public String getKey() {
		return key;
	}

	public BucketAndKey withBucket(String bucket) {
		this.bucket = bucket;
		return this;
	}

	public BucketAndKey withtKey(String key) {
		this.key = key;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
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
		BucketAndKey other = (BucketAndKey) obj;
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
		return true;
	}

	@Override
	public String toString() {
		return "BucketAndKey [bucket=" + bucket + ", key=" + key + "]";
	}

}
