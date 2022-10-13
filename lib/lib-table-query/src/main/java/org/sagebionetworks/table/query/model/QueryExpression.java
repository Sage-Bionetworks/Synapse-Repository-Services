package org.sagebionetworks.table.query.model;

/**
 * QueryExpression ::= {@link NonJoinQueryExpression}
 *
 */
public class QueryExpression extends SimpleBranch {

	public QueryExpression(NonJoinQueryExpression nonJoinQueryExpression) {
		super(nonJoinQueryExpression);
	}
	
}
