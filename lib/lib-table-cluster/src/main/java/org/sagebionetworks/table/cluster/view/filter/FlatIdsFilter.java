package org.sagebionetworks.table.cluster.view.filter;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.table.MainType;
import org.sagebionetworks.repo.model.table.SubType;

/**
 * ViewFitler defined by a flat list of objectIds.
 *
 */
public class FlatIdsFilter extends AbstractViewFilter {
	
	protected final Set<Long> scope;

	public FlatIdsFilter(MainType mainType, List<SubType> subTypes, Set<Long> additionalFilter, Set<Long> scope) {
		super(mainType, subTypes, additionalFilter);
		this.scope = scope;
		this.params.addValue("flatIds", scope);
	}

	@Override
	public boolean isEmpty() {
		return this.scope.isEmpty();
	}

	@Override
	public String getFilterSql() {
		return super.getFilterSql()+ " AND R.OBJECT_ID IN (:flatIds) AND R.OBJECT_VERSION = R.CURRENT_VERSION";
	}

}
