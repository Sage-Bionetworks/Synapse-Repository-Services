package org.sagebionetworks.table.cluster.view.filter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.table.MainType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public abstract class AbstractViewFilter implements ViewFilter {

	protected final MainType mainType;
	protected final List<SubType> subTypes;
	protected final Set<Long> additionalFilter;
	protected final MapSqlParameterSource params;
	
	/**
	 * @param mainType The main object type. Required.
	 * @param subTypes One or more sub-types.  Required.
	 * @param additionalFilter Additional filter to limit the results to this set of object ids.  Optional.
	 */
	public AbstractViewFilter(MainType mainType, List<SubType> subTypes, Set<Long> additionalFilter) {
		ValidateArgument.required(mainType, "mainType");
		ValidateArgument.required(subTypes, "subTypes");
		if(subTypes.isEmpty()) {
			throw new IllegalArgumentException("subTypes must contain at least one type.");
		}
		if(additionalFilter != null && additionalFilter.isEmpty()) {
			throw new IllegalArgumentException("Additional filter cannot be empty");
		}
		this.mainType = mainType;
		this.subTypes = subTypes;
		this.additionalFilter = additionalFilter;
		params = new MapSqlParameterSource();
		params.addValue("mainType", mainType.name());
		params.addValue("subTypes", subTypes.stream().map(t->t.name()).collect(Collectors.toList()));
		if (additionalFilter != null) {
			params.addValue("additionalFilter", additionalFilter);
		}
	}
	
	@Override
	public MapSqlParameterSource getParameters() {
		return params;
	}
	
	@Override
	public String getFilterSql() {
		StringBuilder builder = new StringBuilder(" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes)");
		if(additionalFilter != null) {
			builder.append(" AND R.OBJECT_ID IN (:additionalFilter)");
		}
		return builder.toString();
	}
	
}
