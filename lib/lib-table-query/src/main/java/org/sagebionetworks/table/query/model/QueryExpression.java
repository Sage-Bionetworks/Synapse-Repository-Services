package org.sagebionetworks.table.query.model;

/**
 * QueryExpression ::= {@link NonJoinQueryExpression}
 *
 */
public class QueryExpression extends SimpleBranch implements HasSqlContext {
	
	private SqlContext sqlContext;

	public QueryExpression(NonJoinQueryExpression nonJoinQueryExpression) {
		super(nonJoinQueryExpression);
		// defaults to query for most cases.
		this.sqlContext = SqlContext.query;
		this.recursiveSetParent();
	}

	@Override
	public SqlContext getSqlContext() {
		return sqlContext;
	}

	/**
	 * @param sqlContext the sqlContext to set
	 */
	public void setSqlContext(SqlContext sqlContext) {
		this.sqlContext = sqlContext;
	}
	
}
