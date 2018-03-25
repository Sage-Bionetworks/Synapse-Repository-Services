package org.sagebionetworks.table.query.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.OrderByClause;
import org.sagebionetworks.table.query.model.OrderingSpecification;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SortKey;
import org.sagebionetworks.table.query.model.SortSpecification;
import org.sagebionetworks.table.query.model.SortSpecificationList;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A utility for processing table SQL strings. This class is part of the table
 * API and is used in the portal.
 * 
 * @author John
 * 
 */
public class TableSqlProcessor {

	/**
	 * Given a sql string toggle the sort on the passed column name. If the
	 * column is already included in sort clause, the direction will be toggled
	 * and the column will be moved to the first sort column. If the column is
	 * not already included in the sort clause, it will be added as the first
	 * column with a default direction of ASC.
	 * 
	 * @param sql
	 *            The resulting SQL with the new sort.
	 * @param selectColumnName
	 *            The name of the column to sort on. This should be a value from
	 *            SortColumn.getName().
	 * @return
	 * @throws ParseException
	 */
	public static String toggleSort(String sql, String columnName)
			throws ParseException {
		QuerySpecification model = TableQueryParser.parserQuery(sql);
		SelectList selectList = model.getSelectList();
		OrderByClause obc = model.getTableExpression().getOrderByClause();
		OrderByClause newCluase = new OrderByClause(new SortSpecificationList());
		// This will be the sort key for the column
		SortKey columnNameKey = SqlElementUntils.createSortKey(columnName);
		// Is this sort column already in the list?
		OrderingSpecification currentOrder = getCurrentDirection(obc,
				columnNameKey);
		OrderingSpecification newOrder;
		if (currentOrder != null) {
			if (OrderingSpecification.ASC.equals(currentOrder)) {
				newOrder = OrderingSpecification.DESC;
			} else {
				newOrder = OrderingSpecification.ASC;
			}
		} else {
			newOrder = OrderingSpecification.ASC;
		}
		SortSpecification newSortSpec = new SortSpecification(columnNameKey,
				newOrder);
		newCluase.getSortSpecificationList().addSortSpecification(newSortSpec);
		// Add back everything that is not the toggle column
		addAllSortColumns(obc, newCluase, newSortSpec);
		// Create the new sql
		return createSQL(model, selectList, newCluase);
	}

	/**
	 * Create a SQL string from an exiting QuerySpecification and a new
	 * OrderByClause.
	 * 
	 * @param model
	 * @param newCluase
	 * @return
	 */
	private static String createSQL(QuerySpecification model,
			SelectList selectList, OrderByClause newCluase) {
		TableExpression te = model.getTableExpression();
		QuerySpecification newQuery = new QuerySpecification(
				model.getSetQuantifier(), selectList, new TableExpression(
						te.getFromClause(), te.getWhereClause(),
						te.getGroupByClause(), newCluase, te.getPagination()));
		return newQuery.toSql();
	}

	/**
	 * Add all SortSpecification from the original to the new while skipping the
	 * exclude SortSpecification.
	 * 
	 * @param original
	 * @param newClause
	 * @param exclude
	 *            Do not add this column if found
	 */
	private static void addAllSortColumns(OrderByClause original,
			OrderByClause newClause, SortSpecification exclude) {
		if (original != null) {
			if (original.getSortSpecificationList() != null
					&& original.getSortSpecificationList()
							.getSortSpecifications() != null) {
				for (SortSpecification sort : original
						.getSortSpecificationList().getSortSpecifications()) {
					// Add this column as long as it is not the exclude.
					if (!sort.getSortKey().equivalent(exclude.getSortKey())) {
						newClause.getSortSpecificationList()
								.addSortSpecification(sort);
					}
				}
			}
		}
	}

	/**
	 * Find the OrderingSpecification for the given SortKey.
	 * 
	 * @param obc
	 * @param columnNameKey
	 * @return OrderingSpecification from the OrderByClause that matches the key
	 *         if it exists. If the key is not in the order by then null.
	 */
	private static OrderingSpecification getCurrentDirection(OrderByClause obc,
			SortKey columnNameKey) {
		if (obc != null) {
			if (obc.getSortSpecificationList() != null
					&& obc.getSortSpecificationList().getSortSpecifications() != null) {
				for (SortSpecification sort : obc.getSortSpecificationList()
						.getSortSpecifications()) {
					if (sort.getSortKey().equivalent(columnNameKey)) {
						return sort.getOrderingSpecification();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Extract the sorting info from the given sql
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static List<SortItem> getSortingInfo(String sql)
			throws ParseException {
		QuerySpecification model = TableQueryParser.parserQuery(sql);
		return getSortingInfo(model);
	}

	/**
	 * Extract the sorting info from the given model
	 * 
	 * @param model
	 * @return
	 */
	public static List<SortItem> getSortingInfo(QuerySpecification model) {
		List<SortItem> results = new LinkedList<SortItem>();
		if (model.getTableExpression().getOrderByClause() != null) {
			SortSpecificationList list = model.getTableExpression()
					.getOrderByClause().getSortSpecificationList();
			if (list != null && list.getSortSpecifications() != null) {
				for (SortSpecification sort : list.getSortSpecifications()) {
					SortItem item = new SortItem();
					item.setColumn(sort.getSortKey().toSqlWithoutQuotes());
					if (sort.getOrderingSpecification() == null) {
						// default to ASC.
						item.setDirection(SortDirection.ASC);
					} else {
						// translate from the query object to the API object by name.
						item.setDirection(SortDirection.valueOf(sort.getOrderingSpecification().name()));
					}
					results.add(item);
				}
			}
		}
		return results;
	}
	
	/**
	 * Appends a WHERE clause that is based on the selected facets
	 * @param basicSql must be a very basic SQL i.e. SELECT * FROM (tableId) <ORDER BY ...> 
	 * @param selectedFacets
	 * @return the passed in sql query with its where clause's search condition ANDed with the effective search condition of the list of facets
	 * @throws ParseException 
	 */
	public static String generateSqlWithFacets(String basicSql, List<FacetColumnRequest> selectedFacets, List<ColumnModel> schema) throws ParseException{
		ValidateArgument.required(basicSql, "basicSql");
		ValidateArgument.required(selectedFacets, "selectedFacets");
		ValidateArgument.required(schema, "schema");
		ValidateArgument.requirement(schema.size() >= selectedFacets.size(), "The schema must have at least as many columns as the selected facets");
		
		QuerySpecification model = TableQueryParser.parserQuery(basicSql);
		
		//create a map from column name to columnModel
		Map<String, ColumnModel> columnTypeMap = new HashMap<>();
		for(ColumnModel cm : schema){
			columnTypeMap.put(cm.getName(), cm);
		}
		
		List<FacetRequestColumnModel> facetRequestModels = new ArrayList<>();
		for(FacetColumnRequest facetRequest: selectedFacets){
			ColumnModel columnModel = columnTypeMap.get(facetRequest.getColumnName());
			if(columnModel == null){
				throw new IllegalArgumentException("Schema did not contain ColumnModel infromation for column name: " + facetRequest.getColumnName());
			}
			facetRequestModels.add(new FacetRequestColumnModel(columnModel, facetRequest));
		}
		
		return FacetUtils.appendFacetSearchConditionToQuerySpecification(model, facetRequestModels).toSql();
	}
	

	

}
