package org.sagebionetworks.table.query.model;

/**
 * 
 * SearchedCase ::= CASE {@link SearchedWhenClause} [ {@link ElseClause} ] END
 *
 */
public class SearchedCase extends SQLElement {
	
	private final SearchedWhenClause searchedWhenClause;
	private final ElseClause elseClause;	

	public SearchedCase(SearchedWhenClause searchedWhenClause, ElseClause elseClause) {
		super();
		this.searchedWhenClause = searchedWhenClause;
		this.elseClause = elseClause;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(" CASE");
		searchedWhenClause.toSql(builder, parameters);
		if(elseClause != null) {
			elseClause.toSql(builder, parameters);
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(searchedWhenClause, elseClause);
	}

}
