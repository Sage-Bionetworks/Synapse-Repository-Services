package org.sagebionetworks.evaluation.dbo;

import java.util.Arrays;

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
	private Long startTimestamp;
	private Long endTimestamp;
	
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
		result = prime * result + ((contentSource == null) ? 0 : contentSource.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + Arrays.hashCode(description);
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((endTimestamp == null) ? 0 : endTimestamp.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
		result = prime * result + Arrays.hashCode(quota);
		result = prime * result + ((startTimestamp == null) ? 0 : startTimestamp.hashCode());
		result = prime * result + status;
		result = prime * result + Arrays.hashCode(submissionInstructions);
		result = prime * result + Arrays.hashCode(submissionReceiptMessage);
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
		if (contentSource == null) {
			if (other.contentSource != null)
				return false;
		} else if (!contentSource.equals(other.contentSource))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (!Arrays.equals(description, other.description))
			return false;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (endTimestamp == null) {
			if (other.endTimestamp != null)
				return false;
		} else if (!endTimestamp.equals(other.endTimestamp))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		if (!Arrays.equals(quota, other.quota))
			return false;
		if (startTimestamp == null) {
			if (other.startTimestamp != null)
				return false;
		} else if (!startTimestamp.equals(other.startTimestamp))
			return false;
		if (status != other.status)
			return false;
		if (!Arrays.equals(submissionInstructions, other.submissionInstructions))
			return false;
		if (!Arrays.equals(submissionReceiptMessage, other.submissionReceiptMessage))
			return false;
		return true;
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
