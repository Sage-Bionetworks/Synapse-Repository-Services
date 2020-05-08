package org.sagebionetworks.repo.model.table;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * DTO that specifies the scope filter to apply when working with a view
 * 
 * @author Marco Marasca
 */
public class ViewScopeFilter implements HasViewObjectType {

	private final ViewObjectType objectType;
	private final List<String> subTypes;
	private final boolean filterByObjectId;
	private final Set<Long> containerIds;

	public ViewScopeFilter(ViewObjectType objectType, List<String> subTypes, boolean filterByObjectId, Set<Long> containerIds) {
		this.objectType = objectType;
		this.subTypes = subTypes;
		this.filterByObjectId = filterByObjectId;
		this.containerIds = containerIds;
	}
	
	@Override
	public ViewObjectType getObjectType() {
		return objectType;
	}

	public List<String> getSubTypes() {
		return subTypes;
	}

	public Set<Long> getContainerIds() {
		return containerIds;
	}

	public boolean isFilterByObjectId() {
		return filterByObjectId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(containerIds, filterByObjectId, objectType, subTypes);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ViewScopeFilter other = (ViewScopeFilter) obj;
		return Objects.equals(containerIds, other.containerIds) && filterByObjectId == other.filterByObjectId
				&& objectType == other.objectType && Objects.equals(subTypes, other.subTypes);
	}
	
}
