package org.sagebionetworks.table.cluster.view.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.util.ValidateArgument;

public abstract class AbstractViewFilter implements ViewFilter {

	protected final ReplicationType mainType;
	protected final Set<SubType> subTypes;
	protected final Set<Long> limitObjectIds;
	protected final Set<String> excludeKeys;
	protected final Map<String, Object> params;
	protected final boolean excludeDerivedKeys;
	
	/**
	 * @param mainType The main object type. Required.
	 * @param subTypes One or more sub-types.  Required.
	 * @param additionalFilter Additional filter to limit the results to this set of object ids.  Optional.
	 */
	public AbstractViewFilter(ReplicationType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds, Set<String> excludeKeys, boolean excludeDerivedKeys) {
		ValidateArgument.required(mainType, "mainType");
		ValidateArgument.required(subTypes, "subTypes");
		if(subTypes.isEmpty()) {
			throw new IllegalArgumentException("subTypes must contain at least one type.");
		}
		this.mainType = mainType;
		this.subTypes = subTypes;
		this.limitObjectIds = limitObjectIds;
		this.excludeKeys = excludeKeys;
		this.excludeDerivedKeys = excludeDerivedKeys;
		
		params = new HashMap<String, Object>();
		params.put("mainType", mainType.name());
		params.put("subTypes", subTypes.stream().map(t->t.name()).collect(Collectors.toList()));
		if (limitObjectIds != null) {
			params.put("limitObjectIds", limitObjectIds);
		}
		if (excludeKeys != null) {
			params.put("excludeKeys", excludeKeys);
		}
	}
	
	@Override
	public Map<String, Object> getParameters() {
		return params;
	}
	
	@Override
	public Set<SubType> getSubTypes(){
		return subTypes;
	}
	
	@Override
	public String getFilterSql() {
		StringBuilder builder = new StringBuilder(" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes)");
		if(params.containsKey("limitObjectIds")) {
			builder.append(" AND R.OBJECT_ID IN (:limitObjectIds)");
		}
		if(params.containsKey("excludeKeys")) {
			builder.append(" AND A.ANNO_KEY NOT IN (:excludeKeys)");
		}
		if (excludeDerivedKeys) {
			builder.append(" AND A.IS_DERIVED = FALSE");
		}
		return builder.toString();
	}
	
	@Override
	public Optional<Set<Long>> getLimitObjectIds() {
		return Optional.ofNullable(limitObjectIds);
	}
	
	@Override
	public ReplicationType getReplicationType() {
		return mainType;
	}
	
	@Override
	public Optional<List<ChangeMessage>> getSubViews() {
		return Optional.empty();
	}

	@Override
	public int hashCode() {
		return Objects.hash(excludeDerivedKeys, excludeKeys, limitObjectIds, mainType, subTypes);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof AbstractViewFilter)) {
			return false;
		}
		AbstractViewFilter other = (AbstractViewFilter) obj;
		return excludeDerivedKeys == other.excludeDerivedKeys && Objects.equals(excludeKeys, other.excludeKeys)
				&& Objects.equals(limitObjectIds, other.limitObjectIds) && mainType == other.mainType
				&& Objects.equals(subTypes, other.subTypes);
	}
	
}
