package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_ALIAS;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_OBJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_PARENT_ID;

import java.util.List;

import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.ViewScopeFilter;
import org.sagebionetworks.util.ValidateArgument;

public class SQLScopeFilterBuilder {

	private ViewScopeFilter filter;
	
	public SQLScopeFilterBuilder(ViewScopeFilter filter) {
		this.filter = filter;
	}
	
	public SQLScopeFilter build() {
		ValidateArgument.required(filter, "filter");
		ValidateArgument.required(filter.getSubTypes(), "filter.subTypes");
		
		String viewTypeFilter = buildViewTypeFilter();
		String viewScopeFilterColumn = buildViewScopeFilterColumn();
		
		return new SQLScopeFilter(viewTypeFilter, viewScopeFilterColumn);
	}

	private String buildViewScopeFilterColumn() {
		if (filter.isFilterByObjectId()) {
			return OBJECT_REPLICATION_COL_OBJECT_ID;
		}
		return OBJECT_REPLICATION_COL_PARENT_ID;
	}

	private String buildViewTypeFilter() {
		List<Enum<?>> subTypes = filter.getSubTypes();
		
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
