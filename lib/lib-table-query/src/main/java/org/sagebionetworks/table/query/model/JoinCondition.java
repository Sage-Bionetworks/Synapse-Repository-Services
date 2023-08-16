package org.sagebionetworks.table.query.model;

/**
 * This matches &ltjoin condition&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class JoinCondition extends SimpleBranch {
	
	public JoinCondition(SearchCondition searchCondition) {
		super(searchCondition);
	}
	
	// Only used internally to build a JOIN "ON TRUE" (for joining a JSON_TABLE expression on unnested columns)
	public JoinCondition(TruthSpecification truthSpecification) {
		super(truthSpecification);
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("ON ");
		super.toSql(builder, parameters);
	}
}
