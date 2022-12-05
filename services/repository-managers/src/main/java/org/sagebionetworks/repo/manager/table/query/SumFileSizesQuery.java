package org.sagebionetworks.repo.manager.table.query;

import java.util.Optional;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.util.SqlElementUtils;
import org.sagebionetworks.util.ValidateArgument;

public class SumFileSizesQuery {


	private final BasicQuery rowIdAndVersionQuery;
	
	public SumFileSizesQuery(QueryContext expansion) {
		ValidateArgument.required(expansion, "expansion");

		if (TableType.entityview.equals(expansion.getIndexDescription().getTableType())
				|| TableType.dataset.equals(expansion.getIndexDescription().getTableType())) {
			try {
				CombinedQuery combined = CombinedQuery.builder().setQuery(expansion.getStartingSql())
						.setAdditionalFilters(expansion.getAdditionalFilters()).setSelectedFacets(expansion.getSelectedFacets())
						.setSchemaProvider(expansion.getSchemaProvider()).build();
				QuerySpecification model = new TableQueryParser(combined.getCombinedSql()).querySpecification();
				QueryTranslator sqlQuery = QueryTranslator.builder(model.toSql(), expansion.getUserId()).schemaProvider(expansion.getSchemaProvider()).indexDescription(expansion.getIndexDescription()).build();
				// first get the rowId and rowVersions for the given query up to the limit + 1.
				rowIdAndVersionQuery = SqlElementUtils.buildSqlSelectRowIdAndVersions(sqlQuery.getOutputSQL(),expansion.getMaxRowsPerCall() + 1L)
						.map((sql)->{
							return new BasicQuery(sql, sqlQuery.getParameters());
						}).orElse(null);
			} catch (ParseException e) {
				throw new IllegalArgumentException(e);
			}
		} else {
			rowIdAndVersionQuery = null;
		}
	}

	public Optional<BasicQuery> getRowIdAndVersionQuery() {
		return Optional.ofNullable(rowIdAndVersionQuery);
	}


}
