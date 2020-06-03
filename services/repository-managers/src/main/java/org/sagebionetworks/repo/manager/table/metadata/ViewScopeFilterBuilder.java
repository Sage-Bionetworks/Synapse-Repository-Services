package org.sagebionetworks.repo.manager.table.metadata;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.table.ViewScopeFilter;
import org.sagebionetworks.util.ValidateArgument;

public class ViewScopeFilterBuilder {
	
	private final ViewScopeFilterProvider provider;
	private final Long viewTypeMask;
	private Set<Long> containerIds;
	
	public ViewScopeFilterBuilder(ViewScopeFilterProvider provider, Long viewTypeMask) {
		this.provider = provider;
		this.viewTypeMask = viewTypeMask;
	}
	
	public ViewScopeFilterBuilder withContainerIds(Set<Long> containerIds) {
		this.containerIds = containerIds;
		return this;
	}
	
	public ViewScopeFilter build() {
		ValidateArgument.required(provider, "provider");
				
		List<String> subTypes = provider.getSubTypesForMask(viewTypeMask);
		boolean filterByObjectId = provider.isFilterScopeByObjectId(viewTypeMask);
		Set<Long> containerIds = this.containerIds == null ? Collections.emptySet() : this.containerIds;
		
		return new ViewScopeFilter(provider.getObjectType(), subTypes, filterByObjectId, containerIds);
	}

}
