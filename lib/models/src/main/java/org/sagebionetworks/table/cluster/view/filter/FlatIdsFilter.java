package org.sagebionetworks.table.cluster.view.filter;

import java.util.Objects;
import java.util.Set;

import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;

/**
 * ViewFitler defined by a flat list of objectIds.
 *
 */
public class FlatIdsFilter extends AbstractViewFilter {
	
	protected final Set<Long> scope;

	public FlatIdsFilter(ReplicationType mainType, Set<SubType> subTypes, Set<Long> scope) {
		this(mainType, subTypes, null, null, scope);
	}

	public FlatIdsFilter(ReplicationType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds, Set<String> excludeKeys,
			Set<Long> scope) {
		super(mainType, subTypes, limitObjectIds, excludeKeys);
		this.scope = scope;
		this.params.put("flatIds", scope);
	}

	@Override
	public boolean isEmpty() {
		return this.scope.isEmpty();
	}

	@Override
	public String getFilterSql() {
		return super.getFilterSql()+ " AND R.OBJECT_ID IN (:flatIds) AND R.OBJECT_VERSION = R.CURRENT_VERSION";
	}

	@Override
	public Builder newBuilder() {
		return new Builder(mainType, subTypes, limitObjectIds, excludeKeys, scope);
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(scope);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof FlatIdsFilter)) {
			return false;
		}
		FlatIdsFilter other = (FlatIdsFilter) obj;
		return Objects.equals(scope, other.scope);
	}


	@Override
	public String toString() {
		return "FlatIdsFilter [scope=" + scope + ", mainType=" + mainType + ", subTypes=" + subTypes
				+ ", limitObjectIds=" + limitObjectIds + ", excludeKeys=" + excludeKeys + ", params=" + params + "]";
	}


	public static class Builder extends AbstractBuilder {
		
		 Set<Long> scope;

		public Builder(ReplicationType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds,
				Set<String> excludeKeys,  Set<Long> scope) {
			super(mainType, subTypes, limitObjectIds, excludeKeys);
			this.scope = scope;
		}

		@Override
		public ViewFilter build() {
			return new FlatIdsFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope);
		}

	}
}
