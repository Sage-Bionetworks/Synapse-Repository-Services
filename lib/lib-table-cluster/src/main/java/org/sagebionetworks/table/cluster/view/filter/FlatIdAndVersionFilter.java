package org.sagebionetworks.table.cluster.view.filter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.table.MainType;
import org.sagebionetworks.repo.model.table.SubType;

/**
 * ViewFilter defined by a flat list of objectId and version pairs.
 * 
 */
public class FlatIdAndVersionFilter extends AbstractViewFilter {
	
	private final Set<IdVersionPair> scope;

	public FlatIdAndVersionFilter(MainType mainType, List<SubType> subTypes, Set<IdVersionPair> scope) {
		super(mainType, subTypes);
		this.scope = scope;
		List<Long[]> pairedList = scope.stream().map(i-> new Long[] {i.getId(), i.getVersion()}).collect(Collectors.toList());
		this.params.addValue("scopePairs", pairedList);
	}

	@Override
	public boolean isEmpty() {
		return this.scope.isEmpty();
	}

	@Override
	public String getFilterSql() {
		return super.getFilterSql()+" AND (R.OBJECT_ID, R.OBJECT_VERSION) IN (:scopePairs)";
	}

}
