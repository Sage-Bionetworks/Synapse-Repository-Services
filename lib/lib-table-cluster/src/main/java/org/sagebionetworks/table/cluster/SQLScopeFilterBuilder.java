package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_ALIAS;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_OBJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_PARENT_ID;

import java.util.List;

import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.metadata.ScopeFilterProvider;
import org.sagebionetworks.util.ValidateArgument;

public class SQLScopeFilterBuilder {
	
	private ScopeFilterProvider scopeFilterProvider;
	private Long viewTypeMask;
	
	public SQLScopeFilterBuilder(ScopeFilterProvider scopeFilterProvider, Long viewTypeMask) {
		this.scopeFilterProvider = scopeFilterProvider;
		this.viewTypeMask = viewTypeMask;
	}
	
	public SQLScopeFilter build() {
		ValidateArgument.required(scopeFilterProvider, "scopeFilterProvider");
		ValidateArgument.required(viewTypeMask, "viewTypeMask");
		
		String viewTypeFilter = buildViewTypeFilter();
		String viewScopeFilterColumn = buildViewScopeFilterColumn();
		
		return new SQLScopeFilter(viewTypeFilter, viewScopeFilterColumn);
	}

	private String buildViewScopeFilterColumn() {
		if (scopeFilterProvider.isFilterScopeByObjectId(viewTypeMask)) {
			return OBJECT_REPLICATION_COL_OBJECT_ID;
		}
		return OBJECT_REPLICATION_COL_PARENT_ID;
	}

	private String buildViewTypeFilter() {
		List<Enum<?>> subTypes = scopeFilterProvider.getSubTypesForMask(viewTypeMask);
		
		StringBuilder builder = new StringBuilder();
		
		builder.append(OBJECT_REPLICATION_ALIAS);
		builder.append(".");
		builder.append(TableConstants.OBJECT_REPLICATION_COL_SUBTYPE);
		builder.append(" IN (");
		builder.append(TableConstants.joinEnumForSQL(subTypes.stream()));
		builder.append(")");
		
		return builder.toString();
	}
	
}
