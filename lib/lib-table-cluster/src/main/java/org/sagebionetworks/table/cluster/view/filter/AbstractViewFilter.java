package org.sagebionetworks.table.cluster.view.filter;

import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.table.MainType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public abstract class AbstractViewFilter implements ViewFilter {

	protected final MapSqlParameterSource params;
	
	/**
	 * @param mainType The main object type. Required.
	 * @param subTypes One or more sub-types.  Required.
	 * @param additionalFilter Additional filter to limit the results to this set of object ids.  Optional.
	 */
	public AbstractViewFilter(MainType mainType, Set<SubType> subTypes) {
		ValidateArgument.required(mainType, "mainType");
		ValidateArgument.required(subTypes, "subTypes");
		if(subTypes.isEmpty()) {
			throw new IllegalArgumentException("subTypes must contain at least one type.");
		}
		params = new MapSqlParameterSource();
		params.addValue("mainType", mainType.name());
		params.addValue("subTypes", subTypes.stream().map(t->t.name()).collect(Collectors.toList()));
	}
	
	@Override
	public MapSqlParameterSource getParameters() {
		return params;
	}
	
	@Override
	public String getFilterSql() {
		StringBuilder builder = new StringBuilder(" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes)");
		if(params.hasValue("limitObjectIds")) {
			builder.append(" AND R.OBJECT_ID IN (:limitObjectIds)");
		}
		if(params.hasValue("excludeKeys")) {
			builder.append(" AND A.ANNO_KEY NOT IN (:excludeKeys)");
		}
		return builder.toString();
	}
	
	@Override
	public ViewFilter setLimitToObjectIds(Set<Long> limitObjectIds) {
		if(limitObjectIds != null && limitObjectIds.isEmpty()) {
			throw new IllegalArgumentException("Limit ObjectIds cannot be empty");
		}
		if (limitObjectIds != null) {
			params.addValue("limitObjectIds", limitObjectIds);
		}
		return this;
	}

	@Override
	public ViewFilter setExcludeAnnotationKeys(Set<String> excludeKeys) {
		if(excludeKeys != null && excludeKeys.isEmpty()) {
			throw new IllegalArgumentException("Exclude Keys cannot be empty");
		}
		if (excludeKeys != null) {
			params.addValue("excludeKeys", excludeKeys);
		}
		return this;
	}
	
}
