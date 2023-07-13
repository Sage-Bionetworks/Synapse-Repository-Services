package org.sagebionetworks.table.query.model;

/**
 * 
 * DefiningClause ::= <defining> {@link SearchCondition}
 *
 */
public class DefiningClause extends SQLElement {
	
	private SearchCondition searchCondition;
	
	public DefiningClause(SearchCondition searchCondition) {
		super();
		this.searchCondition = searchCondition;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("DEFINING_WHERE ");
		searchCondition.toSql(builder, parameters);
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(searchCondition);
	}

	public SearchCondition getSearchCondition() {
		return searchCondition;
	}

}
