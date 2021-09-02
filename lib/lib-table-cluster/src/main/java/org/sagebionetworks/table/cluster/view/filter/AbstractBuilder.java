package org.sagebionetworks.table.cluster.view.filter;

import java.util.Set;

import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;

public abstract class AbstractBuilder implements ViewFilterBuilder {
	
	protected ReplicationType mainType;
	protected Set<SubType> subTypes;
	protected Set<Long> limitObjectIds;
	protected Set<String> excludeKeys;

	public AbstractBuilder(ReplicationType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds, Set<String> excludeKeys) {
		super();
		this.mainType = mainType;
		this.subTypes = subTypes;
		this.limitObjectIds = limitObjectIds;
		this.excludeKeys = excludeKeys;
	}

	@Override
	public ViewFilterBuilder addLimitObjectids(Set<Long> limitObjectIds) {
		this.limitObjectIds = limitObjectIds;
		return this;
	}

	@Override
	public ViewFilterBuilder addExcludeAnnotationKeys(Set<String> excludeKeys) {
		this.excludeKeys = excludeKeys;
		return this;
	}

}
