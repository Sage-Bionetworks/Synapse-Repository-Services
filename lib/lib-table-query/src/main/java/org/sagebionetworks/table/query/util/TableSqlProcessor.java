package org.sagebionetworks.table.query.util;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.OrderByClause;
import org.sagebionetworks.table.query.model.OrderingSpecification;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SQLElement;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SortKey;
import org.sagebionetworks.table.query.model.SortSpecification;
import org.sagebionetworks.table.query.model.SortSpecificationList;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;
import org.sagebionetworks.table.query.model.visitors.IsAggregateVisitor;
import org.sagebionetworks.table.query.model.visitors.ToNameStringVisitor;
import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;

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
		SortKey columnNameKey = createSortKey(columnName);
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
	 * Create delimited sort key from a column name.
	 * 
	 * @param columnName
	 * @return
	 */
	private static SortKey createSortKey(String columnName) {
		try {
			/*
			 * For aggregate functions we can use this ValueExpressionPrimary to
			 * create the SortKey. For non-aggregate functions the name must be
			 * bracketed in quotes.
			 */
			ValueExpressionPrimary primary = new TableQueryParser(columnName)
					.valueExpressionPrimary();
			IsAggregateVisitor visitor = new IsAggregateVisitor();
			primary.visit(visitor);
			if (visitor.isAggregate()) {
				return new SortKey(primary);
			} else {
				// Put non-aggregate column names in quotes.
				StringBuilder builder = new StringBuilder();
				builder.append("\"");
				builder.append(columnName);
				builder.append("\"");
				return new TableQueryParser(builder.toString()).sortKey();
			}
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
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
		ToSimpleSqlVisitor visitor = new ToSimpleSqlVisitor();
		newQuery.visit(visitor);
		return visitor.getSql();
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
					if (!nameEquals(sort, exclude)) {
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
					if (nameEquals(sort.getSortKey(), columnNameKey)) {
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
					ToNameStringVisitor nameVisitor = new ToNameStringVisitor();
					sort.getSortKey().visit(nameVisitor);
					item.setColumn(nameVisitor.getName());
					if (OrderingSpecification.ASC.equals(sort
							.getOrderingSpecification())) {
						item.setDirection(SortDirection.ASC);
					} else {
						item.setDirection(SortDirection.DESC);
					}
					results.add(item);
				}
			}
		}
		return results;
	}

	/**
	 * Do the two elements have the same name?
	 * 
	 * @param other
	 *            The other element to compare the name to.
	 * @return
	 */
	public static boolean nameEquals(SQLElement one, SQLElement two) {
		return getName(one).equals(getName(two));
	}

	/**
	 * Get the name of the element.
	 * 
	 * @param element
	 * @return
	 */
	public static String getName(SQLElement element) {
		ToNameStringVisitor visitor = new ToNameStringVisitor();
		element.visit(visitor);
		return visitor.getName();
	}
}
