package org.sagebionetworks.table.query.model;

/**
 * 
 *  ElseClause ::= ELSE {@link Result}
 *
 */
public class ElseClause extends SQLElement {
	
	private final Result result;
	

	public ElseClause(Result result) {
		super();
		this.result = result;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(" ELSE ");
		result.toSql(builder, parameters);
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(result);
	}

}
