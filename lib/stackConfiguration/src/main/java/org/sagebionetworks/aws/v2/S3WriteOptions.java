package org.sagebionetworks.aws.v2;

import java.util.Objects;

/**
 * The metadata, storage class and access control to apply to an object written through
 * {@link S3ObjectStore}. A null field leaves S3's default in place.
 */
public class S3WriteOptions {

	private static final S3WriteOptions EMPTY = builder().build();

	private final String contentType;
	private final String contentEncoding;
	private final String contentDisposition;
	private final String contentMd5Base64;
	private final S3StorageClass storageClass;
	private final S3CannedAcl cannedAcl;

	private S3WriteOptions(Builder builder) {
		this.contentType = builder.contentType;
		this.contentEncoding = builder.contentEncoding;
		this.contentDisposition = builder.contentDisposition;
		this.contentMd5Base64 = builder.contentMd5Base64;
		this.storageClass = builder.storageClass;
		this.cannedAcl = builder.cannedAcl;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * @return Options that set nothing, for writes that take S3's defaults for every value.
	 */
	public static S3WriteOptions empty() {
		return EMPTY;
	}

	public String getContentType() {
		return contentType;
	}

	public String getContentEncoding() {
		return contentEncoding;
	}

	public String getContentDisposition() {
		return contentDisposition;
	}

	/**
	 * @return The expected MD5 of the payload, base64 encoded (not hex) as S3 requires.
	 */
	public String getContentMd5Base64() {
		return contentMd5Base64;
	}

	public S3StorageClass getStorageClass() {
		return storageClass;
	}

	public S3CannedAcl getCannedAcl() {
		return cannedAcl;
	}

	@Override
	public int hashCode() {
		return Objects.hash(cannedAcl, contentDisposition, contentEncoding, contentMd5Base64, contentType,
				storageClass);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof S3WriteOptions)) {
			return false;
		}
		S3WriteOptions other = (S3WriteOptions) obj;
		return cannedAcl == other.cannedAcl && Objects.equals(contentDisposition, other.contentDisposition)
				&& Objects.equals(contentEncoding, other.contentEncoding)
				&& Objects.equals(contentMd5Base64, other.contentMd5Base64)
				&& Objects.equals(contentType, other.contentType) && storageClass == other.storageClass;
	}

	@Override
	public String toString() {
		return "S3WriteOptions [contentType=" + contentType + ", contentEncoding=" + contentEncoding
				+ ", contentDisposition=" + contentDisposition + ", contentMd5Base64=" + contentMd5Base64
				+ ", storageClass=" + storageClass + ", cannedAcl=" + cannedAcl + "]";
	}

	public static class Builder {

		private String contentType;
		private String contentEncoding;
		private String contentDisposition;
		private String contentMd5Base64;
		private S3StorageClass storageClass;
		private S3CannedAcl cannedAcl;

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

		public Builder withContentDisposition(String contentDisposition) {
			this.contentDisposition = contentDisposition;
			return this;
		}

		public Builder withContentMd5Base64(String contentMd5Base64) {
			this.contentMd5Base64 = contentMd5Base64;
			return this;
		}

		public Builder withStorageClass(S3StorageClass storageClass) {
			this.storageClass = storageClass;
			return this;
		}

		public Builder withCannedAcl(S3CannedAcl cannedAcl) {
			this.cannedAcl = cannedAcl;
			return this;
		}

		public S3WriteOptions build() {
			return new S3WriteOptions(this);
		}

	}

}
