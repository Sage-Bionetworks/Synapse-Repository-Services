package org.sagebionetworks.repo.model.query.entity;

import org.sagebionetworks.repo.model.query.BasicQuery;

/**
 * Model represents an entire entity query including all of the parts.
 *
 */
public class QueryModel extends SqlElement {
	
	SelectList select;
	Tables from;
	ExpressionList where;
	SortList sort;
	Pagination pagination;
	
	/**
	 * Build a query model for a given query.
	 * 
	 * @param query
	 */
	public QueryModel(BasicQuery query){
		IndexProvider indexProvider = new IndexProvider();
		// build select
		select = new SelectList(query.getSelect(), indexProvider);
		// build where
		where = new ExpressionList(query.getFilters(), indexProvider);
		// build sort
		sort = new SortList(query.getSort(), query.isAscending(), indexProvider);
		// build the from using the expressions.
		from = new Tables(select, where, sort);
		pagination = new Pagination(query.getLimit(), query.getOffset());
	}

	@Override
	public void toSql(StringBuilder builder) {
		select.toSql(builder);
		from.toSql(builder);
		where.toSql(builder);	
		sort.toSql(builder);
		pagination.toSql(builder);
	}

	@Override
	public void bindParameters(Parameters parameters) {
		from.bindParameters(parameters);
		where.bindParameters(parameters);
		pagination.bindParameters(parameters);
	}
	
	/**
	 * Create a count query for this model.
	 * 
	 * @return
	 */
	public String toCountSql(){
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT COUNT(*)");
		from.toSql(builder);
		where.toSql(builder);
		return builder.toString();
	}

}
