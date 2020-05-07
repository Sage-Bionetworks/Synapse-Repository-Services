package org.sagebionetworks.repo.model.table;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;

/**
 * DTO that specifies the scope filter to apply when working with a view
 * 
 * @author Marco Marasca
 */
public class ViewScopeFilter {

	private final ObjectType objectType;
	private final List<Enum<?>> subTypes;
	private final boolean filterByObjectId;
	private final Set<Long> containerIds;

	public ViewScopeFilter(ObjectType objectType, List<Enum<?>> subTypes, boolean filterByObjectId, Set<Long> containerIds) {
		this.objectType = objectType;
		this.subTypes = subTypes;
		this.filterByObjectId = filterByObjectId;
		this.containerIds = containerIds;
	}
	
	public ObjectType getObjectType() {
		return objectType;
	}

	public List<Enum<?>> getSubTypes() {
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
