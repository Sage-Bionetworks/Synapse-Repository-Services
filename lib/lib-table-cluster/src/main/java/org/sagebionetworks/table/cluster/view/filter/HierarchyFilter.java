package org.sagebionetworks.table.cluster.view.filter;


import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.table.MainType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * ViewFilter for a scope defined by a hierarchy of par
 *
 */
public class HierarchyFilter extends AbstractViewFilter {

	
	protected final Set<Long> scope;
	
	public HierarchyFilter(MainType mainType, List<SubType> subTypes, Set<Long> scope) {
		super(mainType, subTypes);
		ValidateArgument.required(scope, "scope");
		this.scope = scope;
		this.params.addValue("parentIds", scope);
	}

	@Override
	public boolean isEmpty() {
		return this.scope.isEmpty();
	}

	@Override
	public String getFilterSql() {
		return super.getFilterSql()+ " AND R.PARENT_ID IN (:parentIds) AND R.OBJECT_VERSION = R.CURRENT_VERSION";
	}

}
