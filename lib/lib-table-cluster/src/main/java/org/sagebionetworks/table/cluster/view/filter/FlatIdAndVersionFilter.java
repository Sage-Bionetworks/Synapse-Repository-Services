package org.sagebionetworks.table.cluster.view.filter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;

/**
 * ViewFilter defined by a flat list of objectId and version pairs.
 * 
 */
public class FlatIdAndVersionFilter extends AbstractViewFilter {
	
	private final Set<IdVersionPair> scope;
	private final Set<Long> objectIds;

	public FlatIdAndVersionFilter(ReplicationType mainType, Set<SubType> subTypes, Set<IdVersionPair> scope) {
		this(mainType, subTypes, null, null, scope, false);
	}
	
	public FlatIdAndVersionFilter(ReplicationType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds,
			Set<String> excludeKeys, Set<IdVersionPair> scope, boolean excludeDerivedKeys) {
		super(mainType, subTypes, limitObjectIds, excludeKeys, excludeDerivedKeys);
		this.scope = scope;
		List<Long[]> pairedList = scope.stream().map(i-> new Long[] {i.getId(), i.getVersion()}).collect(Collectors.toList());
		this.params.put("scopePairs", pairedList);
		this.objectIds = scope.stream().map(i->i.getId()).collect(Collectors.toSet());
		this.params.put("objectIds", objectIds);
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
	public String getObjectIdFilterSql() {
		// This filter includes all versions for each object.
		return super.getFilterSql()+" AND R.OBJECT_ID IN (:objectIds)";
	}

	@Override
	public Builder newBuilder() {
		return new Builder(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
	}

	public Set<IdVersionPair> getScope(){
		return scope;
	}
	
	public Set<Long> getObjectIds(){
		return objectIds;
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
		return "FlatIdAndVersionFilter [scope=" + scope + ", objectIds=" + objectIds + ", mainType=" + mainType + ", subTypes=" + subTypes
				+ ", limitObjectIds=" + limitObjectIds + ", excludeKeys=" + excludeKeys + ", params=" + params + ", excludeDerivedKeys="
				+ excludeDerivedKeys + "]";
	}


	public static class Builder extends AbstractBuilder {
		
		Set<IdVersionPair> scope;

		public Builder(ReplicationType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds,
				Set<String> excludeKeys, Set<IdVersionPair> scope, boolean excludeDerivedKeys) {
			super(mainType, subTypes, limitObjectIds, excludeKeys, excludeDerivedKeys);
			this.scope = scope;
		}

		@Override
		public ViewFilter build() {
			return new FlatIdAndVersionFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
		}
		
	}


	@Override
	public Optional<List<ChangeMessage>> getSubViews() {
		return Optional.empty();
	}

}
