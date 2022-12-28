package org.sagebionetworks.table.cluster.view.filter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;

/**
 * ViewFilter defined by a flat list of objectId and (optional) version pairs. * 
 */
public class IdAndVersionFilter extends AbstractViewFilter {
	
	private Set<IdAndVersion> scope;
	private Set<Long> allObjectIds;
		
	public IdAndVersionFilter(ReplicationType mainType, Set<SubType> subTypes, Set<IdAndVersion> scope) {
		this(mainType, subTypes, null, null, scope, false);
	}
	
	public IdAndVersionFilter(ReplicationType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds, Set<String> excludeKeys, Set<IdAndVersion> scope, boolean excludeDerivedKeys) {
		super(mainType, subTypes, limitObjectIds, excludeKeys, excludeDerivedKeys);
		this.scope = scope;
		this.allObjectIds = scope.stream().map(i->i.getId()).collect(Collectors.toSet());
		this.params.put("objectIds", allObjectIds);
		
		List<Long[]> versionedRefs = scope.stream().filter(id -> id.getVersion().isPresent())
				.map(id -> new Long[] {id.getId(), id.getVersion().get()})
				.collect(Collectors.toList());
		
		if (!versionedRefs.isEmpty()) {
			this.params.put("versionedRefs", versionedRefs);
		}
		
		List<Long> nonVersionedRefs = scope.stream().filter(id -> id.getVersion().isEmpty())
				.map(IdAndVersion::getId)
				.collect(Collectors.toList());

		if (!nonVersionedRefs.isEmpty()) {
			this.params.put("nonVersionedRefs", nonVersionedRefs);
		}
	}

	@Override
	public boolean isEmpty() {
		return scope.isEmpty();
	}
	
	@Override
	public String getFilterSql() {
		StringBuilder filter = new StringBuilder(super.getFilterSql());
		filter.append(" AND (");
		
		boolean hasVersionedRefs = params.containsKey("versionedRefs");
		
		if (hasVersionedRefs) {
			filter.append("((R.OBJECT_ID, R.OBJECT_VERSION) IN (:versionedRefs))");
		}
		
		boolean hasNonVersionedRefs = params.containsKey("nonVersionedRefs");
		
		if (hasNonVersionedRefs) {
			if (hasVersionedRefs) {
				filter.append(" OR ");
			}
			filter.append("(R.OBJECT_ID IN (:nonVersionedRefs) AND R.OBJECT_VERSION = R.CURRENT_VERSION)");
		}
		
		return filter.append(")").toString();
	}

	@Override
	public String getObjectIdFilterSql() {
		// This filter includes all versions for each object.
		return super.getFilterSql() + " AND R.OBJECT_ID IN (:objectIds)";
	}

	public Set<Long> getObjectIds() {
		return allObjectIds;
	}
	
	@Override
	public Optional<List<ChangeMessage>> getSubViews() {
		return Optional.empty();
	}
	
	@Override
	public ViewFilterBuilder newBuilder() {
		return new Builder(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
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
		if (!(obj instanceof IdAndVersionFilter)) {
			return false;
		}
		IdAndVersionFilter other = (IdAndVersionFilter) obj;
		return Objects.equals(scope, other.scope);
	}

	@Override
	public String toString() {
		return "IdAndVersionFilter [scope=" + scope + ", allObjectIds=" + allObjectIds + ", mainType=" + mainType + ", subTypes=" + subTypes
				+ ", limitObjectIds=" + limitObjectIds + ", excludeKeys=" + excludeKeys + ", params=" + params + ", excludeDerivedKeys="
				+ excludeDerivedKeys + "]";
	}
	
	public static class Builder extends AbstractBuilder {
		
		Set<IdAndVersion> scope;

		public Builder(ReplicationType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds,
				Set<String> excludeKeys, Set<IdAndVersion> scope, boolean excludeDerivedKeys) {
			super(mainType, subTypes, limitObjectIds, excludeKeys, excludeDerivedKeys);
			this.scope = scope;
		}

		@Override
		public ViewFilter build() {
			return new IdAndVersionFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
		}
		
	}

}
