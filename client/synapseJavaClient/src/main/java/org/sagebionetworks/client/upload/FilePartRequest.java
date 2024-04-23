package org.sagebionetworks.client.upload;

import java.io.File;
import java.util.Objects;

import org.apache.http.impl.client.CloseableHttpClient;
import org.sagebionetworks.client.SynapseClient;

/**
 * Everything needed to upload a single part from within its original file.
 *
 */
public class FilePartRequest {

	private SynapseClient synapseClient;
	private String uploadId;
	private Long partNumber;
	private File file;
	private Long partLength;
	private Long partOffset;
	private CloseableHttpClient httpClient;

	public SynapseClient getSynapseClient() {
		return synapseClient;
	}

	public FilePartRequest setSynapseClient(SynapseClient synapseClient) {
		this.synapseClient = synapseClient;
		return this;
	}

	public String getUploadId() {
		return uploadId;
	}

	public FilePartRequest setUploadId(String uploadId) {
		this.uploadId = uploadId;
		return this;
	}

	public Long getPartNumber() {
		return partNumber;
	}

	public FilePartRequest setPartNumber(Long partNumber) {
		this.partNumber = partNumber;
		return this;
	}

	public File getFile() {
		return file;
	}

	public FilePartRequest setFile(File file) {
		this.file = file;
		return this;
	}

	public Long getPartLength() {
		return partLength;
	}

	public FilePartRequest setPartLength(Long partLength) {
		this.partLength = partLength;
		return this;
	}

	public Long getPartOffset() {
		return partOffset;
	}

	public FilePartRequest setPartOffset(Long partOffset) {
		this.partOffset = partOffset;
		return this;
	}

	public CloseableHttpClient getHttpClient() {
		return httpClient;
	}

	public FilePartRequest setHttpClient(CloseableHttpClient httpClient) {
		this.httpClient = httpClient;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(file, httpClient, partLength, partNumber, partOffset, synapseClient, uploadId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FilePartRequest other = (FilePartRequest) obj;
		return Objects.equals(file, other.file) && Objects.equals(httpClient, other.httpClient)
				&& Objects.equals(partLength, other.partLength) && Objects.equals(partNumber, other.partNumber)
				&& Objects.equals(partOffset, other.partOffset) && Objects.equals(synapseClient, other.synapseClient)
				&& Objects.equals(uploadId, other.uploadId);
	}

	@Override
	public String toString() {
		return "FilePartRequest [synapseClient=" + synapseClient + ", uploadId=" + uploadId + ", partNumber="
				+ partNumber + ", file=" + file + ", partLength=" + partLength + ", partOffset=" + partOffset
				+ ", httpClient=" + httpClient + "]";
	}
	
	

}
