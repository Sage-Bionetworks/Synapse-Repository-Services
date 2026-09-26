package org.sagebionetworks.aws.v2;

import java.util.Objects;

/**
 * Response headers that S3 should override when a pre-signed download URL is redeemed. A null field
 * leaves the value stored on the object in place.
 */
public class S3ResponseHeaders {

	private final String contentType;
	private final String contentDisposition;

	public S3ResponseHeaders(String contentType, String contentDisposition) {
		this.contentType = contentType;
		this.contentDisposition = contentDisposition;
	}

	public String getContentType() {
		return contentType;
	}

	public String getContentDisposition() {
		return contentDisposition;
	}

	@Override
	public int hashCode() {
		return Objects.hash(contentDisposition, contentType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof S3ResponseHeaders)) {
			return false;
		}
		S3ResponseHeaders other = (S3ResponseHeaders) obj;
		return Objects.equals(contentDisposition, other.contentDisposition)
				&& Objects.equals(contentType, other.contentType);
	}

	@Override
	public String toString() {
		return "S3ResponseHeaders [contentType=" + contentType + ", contentDisposition=" + contentDisposition + "]";
	}

}
