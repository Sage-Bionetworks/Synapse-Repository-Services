package org.sagebionetworks.evaluation.dbo;

import java.util.Arrays;
import java.util.Objects;

public class EvaluationBackup {
	private Long id;
	private String eTag;
	private String name;
	private byte[] description;
	private Long ownerId;
	private Long createdOn;
	private String contentSource;
	private int status;
	private byte[] submissionInstructions;
	private byte[] submissionReceiptMessage;
	private byte[] quota;
	private String quotaJson;
	private Long startTimestamp;
	private Long endTimestamp;
	
	
	public String getQuotaJson() {
		return quotaJson;
	}
	public void setQuotaJson(String quotaJson) {
		this.quotaJson = quotaJson;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String geteTag() {
		return eTag;
	}
	public void seteTag(String eTag) {
		this.eTag = eTag;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public byte[] getDescription() {
		return description;
	}
	public void setDescription(byte[] description) {
		this.description = description;
	}
	public Long getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}
	public Long getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}
	public String getContentSource() {
		return contentSource;
	}
	public void setContentSource(String contentSource) {
		this.contentSource = contentSource;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public byte[] getSubmissionInstructions() {
		return submissionInstructions;
	}
	public void setSubmissionInstructions(byte[] submissionInstructions) {
		this.submissionInstructions = submissionInstructions;
	}
	public byte[] getSubmissionReceiptMessage() {
		return submissionReceiptMessage;
	}
	public void setSubmissionReceiptMessage(byte[] submissionReceiptMessage) {
		this.submissionReceiptMessage = submissionReceiptMessage;
	}
	
	public byte[] getQuota() {
		return quota;
	}
	public void setQuota(byte[] quota) {
		this.quota = quota;
	}
	
	
	public Long getStartTimestamp() {
		return startTimestamp;
	}
	public void setStartTimestamp(Long startTimestamp) {
		this.startTimestamp = startTimestamp;
	}
	public Long getEndTimestamp() {
		return endTimestamp;
	}
	public void setEndTimestamp(Long endTimestamp) {
		this.endTimestamp = endTimestamp;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(description);
		result = prime * result + Arrays.hashCode(quota);
		result = prime * result + Arrays.hashCode(submissionInstructions);
		result = prime * result + Arrays.hashCode(submissionReceiptMessage);
		result = prime * result + Objects.hash(contentSource, createdOn, eTag, endTimestamp, id, name, ownerId,
				quotaJson, startTimestamp, status);
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
		EvaluationBackup other = (EvaluationBackup) obj;
		return Objects.equals(contentSource, other.contentSource) && Objects.equals(createdOn, other.createdOn)
				&& Arrays.equals(description, other.description) && Objects.equals(eTag, other.eTag)
				&& Objects.equals(endTimestamp, other.endTimestamp) && Objects.equals(id, other.id)
				&& Objects.equals(name, other.name) && Objects.equals(ownerId, other.ownerId)
				&& Arrays.equals(quota, other.quota) && Objects.equals(quotaJson, other.quotaJson)
				&& Objects.equals(startTimestamp, other.startTimestamp) && status == other.status
				&& Arrays.equals(submissionInstructions, other.submissionInstructions)
				&& Arrays.equals(submissionReceiptMessage, other.submissionReceiptMessage);
	}
	@Override
	public String toString() {
		return "EvaluationBackup [id=" + id + ", eTag=" + eTag + ", name=" + name + ", description="
				+ Arrays.toString(description) + ", ownerId=" + ownerId + ", createdOn=" + createdOn
				+ ", contentSource=" + contentSource + ", status=" + status + ", submissionInstructions="
				+ Arrays.toString(submissionInstructions) + ", submissionReceiptMessage="
				+ Arrays.toString(submissionReceiptMessage) + ", quota=" + Arrays.toString(quota) + ", startTimestamp="
				+ startTimestamp + ", endTimestamp=" + endTimestamp + "]";
	}
	
}
