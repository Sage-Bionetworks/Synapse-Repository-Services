package org.sagebionetworks.repo.manager.table.query;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.OrderByClause;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SetQuantifier;
import org.sagebionetworks.util.ValidateArgument;

public class ActionsRequiredQuery {
	
	private BasicQuery selectFileEntitiesQuery;

	public ActionsRequiredQuery(QueryContext expansion) {
		ValidateArgument.required(expansion, "expansion");
		ValidateArgument.requirement(expansion.getSelectFileColumn() != null, "The query.selectFileColumn is required when including actions required");
		
		try {
			CombinedQuery combined = CombinedQuery.builder()
				.setQuery(expansion.getStartingSql())
				.setSchemaProvider(expansion.getSchemaProvider())
				.setSelectedFacets(expansion.getSelectedFacets())
				.setAdditionalFilters(expansion.getAdditionalFilters())
				.build();
			
			ColumnModel selectFileColumn = combined.getTableAndColumnMapper().getUnionOfAllTableSchemas().stream()
				.filter(selectColumn -> selectColumn.getId().equals(expansion.getSelectFileColumn().toString()) && ColumnType.ENTITYID == selectColumn.getColumnType())
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("The query.selectFileColumn must be an ENTITYID column that is part of the schema of the underlying table/view"));
						
			QueryExpression expression = new TableQueryParser(combined.getCombinedSql()).queryExpression();
			
			QuerySpecification querySpec = expression.getFirstElementOfType(QuerySpecification.class);
			
			if (querySpec.hasAnyAggregateElements()) {
				throw new IllegalArgumentException("Including the actions required is not supported for aggregate queries");
			}
			
			SelectList selectFileColumnList = new TableQueryParser(selectFileColumn.getName()).selectList();
			OrderByClause orderBy = new OrderByClause(new TableQueryParser(selectFileColumn.getName()).sortSpecificationList());
			
			querySpec.setSetQuantifier(SetQuantifier.DISTINCT);
			querySpec.getSelectList().replaceElement(selectFileColumnList);
			querySpec.getTableExpression().replacePagination(null);
			querySpec.getTableExpression().replaceOrderBy(orderBy);
			
			QueryTranslator sqlQuery = QueryTranslator.builder(expression.toSql(), expansion.getUserId())
				.schemaProvider(expansion.getSchemaProvider())
				.indexDescription(expansion.getIndexDescription())
				.build();
			
			selectFileEntitiesQuery = new BasicQuery(sqlQuery.getOutputSQL(), sqlQuery.getParameters());
			

		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public BasicQuery getFileEntityQuery() {
		return selectFileEntitiesQuery;
	}

}
