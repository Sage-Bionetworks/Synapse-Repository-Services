package org.sagebionetworks.table.query.model;

/**
 * Modified subset of &ltqualified join&gt in:
 * <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 * Omits the optional [ NATURAL ] and substitutes &ltjoin specification&gt for &ltjoin
 * condition&gt, which &ltjoin specification&gt holds.
 *<p>
 * Effectively: <p> &ltqualified join&gt ::= &lttable reference&gt [ &ltjoin type&gt ] JOIN
 * &lttable reference&gt &ltjoin condition&gt
 */
public class QualifiedJoin extends SQLElement {
	TableReference tableReferenceLHS;// left side of join
	JoinType joinType; // type of join to perform
	TableReference tableReferenceRHS;// right side of join
	JoinCondition joinCondition;// condition for join

	public QualifiedJoin(TableReference tableReferenceLHS, TableReference tableReferenceRHS,
			JoinCondition joinCondition) {
		this(tableReferenceLHS, null, tableReferenceRHS, joinCondition);
	}

	public QualifiedJoin(TableReference tableReferenceLHS, JoinType joinType, TableReference tableReferenceRHS,
			JoinCondition joinCondition) {
		this.tableReferenceLHS = tableReferenceLHS;
		this.joinType = joinType;
		this.tableReferenceRHS = tableReferenceRHS;
		this.joinCondition = joinCondition;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		tableReferenceLHS.toSql(builder, parameters);
		if (joinType != null) {
			builder.append(" ");
			joinType.toSql(builder, parameters);
		}
		builder.append(" JOIN ");
		tableReferenceRHS.toSql(builder, parameters);
		if(joinCondition != null) {
			builder.append(" ");
			joinCondition.toSql(builder, parameters);
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(tableReferenceLHS, tableReferenceRHS, joinCondition);
	}

}
