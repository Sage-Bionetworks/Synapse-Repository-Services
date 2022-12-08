package org.sagebionetworks.table.cluster.view.filter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.LimitExceededException;
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

	private final ContainerProvider containerProvider;
	private Set<Long> parentIds;

	public HierarchicaFilter(ReplicationType mainType, Set<SubType> subTypes, ContainerProvider containerProvider) {
		this(mainType, subTypes, null, null, containerProvider, false);
	}

	public HierarchicaFilter(ReplicationType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds,
			Set<String> excludeKeys, ContainerProvider provider, boolean excludeDerivedKeys) {
		super(mainType, subTypes, limitObjectIds, excludeKeys, excludeDerivedKeys);
		ValidateArgument.required(provider, "ContainerProvider");
		containerProvider = provider;
	}

	@Override
	public Map<String, Object> getParameters() {
		this.params.put("parentIds", loadParentIds());
		return params;
	}

	private Set<Long> loadParentIds() {
		if (parentIds != null) {
			return parentIds;
		}

		try {
			parentIds = containerProvider.getScope();
		} catch (LimitExceededException e){
			throw new IllegalStateException(e);
		}

		return parentIds;
	}
	@Override
	public boolean isEmpty() {
		return this.loadParentIds().isEmpty();
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

	public Set<Long> getParentIds() {
		return loadParentIds();
	}

	@Override
	public Builder newBuilder() {
		return new Builder(mainType, subTypes, limitObjectIds, excludeKeys, containerProvider, excludeDerivedKeys);
	}

	@Override
	public Optional<List<ChangeMessage>> getSubViews() {
		if (ReplicationType.ENTITY.equals(mainType) && loadParentIds().size() > 1) {
			return Optional.of(loadParentIds().stream().map(p -> new ChangeMessage().setObjectId(KeyFactory.keyToString(p))
					.setObjectType(ObjectType.ENTITY_CONTAINER)).collect(Collectors.toList()));
		} else {
			return Optional.empty();
		}
	}



	@Override
	public String toString() {
		return "HierarchyFilter [scope=" + parentIds + ", mainType=" + mainType + ", subTypes=" + subTypes
				+ ", limitObjectIds=" + limitObjectIds + ", excludeKeys=" + excludeKeys + ", params=" + params + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		HierarchicaFilter that = (HierarchicaFilter) o;
		return Objects.equals(parentIds, that.parentIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), containerProvider);
	}

	public static class Builder extends AbstractBuilder {

		ContainerProvider containerProvider;

		public Builder(ReplicationType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds,
				Set<String> excludeKeys, ContainerProvider containerProvider, boolean excludeDerivedKeys) {
			super(mainType, subTypes, limitObjectIds, excludeKeys, excludeDerivedKeys);
			this.containerProvider = containerProvider;
		}

		@Override
		public ViewFilter build() {
			return new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, containerProvider, excludeDerivedKeys);
		}

	}

}
