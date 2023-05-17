package org.sagebionetworks.table.query.model;

/**
 * 
 *  SearchedWhenClause ::= WHEN {@link SearchCondition} THEN {@link Result}
 *
 */
public class SearchedWhenClause extends SQLElement {
	
	private final SearchCondition searchCondition;
	private final Result result;
	

	public SearchedWhenClause(SearchCondition searchCondition, Result result) {
		super();
		this.searchCondition = searchCondition;
		this.result = result;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("WHEN ");
		searchCondition.toSql(builder, parameters);
		builder.append(" THEN ");
		result.toSql(builder, parameters);
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(searchCondition, result);
	}

}
