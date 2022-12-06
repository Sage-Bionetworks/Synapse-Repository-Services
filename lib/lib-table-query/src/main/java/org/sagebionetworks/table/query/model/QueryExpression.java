package org.sagebionetworks.table.query.model;

/**
 * QueryExpression ::= {@link NonJoinQueryExpression}
 *
 */
public class QueryExpression extends SimpleBranch implements HasSqlContext {
	
	private SqlContext sqlContext;

	public QueryExpression(NonJoinQueryExpression nonJoinQueryExpression) {
		super(nonJoinQueryExpression);
		this.sqlContext = SqlContext.query;
		this.recursiveSetParent();
	}

	@Override
	public SqlContext getSqlContext() {
		return sqlContext;
	}
	
	public void setSqlContext(SqlContext context) {
		this.sqlContext = context;
	}
	
}
