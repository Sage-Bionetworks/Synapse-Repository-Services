package org.sagebionetworks.table.query.model;

/**
 * 
 * SimpleWhenClause ::= WHEN {@link WhenOperand} THEN {@link Result}
 *
 */
public class SimpleWhenClause extends SQLElement {
	
	private final WhenOperand whenOperand;
	private final Result result;
	
	public SimpleWhenClause(WhenOperand whenOperand, Result result) {
		super();
		this.whenOperand = whenOperand;
		this.result = result;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(" WHEN");
		whenOperand.toSql(builder, parameters);
		builder.append(" THEN");
		result.toSql(builder, parameters);
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(whenOperand, result);
	}

}
