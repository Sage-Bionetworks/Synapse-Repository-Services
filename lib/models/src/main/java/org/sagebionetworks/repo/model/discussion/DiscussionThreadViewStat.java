package org.sagebionetworks.repo.model.discussion;

/**
 * This DTO represents the statistic about number of unique users viewing a thread.
 */
public class DiscussionThreadViewStat {

	Long threadId;
	Long numberOfViews;

	public Long getThreadId() {
		return threadId;
	}
	public void setThreadId(Long threadId) {
		this.threadId = threadId;
	}
	public Long getNumberOfViews() {
		return numberOfViews;
	}
	public void setNumberOfViews(Long numberOfViews) {
		this.numberOfViews = numberOfViews;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((numberOfViews == null) ? 0 : numberOfViews.hashCode());
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
		DiscussionThreadViewStat other = (DiscussionThreadViewStat) obj;
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
		return "DiscussionThreadViewStat [threadId=" + threadId
				+ ", numberOfViews=" + numberOfViews + "]";
	}
}
