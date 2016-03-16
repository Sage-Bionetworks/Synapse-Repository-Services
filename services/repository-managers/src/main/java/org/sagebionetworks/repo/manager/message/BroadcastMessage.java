package org.sagebionetworks.repo.manager.message;

import org.apache.http.entity.ContentType;
import org.sagebionetworks.repo.model.subscription.Topic;

/**
 * Metada to send an email to a user.
 *
 */
public class BroadcastMessage {

	Topic topic;
	String body;
	String subject;
	ContentType contentType;
	
	/**
	 * All users subscribed to this topic should receive this message.
	 * 
	 * @return
	 */
	public Topic getTopic() {
		return topic;
	}
	/**
	 * All users subscribed to this topic should receive this message.
	 * @param topic
	 */
	public void setTopic(Topic topic) {
		this.topic = topic;
	}
	/**
	 * The body of the email.
	 * @return
	 */
	public String getBody() {
		return body;
	}
	/**
	 * The body of the email
	 * @param body
	 */
	public void setBody(String body) {
		this.body = body;
	}
	/**
	 * The subject line of the email.
	 * @return
	 */
	public String getSubject() {
		return subject;
	}
	/**
	 * The subject line of the email.
	 * @param subject
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}
	/**
	 * The content type of the email.
	 * @return
	 */
	public ContentType getContentType() {
		return contentType;
	}
	/**
	 * The content type of the email.
	 * @param contentType
	 */
	public void setContentType(ContentType contentType) {
		this.contentType = contentType;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result
				+ ((contentType == null) ? 0 : contentType.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
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
		BroadcastMessage other = (BroadcastMessage) obj;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		if (contentType == null) {
			if (other.contentType != null)
				return false;
		} else if (!contentType.equals(other.contentType))
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		if (topic == null) {
			if (other.topic != null)
				return false;
		} else if (!topic.equals(other.topic))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "BroadcastMessage [topic=" + topic + ", body=" + body
				+ ", subject=" + subject + ", contentType=" + contentType + "]";
	}
	
	
}
