package org.sagebionetworks.repo.manager.table.query;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.OrderByClause;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SetQuantifier;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.ValidateArgument;

public class ActionsRequiredQuery {
	
	private QueryTranslator selectFilesQuery;

	public ActionsRequiredQuery(QueryContext expansion) {
		ValidateArgument.required(expansion, "expansion");
		
		try {
			CombinedQuery combined = CombinedQuery.builder()
				.setQuery(expansion.getStartingSql())
				.setSchemaProvider(expansion.getSchemaProvider())
				.setSelectedFacets(expansion.getSelectedFacets())
				.setAdditionalFilters(expansion.getAdditionalFilters())
				.build();
			
			QueryExpression expression = new TableQueryParser(combined.getCombinedSql()).queryExpression();
			
			QuerySpecification querySpec = expression.getFirstElementOfType(QuerySpecification.class);
			
			if (querySpec.hasAnyAggregateElements()) {
				throw new IllegalArgumentException("Including the actions required is not supported for aggregate queries");
			}
			
			TableAndColumnMapper tableAndColumnMapper = new TableAndColumnMapper(querySpec, expansion.getSchemaProvider());
			
			Pair<SelectList, OrderByClause> selectFileColumn = tableAndColumnMapper.buildSelectAndOrderByFileColumn(expansion.getSelectFileColumn());
			
			querySpec.replaceSelectList(selectFileColumn.getFirst(), SetQuantifier.DISTINCT);
			querySpec.getTableExpression().replacePagination(null);
			querySpec.getTableExpression().replaceOrderBy(selectFileColumn.getSecond());
			
			selectFilesQuery = QueryTranslator.builder(expression.toSql(), expansion.getUserId())
				.schemaProvider(expansion.getSchemaProvider())
				.indexDescription(expansion.getIndexDescription())
				.build();
			
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public BasicQuery getFileEntityQuery(long limit, long offset) {
		
		// Avoid parsing and translating the query each time and simply append the pagination params
		String sql = selectFilesQuery.getOutputSQL() + " LIMIT :" + TableConstants.P_LIMIT + " OFFSET :" + TableConstants.P_OFFSET;
		
		Map<String, Object> parameters = new HashMap<>(selectFilesQuery.getParameters());
		
		parameters.put(TableConstants.P_LIMIT, limit);
		parameters.put(TableConstants.P_OFFSET, offset);
		
		return new BasicQuery(sql, parameters);
	}

}
