package org.sagebionetworks.repo.model.dao.subscription;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.subscription.SortByType;
import org.sagebionetworks.repo.model.subscription.SortDirection;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;

/**
 * Request to list subscriptions for different object types.
 *
 */
public class SubscriptionListRequest {
	
	String subscriberId;
	SubscriptionObjectType objectType;
	List<String> objectIds;
	Set<Long> projectIds;
	SortByType sortByType;
	SortDirection sortDirection;
	Long limit;
	Long offset;
	
	public String getSubscriberId() {
		return subscriberId;
	}
	public SubscriptionObjectType getObjectType() {
		return objectType;
	}
	public List<String> getObjectIds() {
		return objectIds;
	}
	public Set<Long> getProjectIds() {
		return projectIds;
	}
	public SortByType getSortByType() {
		return sortByType;
	}
	public SortDirection getSortDirection() {
		return sortDirection;
	}
	public Long getLimit() {
		return limit;
	}
	public Long getOffset() {
		return offset;
	}
	
	public SubscriptionListRequest withSubscriberId(String subscriberId) {
		this.subscriberId = subscriberId;
		return this;
	}
	public SubscriptionListRequest withObjectType(SubscriptionObjectType objectType) {
		this.objectType = objectType;
		return this;
	}
	public SubscriptionListRequest withObjectIds(List<String> objectIds) {
		this.objectIds = objectIds;
		return this;
	}
	public SubscriptionListRequest withProjectIds(Set<Long> projectIds) {
		this.projectIds = projectIds;
		return this;
	}
	public SubscriptionListRequest withSortByType(SortByType sortByType) {
		this.sortByType = sortByType;
		return this;
	}
	public SubscriptionListRequest withSortDirection(SortDirection sortDirection) {
		this.sortDirection = sortDirection;
		return this;
	}
	public SubscriptionListRequest withLimit(Long limit) {
		this.limit = limit;
		return this;
	}
	public SubscriptionListRequest withOffset(Long offset) {
		this.offset = offset;
		return this;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((limit == null) ? 0 : limit.hashCode());
		result = prime * result + ((objectIds == null) ? 0 : objectIds.hashCode());
		result = prime * result + ((objectType == null) ? 0 : objectType.hashCode());
		result = prime * result + ((offset == null) ? 0 : offset.hashCode());
		result = prime * result + ((projectIds == null) ? 0 : projectIds.hashCode());
		result = prime * result + ((sortByType == null) ? 0 : sortByType.hashCode());
		result = prime * result + ((sortDirection == null) ? 0 : sortDirection.hashCode());
		result = prime * result + ((subscriberId == null) ? 0 : subscriberId.hashCode());
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
		SubscriptionListRequest other = (SubscriptionListRequest) obj;
		if (limit == null) {
			if (other.limit != null)
				return false;
		} else if (!limit.equals(other.limit))
			return false;
		if (objectIds == null) {
			if (other.objectIds != null)
				return false;
		} else if (!objectIds.equals(other.objectIds))
			return false;
		if (objectType != other.objectType)
			return false;
		if (offset == null) {
			if (other.offset != null)
				return false;
		} else if (!offset.equals(other.offset))
			return false;
		if (projectIds == null) {
			if (other.projectIds != null)
				return false;
		} else if (!projectIds.equals(other.projectIds))
			return false;
		if (sortByType != other.sortByType)
			return false;
		if (sortDirection != other.sortDirection)
			return false;
		if (subscriberId == null) {
			if (other.subscriberId != null)
				return false;
		} else if (!subscriberId.equals(other.subscriberId))
			return false;
		return true;
	}

}
