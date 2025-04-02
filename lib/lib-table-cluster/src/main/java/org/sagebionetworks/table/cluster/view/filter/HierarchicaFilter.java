package org.sagebionetworks.table.cluster.view.filter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * ViewFilter for a scope defined by a hierarchy of parentIds
 *
 */
public class HierarchicaFilter extends AbstractViewFilter {

	protected final Set<Long> parentIds;
	private final Set<Long> scope;
 	
	public HierarchicaFilter(ReplicationType mainType, Set<SubType> subTypes, Set<Long> parentIds, Set<Long> scope) {
		this(mainType, subTypes, null, null, parentIds, false, scope);
	}

	public HierarchicaFilter(ReplicationType mainType, Set<SubType> subTypes, Set<Long> parentIds) {
		this(mainType, subTypes, null, null, parentIds, false, null);
	}

	public HierarchicaFilter(ReplicationType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds,
			Set<String> excludeKeys, Set<Long> parentIds, boolean excludeDerivedKeys, Set<Long> scope) {
		super(mainType, subTypes, limitObjectIds, excludeKeys, excludeDerivedKeys);
		ValidateArgument.required(parentIds, "parentIds");
		this.parentIds = parentIds;
		this.scope = scope;
		this.params.put("parentIds", parentIds);
	}

	@Override
	public boolean isEmpty() {
		return this.parentIds.isEmpty();
	}

	@Override
	public String getFilterSql() {
		return super.getFilterSql() + " AND R.PARENT_ID IN (:parentIds) AND R.OBJECT_VERSION = R.CURRENT_VERSION";
	}

	@Override
	public String getObjectIdFilterSql() {
		// this filter includes all versions of each object.
		return super.getFilterSql() + " AND R.PARENT_ID IN (:parentIds)";
	}

	@Override
	public Builder newBuilder() {
		return new Builder(mainType, subTypes, limitObjectIds, excludeKeys, parentIds, excludeDerivedKeys, scope);
	}

	@Override
	public Optional<List<ChangeMessage>> getSubViews() {
		if (ReplicationType.ENTITY.equals(mainType) && parentIds.size() > 1) {
			return Optional.of(parentIds.stream().map(p -> new ChangeMessage().setObjectId(KeyFactory.keyToString(p))
					.setObjectType(ObjectType.ENTITY_CONTAINER)).collect(Collectors.toList()));
		} else {
			return Optional.empty();
		}
	}
	
	public Set<Long> getParentIds() {
		return parentIds;
	}
	
	public Set<Long> getScope(){
		return scope;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(parentIds, scope);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		HierarchicaFilter other = (HierarchicaFilter) obj;
		return Objects.equals(parentIds, other.parentIds) && Objects.equals(scope, other.scope);
	}

	@Override
	public String toString() {
		return "HierarchicaFilter [parentIds=" + parentIds + ", scope=" + scope + ", mainType=" + mainType
				+ ", subTypes=" + subTypes + ", limitObjectIds=" + limitObjectIds + ", excludeKeys=" + excludeKeys
				+ ", params=" + params + ", excludeDerivedKeys=" + excludeDerivedKeys + "]";
	}

	public static class Builder extends AbstractBuilder {

		Set<Long> parentIds;
		Set<Long> scope;

		public Builder(ReplicationType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds,
				Set<String> excludeKeys, Set<Long> parentIds, boolean excludeDerivedKeys, Set<Long> scope) {
			super(mainType, subTypes, limitObjectIds, excludeKeys, excludeDerivedKeys);
			this.parentIds = parentIds;
			this.scope = scope;
		}

		@Override
		public ViewFilter build() {
			return new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, parentIds, excludeDerivedKeys, scope);
		}

	}

}
