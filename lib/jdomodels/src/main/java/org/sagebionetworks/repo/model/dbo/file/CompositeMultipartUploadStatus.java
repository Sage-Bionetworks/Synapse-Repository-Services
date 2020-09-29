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
	private Long fileSize;
	private Long partSize;

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

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public Long getPartSize() {
		return partSize;
	}

	public void setPartSize(Long partSize) {
		this.partSize = partSize;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bucket, etag, fileSize, key, multipartUploadStatus, numberOfParts, partSize, requestType,
				uploadToken, uploadType);
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
				&& Objects.equals(fileSize, other.fileSize) && Objects.equals(key, other.key)
				&& Objects.equals(multipartUploadStatus, other.multipartUploadStatus)
				&& Objects.equals(numberOfParts, other.numberOfParts) && Objects.equals(partSize, other.partSize)
				&& requestType == other.requestType && Objects.equals(uploadToken, other.uploadToken)
				&& uploadType == other.uploadType;
	}

	@Override
	public String toString() {
		return "CompositeMultipartUploadStatus [multipartUploadStatus=" + multipartUploadStatus + ", etag=" + etag
				+ ", uploadToken=" + uploadToken + ", bucket=" + bucket + ", key=" + key + ", numberOfParts="
				+ numberOfParts + ", uploadType=" + uploadType + ", requestType=" + requestType + ", fileSize="
				+ fileSize + ", partSize=" + partSize + "]";
	}

}
