package org.sagebionetworks.repo.model.table;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * DTO that specifies the scope filter to apply when working with a view
 * 
 * @author Marco Marasca
 */
public class ViewScopeFilter  {

	private final MainType mainType;
	private final List<String> subTypes;
	private final boolean filterByObjectId;
	private final Set<Long> containerIds;

	public ViewScopeFilter(MainType mainType, List<String> subTypes, boolean filterByObjectId, Set<Long> containerIds) {
		this.mainType = mainType;
		this.subTypes = subTypes;
		this.filterByObjectId = filterByObjectId;
		this.containerIds = containerIds;
	}
	
	public MainType getMainType() {
		return mainType;
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
		return Objects.hash(containerIds, filterByObjectId, mainType, subTypes);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ViewScopeFilter)) {
			return false;
		}
		ViewScopeFilter other = (ViewScopeFilter) obj;
		return Objects.equals(containerIds, other.containerIds) && filterByObjectId == other.filterByObjectId
				&& mainType == other.mainType && Objects.equals(subTypes, other.subTypes);
	}
	
}
