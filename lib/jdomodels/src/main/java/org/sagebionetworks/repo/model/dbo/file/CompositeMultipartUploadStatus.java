package org.sagebionetworks.repo.model.dbo.file;

import java.util.Objects;

import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.UploadType;

/**
 * Includes the MultipartUploadStatus exposed in the API and data used internally.
 *
 */
public class CompositeMultipartUploadStatus {

	private MultipartUploadStatus multipartUploadStatus;
	private String etag;
	private String uploadToken;
	private String bucket;
	private String key;
	private Integer numberOfParts;
	private UploadType uploadType;
	private MultiPartRequestType requestType;
	private Long partSize;

	// The following is pulled in if the request type is a copy
	private Long sourceFileHandleId;
	private String sourceFileEtag;
	private String sourceBucket;
	private String sourceKey;
	private Long sourceFileSize;

	public MultipartUploadStatus getMultipartUploadStatus() {
		return multipartUploadStatus;
	}

	public void setMultipartUploadStatus(MultipartUploadStatus multipartUploadStatus) {
		this.multipartUploadStatus = multipartUploadStatus;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
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

	public Integer getNumberOfParts() {
		return numberOfParts;
	}

	public void setNumberOfParts(Integer numberOfParts) {
		this.numberOfParts = numberOfParts;
	}

	public UploadType getUploadType() {
		return uploadType;
	}

	public void setUploadType(UploadType uploadType) {
		this.uploadType = uploadType;
	}

	public MultiPartRequestType getRequestType() {
		return requestType;
	}

	public void setRequestType(MultiPartRequestType requestType) {
		this.requestType = requestType;
	}

	public Long getSourceFileHandleId() {
		return sourceFileHandleId;
	}

	public void setSourceFileHandleId(Long sourceFileHandleId) {
		this.sourceFileHandleId = sourceFileHandleId;
	}
	
	public String getSourceFileEtag() {
		return sourceFileEtag;
	}
	
	public void setSourceFileEtag(String sourceFileEtag) {
		this.sourceFileEtag = sourceFileEtag;
	}

	public String getSourceBucket() {
		return sourceBucket;
	}

	public void setSourceBucket(String sourceBucket) {
		this.sourceBucket = sourceBucket;
	}

	public String getSourceKey() {
		return sourceKey;
	}

	public void setSourceKey(String sourceKey) {
		this.sourceKey = sourceKey;
	}

	public Long getSourceFileSize() {
		return sourceFileSize;
	}
	
	public void setSourceFileSize(Long sourceFileSize) {
		this.sourceFileSize = sourceFileSize;
	}

	public Long getPartSize() {
		return partSize;
	}

	public void setPartSize(Long partSize) {
		this.partSize = partSize;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bucket, etag, sourceFileSize, key, multipartUploadStatus, numberOfParts, partSize, requestType,
				sourceBucket, sourceFileEtag, sourceFileHandleId, sourceKey, uploadToken, uploadType);
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
		CompositeMultipartUploadStatus other = (CompositeMultipartUploadStatus) obj;
		return Objects.equals(bucket, other.bucket) && Objects.equals(etag, other.etag)
				&& Objects.equals(sourceFileSize, other.sourceFileSize) && Objects.equals(key, other.key)
				&& Objects.equals(multipartUploadStatus, other.multipartUploadStatus)
				&& Objects.equals(numberOfParts, other.numberOfParts) && Objects.equals(partSize, other.partSize)
				&& requestType == other.requestType && Objects.equals(sourceBucket, other.sourceBucket)
				&& Objects.equals(sourceFileEtag, other.sourceFileEtag)
				&& Objects.equals(sourceFileHandleId, other.sourceFileHandleId)
				&& Objects.equals(sourceKey, other.sourceKey) && Objects.equals(uploadToken, other.uploadToken)
				&& uploadType == other.uploadType;
	}

	@Override
	public String toString() {
		return "CompositeMultipartUploadStatus [multipartUploadStatus=" + multipartUploadStatus + ", etag=" + etag
				+ ", uploadToken=" + uploadToken + ", bucket=" + bucket + ", key=" + key + ", numberOfParts="
				+ numberOfParts + ", uploadType=" + uploadType + ", requestType=" + requestType + ", partSize="
				+ partSize + ", sourceFileHandleId=" + sourceFileHandleId + ", sourceFileEtag=" + sourceFileEtag
				+ ", sourceBucket=" + sourceBucket + ", sourceKey=" + sourceKey + ", sourceFileSize=" + sourceFileSize + "]";
	}

}
