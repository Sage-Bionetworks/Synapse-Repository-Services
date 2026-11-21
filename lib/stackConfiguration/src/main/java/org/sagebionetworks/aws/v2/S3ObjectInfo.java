package org.sagebionetworks.aws.v2;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The metadata S3 reports for an existing object. Only the values Synapse reads are carried; a null
 * field means S3 did not report that value for the object.
 */
public class S3ObjectInfo {

	/**
	 * The 'x-amz-restore' header is a comma separated list of quoted directives, for example
	 * <code>ongoing-request="false", expiry-date="Fri, 21 Dec 2012 00:00:00 GMT"</code>.
	 */
	private static final Pattern ONGOING_REQUEST = Pattern.compile("ongoing-request=\"(.*?)\"");

	private final String contentType;
	private final String contentEncoding;
	private final String etag;
	private final Long contentLength;
	private final String archiveStatus;
	private final Boolean ongoingRestore;

	private S3ObjectInfo(Builder builder) {
		this.contentType = builder.contentType;
		this.contentEncoding = builder.contentEncoding;
		this.etag = builder.etag;
		this.contentLength = builder.contentLength;
		this.archiveStatus = builder.archiveStatus;
		this.ongoingRestore = builder.ongoingRestore;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getContentType() {
		return contentType;
	}

	public String getContentEncoding() {
		return contentEncoding;
	}

	public String getEtag() {
		return etag;
	}

	public Long getContentLength() {
		return contentLength;
	}

	/**
	 * @return The archive tier the object has been transitioned to, or null when the object is
	 *         directly readable.
	 */
	public String getArchiveStatus() {
		return archiveStatus;
	}

	/**
	 * @return True while a restore of an archived object is in progress, false once it has
	 *         completed, and null when no restore was ever requested.
	 */
	public Boolean getOngoingRestore() {
		return ongoingRestore;
	}

	/**
	 * Extracts the ongoing-restore flag from the raw value of the S3 'x-amz-restore' header.
	 *
	 * @param restoreHeader The raw header value, may be null
	 * @return Null when the header is absent or carries no ongoing-request directive
	 */
	public static Boolean parseOngoingRestore(String restoreHeader) {
		if (restoreHeader == null) {
			return null;
		}
		Matcher matcher = ONGOING_REQUEST.matcher(restoreHeader);
		if (!matcher.find()) {
			return null;
		}
		return Boolean.valueOf(matcher.group(1));
	}

	@Override
	public int hashCode() {
		return Objects.hash(archiveStatus, contentEncoding, contentLength, contentType, etag, ongoingRestore);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof S3ObjectInfo)) {
			return false;
		}
		S3ObjectInfo other = (S3ObjectInfo) obj;
		return Objects.equals(archiveStatus, other.archiveStatus)
				&& Objects.equals(contentEncoding, other.contentEncoding)
				&& Objects.equals(contentLength, other.contentLength)
				&& Objects.equals(contentType, other.contentType) && Objects.equals(etag, other.etag)
				&& Objects.equals(ongoingRestore, other.ongoingRestore);
	}

	@Override
	public String toString() {
		return "S3ObjectInfo [contentType=" + contentType + ", contentEncoding=" + contentEncoding + ", etag=" + etag
				+ ", contentLength=" + contentLength + ", archiveStatus=" + archiveStatus + ", ongoingRestore="
				+ ongoingRestore + "]";
	}

	public static class Builder {

		private String contentType;
		private String contentEncoding;
		private String etag;
		private Long contentLength;
		private String archiveStatus;
		private Boolean ongoingRestore;

		private Builder() {
		}

		public Builder withContentType(String contentType) {
			this.contentType = contentType;
			return this;
		}

		public Builder withContentEncoding(String contentEncoding) {
			this.contentEncoding = contentEncoding;
			return this;
		}

		public Builder withEtag(String etag) {
			this.etag = etag;
			return this;
		}

		public Builder withContentLength(Long contentLength) {
			this.contentLength = contentLength;
			return this;
		}

		public Builder withArchiveStatus(String archiveStatus) {
			this.archiveStatus = archiveStatus;
			return this;
		}

		public Builder withOngoingRestore(Boolean ongoingRestore) {
			this.ongoingRestore = ongoingRestore;
			return this;
		}

		public S3ObjectInfo build() {
			return new S3ObjectInfo(this);
		}

	}

}
