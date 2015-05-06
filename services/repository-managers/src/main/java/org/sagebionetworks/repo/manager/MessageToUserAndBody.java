package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.message.MessageToUser;

public class MessageToUserAndBody {
	private MessageToUser metadata;
	private String body;
	
	
	
	public MessageToUserAndBody(MessageToUser metadata, String body) {
		super();
		this.metadata = metadata;
		this.body = body;
	}
	
	public MessageToUser getMetadata() {
		return metadata;
	}
	public void setMetadata(MessageToUser metadata) {
		this.metadata = metadata;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result
				+ ((metadata == null) ? 0 : metadata.hashCode());
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
		MessageToUserAndBody other = (MessageToUserAndBody) obj;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		if (metadata == null) {
			if (other.metadata != null)
				return false;
		} else if (!metadata.equals(other.metadata))
			return false;
		return true;
	}
	
	

}
