package org.sagebionetworks.table.cluster.view.filter;

import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.table.MainType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public abstract class AbstractViewFilter implements ViewFilter {

	protected final MainType mainType;
	protected final Set<SubType> subTypes;
	protected final Set<Long> limitObjectIds;
	protected final Set<String> excludeKeys;
	protected final MapSqlParameterSource params;
	
	/**
	 * @param mainType The main object type. Required.
	 * @param subTypes One or more sub-types.  Required.
	 * @param additionalFilter Additional filter to limit the results to this set of object ids.  Optional.
	 */
	public AbstractViewFilter(MainType mainType, Set<SubType> subTypes, Set<Long> limitObjectIds, Set<String> excludeKeys) {
		ValidateArgument.required(mainType, "mainType");
		ValidateArgument.required(subTypes, "subTypes");
		this.mainType = mainType;
		if(subTypes.isEmpty()) {
			throw new IllegalArgumentException("subTypes must contain at least one type.");
		}
		this.subTypes = subTypes;
		this.limitObjectIds = limitObjectIds;
		this.excludeKeys = excludeKeys;
		params = new MapSqlParameterSource();
		params.addValue("mainType", mainType.name());
		params.addValue("subTypes", subTypes.stream().map(t->t.name()).collect(Collectors.toList()));
		if (limitObjectIds != null) {
			params.addValue("limitObjectIds", limitObjectIds);
		}
		if (excludeKeys != null) {
			params.addValue("excludeKeys", excludeKeys);
		}
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

}
