package org.sagebionetworks.aws.v2;

import java.util.Objects;

/**
 * A single S3 object tag.
 */
public class S3ObjectTag {

	private final String key;
	private final String value;

	public S3ObjectTag(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof S3ObjectTag)) {
			return false;
		}
		S3ObjectTag other = (S3ObjectTag) obj;
		return Objects.equals(key, other.key) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "S3ObjectTag [key=" + key + ", value=" + value + "]";
	}

}
