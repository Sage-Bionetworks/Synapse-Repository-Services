package org.sagebionetworks.table.query.model;

/**
 * Result ::= @link {@link ResultExpression} | NULL
 *
 */
public class Result extends SQLElement {

	private final ResultExpression resultExpression;


	public Result(ResultExpression resultExpression) {
		super();
		this.resultExpression = resultExpression;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		resultExpression.toSql(builder, parameters);
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(resultExpression);
	}

}
