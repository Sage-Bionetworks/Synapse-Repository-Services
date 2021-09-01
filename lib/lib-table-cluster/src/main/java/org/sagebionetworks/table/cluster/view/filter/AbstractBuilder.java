package org.sagebionetworks.table.cluster.view.filter;

import java.util.Set;

import org.sagebionetworks.repo.model.table.MainType;
import org.sagebionetworks.repo.model.table.SubType;

public abstract class AbstractBuilder implements Builder {
	
	protected MainType mainType;
	protected Set<SubType> subTypes;
	protected Set<Long> limitObjectIds;
	protected Set<String> excludeKeys;

	public AbstractBuilder(MainType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds, Set<String> excludeKeys) {
		super();
		this.mainType = mainType;
		this.subTypes = subTypes;
		this.limitObjectIds = limitObjectIds;
		this.excludeKeys = excludeKeys;
	}

	@Override
	public Builder addLimitObjectids(Set<Long> limitObjectIds) {
		this.limitObjectIds = limitObjectIds;
		return this;
	}

	@Override
	public Builder addExcludeAnnotationKeys(Set<String> excludeKeys) {
		this.excludeKeys = excludeKeys;
		return this;
	}

}
