package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A stat for a project and user
 */
public class ProjectStat {

	long projectId;
	long userId;
	Date lastAccessed;

	public ProjectStat(long projectId, long userId, Date lastAccessed) {
		this.projectId = projectId;
		this.userId = userId;
		this.lastAccessed = lastAccessed;
	}

	public long getProjectId() {
		return projectId;
	}

	public void setProjectId(long projectId) {
		this.projectId = projectId;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public Date getLastAccessed() {
		return lastAccessed;
	}

	public void setLastAccessed(Date lastAccessed) {
		this.lastAccessed = lastAccessed;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lastAccessed == null) ? 0 : lastAccessed.hashCode());
		result = prime * result + (int) (projectId ^ (projectId >>> 32));
		result = prime * result + (int) (userId ^ (userId >>> 32));
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
		ProjectStat other = (ProjectStat) obj;
		if (lastAccessed == null) {
			if (other.lastAccessed != null)
				return false;
		} else if (!lastAccessed.equals(other.lastAccessed))
			return false;
		if (projectId != other.projectId)
			return false;
		if (userId != other.userId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ProjectStat [projectId=" + projectId + ", userId=" + userId + ", lastAccessed=" + lastAccessed + "]";
	}
}
