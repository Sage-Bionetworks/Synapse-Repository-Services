package org.sagebionetworks.repo.manager.table;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.query.util.FacetRequestColumnModel;
import org.sagebionetworks.util.ValidateArgument;

public class FacetTransformerListColumnValueCounts implements FacetTransformer {
	public static final String VALUE_ALIAS = "value";
	public static final String COUNT_ALIAS = "frequency";
	public static final long MAX_NUM_FACET_CATEGORIES = 100;


	private String columnName;
	private List<FacetRequestColumnModel> facets;

	private SqlQuery generatedFacetSqlQuery;
	private Set<String> selectedValues;

	public FacetTransformerValueCounts(String columnName, List<FacetRequestColumnModel> facets, SqlQuery originalQuery, Set<String> selectedValues) {
		ValidateArgument.required(columnName, "columnName");
		ValidateArgument.required(facets, "facets");
		ValidateArgument.required(originalQuery, "originalQuery");
		this.columnName = columnName;
		this.facets = facets;
		this.selectedValues = selectedValues;
		this.generatedFacetSqlQuery = generateFacetSqlQuery(originalQuery);
	}


	private SqlQuery generateFacetSqlQuery(SqlQuery originalQuery) {

	}

	@Override
	public String getColumnName() {
		return null;
	}

	@Override
	public SqlQuery getFacetSqlQuery() {
		return null;
	}

	@Override
	public FacetColumnResult translateToResult(RowSet rowSet) {
		return null;
	}

}
