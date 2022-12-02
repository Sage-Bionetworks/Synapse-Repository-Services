package org.sagebionetworks.repo.manager.table.query;

import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.util.ValidateArgument;

public class MainQuery {

	private final QueryTranslator sqlQuery;
	
	public MainQuery(QueryContext expansion) {

		ValidateArgument.required(expansion, "expansion");

		CombinedQuery combined = CombinedQuery.builder().setQuery(expansion.getStartingSql())
				.setSchemaProvider(expansion.getSchemaProvider()).setOverrideOffset(expansion.getOffset())
				.setOverrideLimit(expansion.getLimit()).setSelectedFacets(expansion.getSelectedFacets())
				.setSortList(expansion.getSort()).setAdditionalFilters(expansion.getAdditionalFilters()).build();

		sqlQuery = QueryTranslator.builder(combined.getCombinedSql(), expansion.getUserId())
				.schemaProvider(expansion.getSchemaProvider()).indexDescription(expansion.getIndexDescription())
				.maxBytesPerPage(expansion.getMaxBytesPerPage()).includeEntityEtag(expansion.getIncludeEntityEtag())
				.build();
	}

	public QueryTranslator getTranslator() {
		return sqlQuery;
	}

}
