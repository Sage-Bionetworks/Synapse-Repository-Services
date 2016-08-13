package org.sagebionetworks.repo.model.discussion;

import java.util.List;

public class DiscussionThreadStat {

	Long threadId;
	Long numberOfReplies;
	Long lastActivity;
	Long numberOfViews;
	List<String> activeAuthors;
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
	public Long getNumberOfViews() {
		return numberOfViews;
	}
	public void setNumberOfViews(Long numberOfViews) {
		this.numberOfViews = numberOfViews;
	}
	public List<String> getActiveAuthors() {
		return activeAuthors;
	}
	public void setActiveAuthors(List<String> activeAuthors) {
		this.activeAuthors = activeAuthors;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((activeAuthors == null) ? 0 : activeAuthors.hashCode());
		result = prime * result + ((lastActivity == null) ? 0 : lastActivity.hashCode());
		result = prime * result + ((numberOfReplies == null) ? 0 : numberOfReplies.hashCode());
		result = prime * result + ((numberOfViews == null) ? 0 : numberOfViews.hashCode());
		result = prime * result + ((threadId == null) ? 0 : threadId.hashCode());
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
		DiscussionThreadStat other = (DiscussionThreadStat) obj;
		if (activeAuthors == null) {
			if (other.activeAuthors != null)
				return false;
		} else if (!activeAuthors.equals(other.activeAuthors))
			return false;
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
		if (numberOfViews == null) {
			if (other.numberOfViews != null)
				return false;
		} else if (!numberOfViews.equals(other.numberOfViews))
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
		return "DiscussionThreadStat [threadId=" + threadId + ", numberOfReplies=" + numberOfReplies + ", lastActivity="
				+ lastActivity + ", numberOfViews=" + numberOfViews + ", activeAuthors=" + activeAuthors + "]";
	}
}
