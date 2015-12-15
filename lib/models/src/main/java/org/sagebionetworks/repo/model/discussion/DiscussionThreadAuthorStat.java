package org.sagebionetworks.repo.model.discussion;

import java.util.List;

/**
 * This DTO represents the statistic about contributors of a thread.
 */
public class DiscussionThreadAuthorStat {

	Long threadId;
	List<String> activeAuthors;
	public Long getThreadId() {
		return threadId;
	}
	public void setThreadId(Long threadId) {
		this.threadId = threadId;
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
		result = prime * result
				+ ((activeAuthors == null) ? 0 : activeAuthors.hashCode());
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
		DiscussionThreadAuthorStat other = (DiscussionThreadAuthorStat) obj;
		if (activeAuthors == null) {
			if (other.activeAuthors != null)
				return false;
		} else if (!activeAuthors.equals(other.activeAuthors))
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
		return "DiscussionThreadAuthorStat [threadId=" + threadId
				+ ", activeAuthors=" + activeAuthors + "]";
	}
}
