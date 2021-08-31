package org.sagebionetworks.table.cluster.view.filter;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.table.MainType;
import org.sagebionetworks.repo.model.table.SubType;

/**
 * ViewFilter defined by a flat list of objectId and version pairs.
 * 
 */
public class FlatIdAndVersionFilter extends AbstractViewFilter {
	
	private final Set<IdVersionPair> scope;

	public FlatIdAndVersionFilter(MainType mainType, Set<SubType> subTypes, Set<IdVersionPair> scope) {
		this(mainType, subTypes, null, null, scope);
	}
	
	public FlatIdAndVersionFilter(MainType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds,
			Set<String> excludeKeys, Set<IdVersionPair> scope) {
		super(mainType, subTypes, limitObjectIds, excludeKeys);
		this.scope = scope;
		List<Long[]> pairedList = scope.stream().map(i-> new Long[] {i.getId(), i.getVersion()}).collect(Collectors.toList());
		this.params.addValue("scopePairs", pairedList);
	}


	@Override
	public boolean isEmpty() {
		return this.scope.isEmpty();
	}

	@Override
	public String getFilterSql() {
		return super.getFilterSql()+" AND (R.OBJECT_ID, R.OBJECT_VERSION) IN (:scopePairs)";
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
		if (!(obj instanceof FlatIdAndVersionFilter)) {
			return false;
		}
		FlatIdAndVersionFilter other = (FlatIdAndVersionFilter) obj;
		return Objects.equals(scope, other.scope);
	}

	@Override
	public String toString() {
		return "FlatIdAndVersionFilter [scope=" + scope + ", mainType=" + mainType + ", subTypes=" + subTypes
				+ ", limitObjectIds=" + limitObjectIds + ", excludeKeys=" + excludeKeys + ", params=" + params + "]";
	}


	public static class Builder extends AbstractBuilder {
		
		Set<IdVersionPair> scope;

		public Builder(MainType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds,
				Set<String> excludeKeys, Set<IdVersionPair> scope) {
			super(mainType, subTypes, limitObjectIds, excludeKeys);
			this.scope = scope;
		}

		@Override
		public ViewFilter build() {
			return new FlatIdAndVersionFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope);
		}
		
	}
}
