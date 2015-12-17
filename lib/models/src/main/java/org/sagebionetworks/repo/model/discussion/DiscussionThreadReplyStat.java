package org.sagebionetworks.repo.model.discussion;

/**
 * This DTO represents the statistic of a thread that related to its replies.
 */
public class DiscussionThreadReplyStat {

	Long threadId;
	Long numberOfReplies;
	Long lastActivity;
	public Long getThreadId() {
		return threadId;
	}
	public void setThreadId(Long threadId) {
		this.threadId = threadId;
	}
	public Long getNumberOfReplies() {
		return numberOfReplies;
	}
	public void setNumberOfReplies(Long numberOfReplies) {
		this.numberOfReplies = numberOfReplies;
	}
	public Long getLastActivity() {
		return lastActivity;
	}
	public void setLastActivity(Long lastActivity) {
		this.lastActivity = lastActivity;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((lastActivity == null) ? 0 : lastActivity.hashCode());
		result = prime * result
				+ ((numberOfReplies == null) ? 0 : numberOfReplies.hashCode());
		result = prime * result
				+ ((threadId == null) ? 0 : threadId.hashCode());
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
		DiscussionThreadReplyStat other = (DiscussionThreadReplyStat) obj;
		if (lastActivity == null) {
			if (other.lastActivity != null)
				return false;
		} else if (!lastActivity.equals(other.lastActivity))
			return false;
		if (numberOfReplies == null) {
			if (other.numberOfReplies != null)
				return false;
		} else if (!numberOfReplies.equals(other.numberOfReplies))
			return false;
		if (threadId == null) {
			if (other.threadId != null)
				return false;
		} else if (!threadId.equals(other.threadId))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DiscussionThreadReplyStat [threadId=" + threadId
				+ ", numberOfReplies=" + numberOfReplies + ", lastActivity="
				+ lastActivity + "]";
	}
}
