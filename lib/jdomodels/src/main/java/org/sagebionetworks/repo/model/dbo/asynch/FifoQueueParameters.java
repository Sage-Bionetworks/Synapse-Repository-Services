package org.sagebionetworks.repo.model.dbo.asynch;

import java.util.Objects;

public class FifoQueueParameters {

	private String messageDeduplicationId;
	private String messageGroupId;

	public String getMessageDeduplicationId() {
		return messageDeduplicationId;
	}

	public String getMessageGroupId() {
		return messageGroupId;
	}

	public FifoQueueParameters setMessageDeduplicationId(String messageDeduplicationId) {
		this.messageDeduplicationId = messageDeduplicationId;
		return this;
	}

	public FifoQueueParameters setMessageGroupId(String messageGroupId) {
		this.messageGroupId = messageGroupId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(messageDeduplicationId, messageGroupId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FifoQueueParameters other = (FifoQueueParameters) obj;
		return Objects.equals(messageDeduplicationId, other.messageDeduplicationId)
				&& Objects.equals(messageGroupId, other.messageGroupId);
	}

	@Override
	public String toString() {
		return "FifoQueueParameters [messageDeduplicationId=" + messageDeduplicationId + ", messageGroupId="
				+ messageGroupId + "]";
	}

}
